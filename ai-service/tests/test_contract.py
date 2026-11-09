import asyncio
import json
from dataclasses import replace
from time import monotonic

import httpx
import pytest

from app.candidates import candidates
from app.config import Settings
from app.jev import Budget, Jev, ServiceError, validate_answers
from app.main import create_app
from app.pipeline import IMPORTANCE, MATCH_LEVELS, legacy_score

CFG = Settings(api_key="synthetic-provider", service_token="synthetic-internal", model="typesafe/jev-1.14.0")
@pytest.fixture(autouse=True)
def isolated_budget(tmp_path, monkeypatch):
    original = Budget.__init__
    monkeypatch.setattr(Budget, "__init__", lambda self, limit, path: original(self, limit, str(tmp_path / "budget.sqlite3")))


AUTH = {"Authorization": "Bearer synthetic-internal"}
DETAIL = {"jobDescription": {"requirements": "需要 Python，含英文 (test) 😀；五险一金"}}


def fixture(payload, unknown=False):
    answers = {}
    for key, q in payload["questions"].items():
        if q["type"] == "noul":
            answers[key] = {"type": "noul", "noul": 0.99}
        elif q["type"] == "choice":
            status = "unknown" if unknown and key == "skills_status" else "scored"
            answers[key] = dict(type="choice", choice=status, confidence=1,
                                probabilities={k: float(k == status) for k in q["criteria"]})
        else:
            answers[key] = dict(type="score", score=3.5, confidence=0.95,
                                legend={str(i): v for i, v in enumerate(q["criteria"])},
                                probabilities={"0": 0, "1": 0, "2": 0, "3": 0.5, "4": 0.5})
    return {"model": payload["model"], "answers": answers, "usage": {"input_tokens": 100, "output_tokens": 30}}


async def api(cfg=CFG, handler=None):
    handler = handler or (lambda request: httpx.Response(200, json=fixture(json.loads(request.content))))
    app = create_app(cfg, httpx.MockTransport(handler))
    return app, httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://internal")


async def test_auth_readiness_and_safe_validation():
    app, client = await api(Settings())
    async with client:
        assert (await client.get("/health/live")).status_code == 200
        assert (await client.get("/health/ready")).status_code == 503
        assert (await client.post("/internal/v1/job-analysis", json={})).status_code == 401
    app, client = await api()
    async with client:
        r = await client.post("/internal/v1/job-analysis", headers=AUTH, json={"jobId": 42, "detailData": {}, "secret": "private"})
        assert r.status_code == 422 and "private" not in r.text
        assert not (await client.get("/health/ready")).json()["qualityGate"]["automaticWritebackAllowed"]


def test_candidates_unicode_pointer_and_no_legacy():
    source = {"jobDescription": {"a/b~c": " 😀需要 Python, Go 或 Rust；无需学历"},
              "jobTags": ["五险一金"], "analysisReport": {"requirements": "FAKE"}}
    cs = candidates(source)
    assert len(cs) == 3 and all("FAKE" not in c.text for c in cs)
    e = cs[0].evidence[0]
    assert e["sourcePath"] == "/jobDescription/a~1b~0c"
    assert source["jobDescription"]["a/b~c"][e["start"]:e["end"]] == e["text"]
    assert e["text"] == "😀需要 Python, Go 或 Rust"
    long = "字" * 2100
    spans = candidates({"jobDescription": {"fullDescription": long}})[0].evidence
    assert spans[0]["start"] == 0
    all_spans = [e for c in candidates({"jobDescription": {"fullDescription": long}}) for e in c.evidence]
    assert max(e["end"] for e in all_spans) == len(long)


async def test_analysis_48_candidates_98_questions_multilabel_and_core():
    calls = []
    def handler(request):
        assert str(request.url) == "https://api.commandcode.ai/provider/v1/systemone"
        assert request.headers["authorization"] == "Bearer synthetic-provider"
        assert request.headers["x-cmd-zdr"] == "1"
        assert "cookie" not in request.headers
        p = json.loads(request.content)
        calls.append(p)
        assert "messages" not in p and p["model"] == CFG.model
        return httpx.Response(200, json=fixture(p))
    app, client = await api(replace(CFG, zdr=True, quality_approved=True), handler)
    async with client:
        r = await client.post("/internal/v1/job-analysis", headers=AUTH,
                              json={"jobId": "42", "detailData": {"jobDescription": {"requirements": ";".join(f"条件{i}" for i in range(49))}}})
        assert r.status_code == 200, r.text
        data = r.json()
        assert [len(c["questions"]) for c in calls] == [98, 4, 49]
        assert data["status"] == "complete" and len(data["requirements"]) == len(data["benefits"]) == 49
        assert len(data["coreRequirements"]) == 20
        assert data["usage"] == {"inputTokens": 300, "outputTokens": 90}
        assert calls[0]["questions"]["c0_r"]["instructions"].find("c0") >= 0


async def test_uncalibrated_never_complete_and_empty_not_no_evidence():
    app, client = await api()
    async with client:
        for detail in (DETAIL, {}):
            r = await client.post("/internal/v1/job-analysis", headers=AUTH, json={"jobId": "x", "detailData": detail})
            assert r.status_code == 200 and r.json()["status"] == "needs_review"


async def test_match_independent_states_unknown_not_zero_and_precision():
    calls = []
    def handler(request):
        p = json.loads(request.content)
        calls.append(p)
        return httpx.Response(200, json=fixture(p, unknown='"jobId": "unknown"' in p["state"]))
    app, client = await api(handler=handler)
    async with client:
        r = await client.post("/internal/v1/match-batch", headers=AUTH, json={"resume": "合成 Python 经历 😀", "jobs": [
            {"jobId": id, "detailData": DETAIL} for id in ("ok", "unknown")]})
        assert r.status_code == 200, r.text
        a, b = r.json()["items"]
        assert a["score"] == pytest.approx(8.75)
        assert b["score"] is None and b["status"] == "insufficient_evidence" and not r.json()["complete"]
        assert len(calls) == 2
        for c in calls:
            s = json.loads(c["state"])
            assert isinstance(s["jobId"], str) and "jobs" not in s
        evidence = a["dimensions"][0]["evidence"]
        assert {e["sourcePath"].split("/")[1] for e in evidence} == {"job", "resume"}
        duplicate = await client.post("/internal/v1/match-batch", headers=AUTH, json={"resume": "x", "jobs": [{"jobId": "1", "detailData": {}}] * 2})
        assert duplicate.status_code == 422


@pytest.mark.parametrize("mutation", [
    lambda d: d.update(answers={}),
    lambda d: d["answers"].update(extra={"type": "noul", "noul": 1}),
    lambda d: d["answers"]["q"].update(noul=float("nan")),
    lambda d: d["answers"]["q"].update(noul=float("inf")),
    lambda d: d["answers"]["q"].update(noul=True),
    lambda d: d["answers"]["q"].update(noul=1.01),
    lambda d: d["answers"]["q"].update(type="score"),
    lambda d: d.update(model="other"),
])
def test_invalid_answers(mutation):
    qs = {"q": {"type": "noul", "instructions": "q"}}
    d = fixture({"model": CFG.model, "questions": qs})
    mutation(d)
    with pytest.raises(ServiceError):
        validate_answers(d, qs, CFG.model)


@pytest.mark.parametrize("mutation", [
    lambda a: a.update(score=5), lambda a: a.update(confidence=-0.1),
    lambda a: a.update(probabilities={"0": 1}), lambda a: a.update(legend={}),
    lambda a: a.update(score=2), lambda a: a["probabilities"].update({"0": -1}),
])
def test_invalid_score_distribution(mutation):
    qs = {"q": {"type": "score", "instructions": "q", "criteria": MATCH_LEVELS}}
    d = fixture({"model": CFG.model, "questions": qs})
    mutation(d["answers"]["q"])
    with pytest.raises(ServiceError):
        validate_answers(d, qs, CFG.model)


@pytest.mark.parametrize("status,expected,calls", [(403, 503, 1), (401, 503, 1), (429, 503, 2), (500, 503, 2), (422, 503, 1), (302, 503, 1)])
async def test_http_failures_safe_bounded_and_zdr_closed(status, expected, calls):
    seen = []
    def handler(request):
        seen.append(request)
        return httpx.Response(status, json={"error": {"code": "cmd_zdr_no_providers" if status == 422 else "upstream_error", "message": "PRIVATE"}}, headers={"Location": "https://evil.test"})
    app, client = await api(replace(CFG, zdr=True), handler)
    async with client:
        r = await client.post("/internal/v1/job-analysis", headers=AUTH, json={"jobId": "42", "detailData": DETAIL})
        assert r.status_code == expected and "PRIVATE" not in r.text
        assert len(seen) == calls and all(x.headers["x-cmd-zdr"] == "1" for x in seen)


async def test_101_questions_and_budget_and_deadline():
    calls = []
    def handler(request):
        p = json.loads(request.content)
        calls.append(p)
        return httpx.Response(200, json=fixture(p))
    jev = Jev(CFG, httpx.MockTransport(handler))
    qs = {f"q{i}": {"type": "noul", "instructions": "q"} for i in range(101)}
    result = await jev.ask("synthetic", qs, monotonic() + 1, {"input_tokens": [], "output_tokens": []})
    assert len(result) == 101 and [len(c["questions"]) for c in calls] == [100, 1]
    with pytest.raises(ServiceError) as e:
        await jev.ask("x", {"q": qs["q0"]}, monotonic() - 1, {"input_tokens": [], "output_tokens": []})
    assert e.value.status == 504
    await jev.close()
    jev = Jev(replace(CFG, daily_tokens=1), httpx.MockTransport(handler))
    with pytest.raises(ServiceError) as e:
        await jev.ask("x", {"q": qs["q0"]}, monotonic() + 1, {"input_tokens": [], "output_tokens": []})
    assert e.value.code == "daily_budget_exhausted"
    await jev.close()


async def test_concurrency_is_global():
    running = peak = 0
    async def handler(request):
        nonlocal running, peak
        running += 1
        peak = max(peak, running)
        await asyncio.sleep(0.01)
        running -= 1
        return httpx.Response(200, json=fixture(json.loads(request.content)))
    jev = Jev(replace(CFG, concurrency=2), httpx.MockTransport(handler))
    await asyncio.gather(*(jev.ask("x", {"q": {"type": "noul", "instructions": "q"}}, monotonic() + 1,
                                  {"input_tokens": [], "output_tokens": []}) for _ in range(6)))
    assert peak == 2
    await jev.close()


def test_half_up_boundaries():
    assert [legacy_score(v) for v in (0, 4, 4.49, 4.5, 8.5, 10)] == [0, 4, 4, 5, 9, 10]


async def test_timeout_transport_retried_once_and_request_deadline():
    calls = 0
    def timeout(request):
        nonlocal calls
        calls += 1
        raise httpx.ReadTimeout("PRIVATE", request=request)
    app, client = await api(handler=timeout)
    async with client:
        r = await client.post("/internal/v1/job-analysis", headers=AUTH, json={"jobId": "42", "detailData": DETAIL})
        assert r.status_code == 504 and "PRIVATE" not in r.text and calls == 2
    async def slow(request):
        await asyncio.sleep(0.2)
        return httpx.Response(200, json=fixture(json.loads(request.content)))
    app, client = await api(handler=slow)
    async with client:
        r = await client.post("/internal/v1/job-analysis", headers=AUTH, json={"jobId": "42", "detailData": DETAIL, "timeoutMs": 5})
        assert r.status_code == 504, r.text


async def test_duplicate_json_response_is_rejected_without_retry():
    calls = 0
    def handler(request):
        nonlocal calls
        calls += 1
        return httpx.Response(200, content='{"model":"typesafe/jev-1.14.0","answers":{},"answers":{}}')
    app, client = await api(handler=handler)
    async with client:
        r = await client.post("/internal/v1/job-analysis", headers=AUTH, json={"jobId": "42", "detailData": DETAIL})
        assert r.status_code == 502 and calls == 1


async def test_bounds_and_duplicate_json_input():
    app, client = await api(replace(CFG, max_input_bytes=150, max_candidates=1))
    async with client:
        for body, status in [('{"jobId":"x","jobId":"y","detailData":{}}', 400),
                             ('{"jobId":"x","detailData":{},"timeoutMs":NaN}', 400),
                             (json.dumps({"jobId": "x", "detailData": {"jobTags": ["a", "b"]}}), 422),
                             (json.dumps({"jobId": "x", "detailData": {"jobTags": ["a" * 200]}}), 422)]:
            r = await client.post("/internal/v1/job-analysis", headers={**AUTH, "Content-Type": "application/json"}, content=body)
            assert r.status_code == status, r.text


async def test_partial_match_failure_keeps_exact_job_ids():
    def handler(request):
        p = json.loads(request.content)
        if json.loads(p["state"])["jobId"] == "bad":
            return httpx.Response(403, text="PRIVATE")
        return httpx.Response(200, json=fixture(p))
    app, client = await api(handler=handler)
    async with client:
        r = await client.post("/internal/v1/match-batch", headers=AUTH, json={"resume": "合成经历", "jobs": [
            {"jobId": id, "detailData": DETAIL} for id in ("ok", "bad")]})
        assert r.status_code == 200, r.text
        a, b = r.json()["items"]
        assert a["jobId"] == "ok" and a["status"] == "scored"
        assert b == dict(jobId="bad", status="failed", score=None, dimensions=[])
        assert not r.json()["complete"]


async def test_not_applicable_reweights_and_missing_links_prevents_scoring():
    def handler(request):
        p = json.loads(request.content)
        d = fixture(p)
        a = d["answers"]["education_status"]
        a.update(choice="not_applicable", probabilities={k: float(k == "not_applicable") for k in STATUS_KEYS})
        if json.loads(p["state"])["jobId"] == "missing":
            d["answers"]["skills_r0"]["noul"] = 0.01
        return httpx.Response(200, json=d)
    STATUS_KEYS = ("scored", "unknown", "not_applicable")
    app, client = await api(handler=handler)
    async with client:
        r = await client.post("/internal/v1/match-batch", headers=AUTH, json={"resume": "合成经历", "jobs": [
            {"jobId": id, "detailData": DETAIL} for id in ("ok", "missing")]})
        assert r.status_code == 200, r.text
        a, b = r.json()["items"]
        assert a["score"] == pytest.approx(8.75) and a["dimensions"][-1]["rawScore"] is None
        assert b["score"] is None


def test_budget_survives_new_client_and_is_atomic(tmp_path):
    a, b = Budget(100, "ignored"), Budget(100, "ignored")
    a.reserve(60)
    with pytest.raises(ServiceError) as e:
        b.reserve(41)
    assert e.value.code == "daily_budget_exhausted"
    b.reserve(40)


async def test_missing_usage_stays_null_and_no_evidence_requires_gate():
    def handler(request):
        p = json.loads(request.content)
        d = fixture(p)
        d.pop("usage")
        for a in d["answers"].values():
            a["noul"] = 0.01
        return httpx.Response(200, json=d)
    app, client = await api(replace(CFG, quality_approved=True), handler)
    async with client:
        r = await client.post("/internal/v1/job-analysis", headers=AUTH, json={"jobId": "42", "detailData": DETAIL})
        assert r.json()["status"] == "no_evidence"
        assert r.json()["usage"] == {"inputTokens": None, "outputTokens": None}


@pytest.mark.parametrize("mutation", [
    lambda a: a.update(choice="invented"),
    lambda a: a.update(choice="unknown"),
    lambda a: a["probabilities"].update(scored=0.2),
    lambda a: a["probabilities"].update(extra=0.0),
])
def test_invalid_choice(mutation):
    qs = {"q": dict(type="choice", criteria={"scored": "yes", "unknown": "missing"})}
    d = fixture({"model": CFG.model, "questions": qs})
    mutation(d["answers"]["q"])
    with pytest.raises(ServiceError):
        validate_answers(d, qs, CFG.model)


async def test_zdr_error_code_is_terminal_even_with_500():
    seen = []
    def handler(request):
        seen.append(request)
        return httpx.Response(500, json={"error": {"code": "cmd_zdr_no_providers"}})
    app, client = await api(replace(CFG, zdr=True), handler)
    async with client:
        r = await client.post("/internal/v1/job-analysis", headers=AUTH, json={"jobId": "42", "detailData": DETAIL})
        assert r.status_code == 503 and r.json()["error"]["code"] == "zdr_unavailable"
        assert len(seen) == 1 and seen[0].headers["x-cmd-zdr"] == "1"


async def test_retry_unknown_charge_does_not_claim_total_usage():
    calls = 0
    def handler(request):
        nonlocal calls
        calls += 1
        if calls == 1:
            return httpx.Response(500, text="unavailable")
        return httpx.Response(200, json=fixture(json.loads(request.content)))
    app, client = await api(handler=handler)
    async with client:
        r = await client.post("/internal/v1/job-analysis", headers=AUTH, json={"jobId": "42", "detailData": DETAIL})
        assert r.status_code == 200
        assert r.json()["usage"] == {"inputTokens": None, "outputTokens": None}
