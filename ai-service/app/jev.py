import asyncio
import json
import math
import os
import sqlite3
from datetime import datetime, timezone
from time import monotonic

import httpx

from .config import Settings


class ServiceError(Exception):
    def __init__(self, code: str, status: int):
        self.code, self.status = code, status


def number(value, low=0, high=1):
    if type(value) not in (int, float) or not math.isfinite(value) or not low <= value <= high:
        raise ServiceError("invalid_provider_output", 502)
    return value


def unique_object(pairs):
    obj = {}
    for key, value in pairs:
        if key in obj:
            raise ValueError("duplicate JSON key")
        obj[key] = value
    return obj


def validate_answers(data, questions, model):
    if not isinstance(data, dict) or data.get("model") != model:
        raise ServiceError("invalid_provider_output", 502)
    answers = data.get("answers")
    if not isinstance(answers, dict) or answers.keys() != questions.keys():
        raise ServiceError("invalid_provider_output", 502)
    for qid, q in questions.items():
        a = answers[qid]
        if not isinstance(a, dict) or a.get("type") != q["type"]:
            raise ServiceError("invalid_provider_output", 502)
        if q["type"] == "noul":
            number(a.get("noul"))
            continue
        number(a.get("confidence"))
        probs = a.get("probabilities")
        keys = {str(i) for i in range(len(q["criteria"]))} if q["type"] == "score" else set(q["criteria"])
        if not isinstance(probs, dict) or set(probs) != keys:
            raise ServiceError("invalid_provider_output", 502)
        for value in probs.values():
            number(value)
        if abs(sum(probs.values()) - 1) > 0.001:
            raise ServiceError("invalid_provider_output", 502)
        if q["type"] == "score":
            number(a.get("score"), 0, len(keys) - 1)
            if a.get("legend") != {str(i): v for i, v in enumerate(q["criteria"])}:
                raise ServiceError("invalid_provider_output", 502)
            if abs(a["score"] - sum(int(k) * v for k, v in probs.items())) > 0.01:
                raise ServiceError("invalid_provider_output", 502)
        elif a.get("choice") not in keys or probs[a["choice"]] != max(probs.values()):
            raise ServiceError("invalid_provider_output", 502)
    return answers


class Budget:
    """Conservative reservations survive missing usage and ambiguous network failures."""
    def __init__(self, limit, path):
        self.limit, self.path = limit, path

    def reserve(self, amount):
        day = datetime.now(timezone.utc).date().isoformat()
        try:
            fd = os.open(self.path, os.O_CREAT | os.O_WRONLY, 0o600)
            os.close(fd)
            with sqlite3.connect(self.path, timeout=1) as db:
                db.execute("CREATE TABLE IF NOT EXISTS budget (day TEXT PRIMARY KEY, used INTEGER NOT NULL)")
                db.execute("BEGIN IMMEDIATE")
                row = db.execute("SELECT used FROM budget WHERE day = ?", (day,)).fetchone()
                used = row[0] if row else 0
                if used + amount > self.limit:
                    raise ServiceError("daily_budget_exhausted", 503)
                db.execute("INSERT INTO budget(day,used) VALUES(?,?) ON CONFLICT(day) DO UPDATE SET used=excluded.used",
                           (day, used + amount))
        except (OSError, sqlite3.Error):
            raise ServiceError("budget_unavailable", 503) from None


class Jev:
    def __init__(self, settings: Settings, transport=None):
        self.settings = settings
        self.slots = asyncio.Semaphore(settings.concurrency)
        self.budget = Budget(settings.daily_tokens, settings.budget_path)
        self.http = httpx.AsyncClient(transport=transport, follow_redirects=False, trust_env=False,
                                      headers={"User-Agent": "BossAnalyze-Jev/1"})

    async def close(self):
        await self.http.aclose()

    async def ask(self, state, questions, deadline, usage):
        merged = {}
        entries = list(questions.items())
        for offset in range(0, len(entries), 100):
            batch = dict(entries[offset:offset + 100])
            merged.update(await self._batch(state, batch, deadline, usage))
        return merged

    async def _batch(self, state, questions, deadline, usage):
        cfg = self.settings
        payload = {"model": cfg.model, "state": state, "questions": questions}
        # shortcut: UTF-8 byte count is a conservative token bound, upgrade with a verified Jev tokenizer.
        size = len(json.dumps(payload, ensure_ascii=False).encode())
        if size > 60000 or any(len(json.dumps({"state": state, "question": q}, ensure_ascii=False).encode()) > 30000 for q in questions.values()):
            raise ServiceError("context_limit", 422)
        base = cfg.api_base.rstrip("/")
        if not base.endswith("/v1"):
            base += "/v1"
        headers = {"Authorization": f"Bearer {cfg.api_key}"}
        if cfg.zdr:
            headers["x-cmd-zdr"] = "1"
        for attempt in range(cfg.retries + 1):
            if monotonic() >= deadline:
                raise ServiceError("deadline_exhausted", 504)
            self.budget.reserve(size + 8192)
            try:
                async with asyncio.timeout(max(0, deadline - monotonic())):
                    async with self.slots:
                        response = await self.http.post(base + "/systemone", json=payload,
                                                        headers=headers, timeout=max(0.001, deadline - monotonic()))
                if len(response.content) > 1000000:
                    raise ServiceError("invalid_provider_output", 502)
                if response.status_code == 200:
                    try:
                        data = json.loads(response.content, object_pairs_hook=unique_object)
                        answers = validate_answers(data, questions, cfg.model)
                        reported = data.get("usage")
                        for key in ("input_tokens", "output_tokens"):
                            value = reported.get(key) if isinstance(reported, dict) else None
                            if value is not None and (type(value) is not int or value < 0):
                                raise ServiceError("invalid_provider_output", 502)
                            usage[key].append(value)
                        actual = sum(reported.get(k, 0) or 0 for k in ("input_tokens", "output_tokens")) if isinstance(reported, dict) else 0
                        if actual > size + 8192:
                            self.budget.reserve(actual - size - 8192)
                        return answers
                    except (ValueError, TypeError, KeyError):
                        raise ServiceError("invalid_provider_output", 502) from None
                for values in usage.values():
                    values.append(None)
                try:
                    failure = response.json()
                    error = failure.get("error", {}) if isinstance(failure, dict) else {}
                    if isinstance(error, dict) and error.get("code") == "cmd_zdr_no_providers":
                        raise ServiceError("zdr_unavailable", 503)
                except ValueError:
                    pass
                # ZDR failure and all 4xx except 429 are terminal; never remove headers.
                if response.status_code == 429 or response.status_code >= 500:
                    if attempt < cfg.retries:
                        await asyncio.sleep(min(0.1 * 2 ** attempt, max(0, deadline - monotonic())))
                        continue
                raise ServiceError("provider_unavailable", 503)
            except (TimeoutError, httpx.TimeoutException):
                for values in usage.values():
                    values.append(None)
                if attempt == cfg.retries or monotonic() >= deadline:
                    raise ServiceError("deadline_exhausted", 504) from None
            except httpx.RequestError:
                for values in usage.values():
                    values.append(None)
                if attempt == cfg.retries:
                    raise ServiceError("provider_unavailable", 503) from None
        raise ServiceError("provider_unavailable", 503)
