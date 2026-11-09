"""Offline, read-only evaluation. Never calls a provider or changes quality approval."""
import argparse
import json
import re
import unicodedata
from difflib import SequenceMatcher
from itertools import combinations
from pathlib import Path

from pydantic import BaseModel, ConfigDict, StrictBool, ValidationError

from .jev import unique_object
from .schemas import Evidence


class Record(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    id: str
    groupId: str
    source: dict[str, str]


class Gold(Record):
    split: str
    reviewStatus: str
    reviewer: str
    annotationOrigin: str
    dataOrigin: str
    subgroup: str
    requirements: list[Evidence]
    benefits: list[Evidence]
    unknown: dict[str, StrictBool]
    ranking: list[str]


class Prediction(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    id: str
    model: str
    candidates: list[Evidence]
    requirements: list[Evidence]
    benefits: list[Evidence]
    unknown: dict[str, StrictBool]
    ranking: list[str]


def load(path, schema):
    rows = [schema.model_validate(json.loads(line, object_pairs_hook=unique_object))
            for line in Path(path).read_text().splitlines() if line.strip()]
    if len({r.id for r in rows}) != len(rows) or any(not r.id.strip() for r in rows):
        raise ValueError("invalid dataset")
    return rows


def normalized(row):
    return re.sub(r"\W+", "", unicodedata.normalize("NFKC", " ".join(sorted(row.source.values())))).casefold()


def spans(entries, source):
    result = set()
    valid = 0
    for e in entries:
        text = source.get(e.sourcePath)
        ok = (text is not None and e.end > e.start and e.end <= len(text)
              and text[e.start:e.end] == e.text)
        valid += ok
        result.add((e.sourcePath, e.start, e.end, e.text))
    if len(result) != len(entries):
        raise ValueError("duplicate evidence")
    return result, valid


def ratio(a, b):
    return a / b if b else None


def metrics(gold, predictions):
    counts = {k: 0 for k in ("samples", "evidence", "validEvidence", "candidateGold", "candidateHit",
                            "unknown", "unknownTotal", "unknownCorrect", "rankingPairs", "rankingCorrect")}
    classes = {k: dict(tp=0, predicted=0, gold=0) for k in ("requirements", "benefits")}
    for g in gold:
        p = predictions[g.id]
        counts["samples"] += 1
        if p.model != "typesafe/jev-1.14.0" or p.unknown.keys() != g.unknown.keys():
            raise ValueError("incompatible predictions")
        cs, valid_candidates = spans(p.candidates, g.source)
        counts["evidence"] += len(cs)
        counts["validEvidence"] += valid_candidates
        for name in classes:
            expected, valid = spans(getattr(g, name), g.source)
            if valid != len(getattr(g, name)):
                raise ValueError("invalid gold evidence")
            actual, valid = spans(getattr(p, name), g.source)
            counts["evidence"] += len(actual)
            counts["validEvidence"] += valid
            counts["candidateGold"] += len(expected)
            # A candidate may contain the annotated subspan, but must itself be verbatim.
            counts["candidateHit"] += sum(any(c[0] == e[0] and c[1] <= e[1] and c[2] >= e[2]
                                                         and g.source[c[0]][c[1]:c[2]] == c[3] for c in cs
                                                         if c[0] in g.source) for e in expected)
            classes[name]["tp"] += len(expected & actual)
            classes[name]["predicted"] += len(actual)
            classes[name]["gold"] += len(expected)
        counts["unknown"] += sum(p.unknown.values())
        counts["unknownTotal"] += len(g.unknown)
        counts["unknownCorrect"] += sum(v == p.unknown[k] for k, v in g.unknown.items())
        if len(set(g.ranking)) != len(g.ranking) or len(set(p.ranking)) != len(p.ranking):
            raise ValueError("duplicate ranking ids")
        if set(p.ranking) - set(g.ranking):
            raise ValueError("unexpected ranking ids")
        positions = {v: i for i, v in enumerate(p.ranking)}
        for a, b in combinations(g.ranking, 2):
            counts["rankingPairs"] += 1
            counts["rankingCorrect"] += a in positions and b in positions and positions[a] < positions[b]
    return dict(counts=counts,
                classes={k: dict(v, precision=ratio(v["tp"], v["predicted"]), recall=ratio(v["tp"], v["gold"]))
                         for k, v in classes.items()},
                candidateRecall=ratio(counts["candidateHit"], counts["candidateGold"]),
                evidenceConsistency=ratio(counts["validEvidence"], counts["evidence"]),
                unknownCoverage=ratio(counts["unknown"], counts["unknownTotal"]),
                unknownAgreement=ratio(counts["unknownCorrect"], counts["unknownTotal"]),
                rankingAgreement=ratio(counts["rankingCorrect"], counts["rankingPairs"]))


def evaluate(gold, predicted, development, min_samples, min_ranking):
    if min_samples < 1 or not 0 <= min_ranking <= 1:
        raise ValueError("invalid evaluation policy")
    for g in gold:
        if (g.split != "test" or g.reviewStatus != "reviewed" or not g.reviewer.strip()
                or g.annotationOrigin != "human" or not g.subgroup.strip() or not g.unknown
                or g.dataOrigin not in {"real_public", "real_authorized", "synthetic"}):
            raise ValueError("unreviewed or missing annotations")
    all_sources = [*gold, *development]
    if any(not r.groupId.strip() or not normalized(r) for r in all_sources):
        raise ValueError("missing split identity")
    # shortcut: quadratic near-duplicate audit suits small gold sets; use an indexed audit for large corpora.
    for i, a in enumerate(gold):
        for b in [*gold[i + 1:], *development]:
            if (a.id == b.id or a.groupId == b.groupId or normalized(a) == normalized(b)
                    or SequenceMatcher(None, normalized(a), normalized(b), autojunk=False).ratio() >= 0.9):
                raise ValueError("test leakage or duplicate group")
    predictions = {p.id: p for p in predicted}
    if len(predictions) != len(predicted) or predictions.keys() != {g.id for g in gold}:
        raise ValueError("prediction sample mismatch")
    groups = {name: metrics([g for g in gold if g.subgroup == name], predictions)
              for name in sorted({g.subgroup for g in gold})}
    overall = metrics(gold, predictions)
    checks = []
    for report in [overall, *groups.values()]:
        values = [(report["candidateRecall"], .98), (report["evidenceConsistency"], 1.0),
                  (report["unknownAgreement"], 1.0), (report["rankingAgreement"], min_ranking)]
        values += [(v["precision"], .95) for v in report["classes"].values()]
        checks.extend(None if v is None else v >= threshold for v, threshold in values)
    eligible = (len(gold) >= min_samples and all(g.dataOrigin != "synthetic" for g in gold)
                and all(r["counts"]["samples"] >= min_samples for r in groups.values()))
    status = "fail" if False in checks else "unverifiable" if None in checks or not eligible else "pass"
    return dict(status=status, samples=len(gold), developmentSamples=len(development), metrics=overall,
                subgroups=groups, policy=dict(minSamplesPerGroup=min_samples, minRankingAgreement=min_ranking),
                qualityApproval="unverifiable", reason="offline metrics do not authorize deployment or writeback")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--gold", required=True)
    parser.add_argument("--predictions", required=True)
    parser.add_argument("--development", required=True, help="Complete training/calibration source manifest JSONL")
    parser.add_argument("--min-samples", required=True, type=int)
    parser.add_argument("--min-ranking-agreement", required=True, type=float)
    args = parser.parse_args()
    try:
        result = evaluate(load(args.gold, Gold), load(args.predictions, Prediction),
                          load(args.development, Record), args.min_samples, args.min_ranking_agreement)
    except (OSError, ValueError, TypeError, KeyError, ValidationError):
        result = dict(status="unverifiable", samples=0, reason="invalid, unreviewed, incomplete or leaking input")
    print(json.dumps(result, ensure_ascii=False, allow_nan=False))
    return {"pass": 0, "fail": 1, "unverifiable": 2}[result["status"]]


if __name__ == "__main__":
    raise SystemExit(main())
