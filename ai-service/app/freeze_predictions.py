"""Convert frozen internal requests/responses offline; never reads gold or calls a provider."""
import argparse
import json
import os
import sys
from pathlib import Path

from pydantic import BaseModel, ConfigDict, Field

from .candidates import candidates, resume_candidates
from .evaluate import Prediction, Record, spans
from .jev import unique_object
from .schemas import AnalysisResponse, Evidence, MatchResponse


class FrozenJob(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    jobId: str = Field(min_length=1, max_length=128)
    detailData: dict


class FrozenRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    id: str
    groupId: str
    jobs: list[FrozenJob] = Field(min_length=1)
    resume: str | None = None


class FrozenResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    id: str
    analyses: list[AnalysisResponse]
    match: MatchResponse | None = None
    latencyMs: object = None
    costUsd: object = None


def pointer(part):
    return part.replace("~", "~0").replace("/", "~1")


def convert(request: dict, response: dict) -> tuple[Prediction, Record]:
    """Return a Prediction and its synchronized source Record, without mutating inputs."""
    req = FrozenRequest.model_validate(request, strict=True)
    res = FrozenResponse.model_validate(response, strict=True)
    ids = [job.jobId for job in req.jobs]
    if (not req.id.strip() or not req.groupId.strip() or req.id != res.id
            or any(not job_id.strip() for job_id in ids) or len(set(ids)) != len(ids)):
        raise ValueError("invalid request identity")
    if len(res.analyses) != len(ids) or {a.jobId for a in res.analyses} != set(ids):
        raise ValueError("analysis job identity mismatch")
    if any(a.model != "typesafe/jev-1.14.0" or a.pipelineVersion != "jev-1"
           or a.policyVersion != "jev-policy-1" for a in res.analyses):
        raise ValueError("incompatible frozen analysis")

    source = {}
    candidate_evidence = []
    roots = {job_id: "/job" if len(ids) == 1 else "/jobs/" + pointer(job_id) for job_id in ids}

    def leaves(value, path):
        if isinstance(value, dict):
            for key, child in value.items():
                leaves(child, path + "/" + pointer(key))
        elif isinstance(value, list):
            for i, child in enumerate(value):
                leaves(child, f"{path}/{i}")
        elif isinstance(value, str):
            source[path] = value

    for job in req.jobs:
        root = roots[job.jobId]
        for key in ("jobDescription", "jobTags", "basicInfo"):
            if key in job.detailData:
                leaves(job.detailData[key], root + "/" + key)
        candidate_evidence.extend(e for c in candidates(job.detailData, root) for e in c.evidence)
    if req.resume is not None:
        source["/resume"] = req.resume
        candidate_evidence.extend(e for c in resume_candidates(req.resume) for e in c.evidence)

    def checked(entries):
        unique = {}
        for entry in entries:
            e = Evidence.model_validate(entry, strict=True)
            unique[(e.sourcePath, e.start, e.end, e.text)] = e
        result = list(unique.values())
        _, valid = spans(result, source)
        if valid != len(result):
            raise ValueError("evidence does not match frozen source")
        return result

    selected = {name: [] for name in ("requirements", "benefits", "coreRequirements")}
    for analysis in res.analyses:
        for name in selected:
            selected[name].extend(dict(e.model_dump(), sourcePath=roots[analysis.jobId] + e.sourcePath)
                                  for item in getattr(analysis, name) for e in item.evidence)
    selected = {name: checked(entries) for name, entries in selected.items()}
    if not spans(selected["coreRequirements"], source)[0] <= spans(selected["requirements"], source)[0]:
        raise ValueError("core evidence outside requirements")

    if req.resume is not None and res.match is None:
        raise ValueError("missing frozen match")
    unknown, ranking = {}, []
    if res.match is not None:
        match = res.match
        if (req.resume is None or match.model != res.analyses[0].model
                or match.policyVersion != res.analyses[0].policyVersion):
            raise ValueError("incompatible frozen match")
        if len(match.items) != len(ids) or {item.jobId for item in match.items} != set(ids):
            raise ValueError("match job identity mismatch")
        for item in match.items:
            if item.status == "failed":
                raise ValueError("failed prediction is not representable")
            names = [d.name for d in item.dimensions]
            if len(names) != 4 or set(names) != {"skills", "relevant_experience", "experience", "education"}:
                raise ValueError("incomplete dimensions")
            comparable = (any(d.status == "scored" for d in item.dimensions)
                          and all(d.status != "unknown" for d in item.dimensions))
            if ((item.status == "scored") != comparable
                    or (item.score is not None) != comparable
                    or any((d.rawScore is not None) != (d.status == "scored")
                           or (d.status != "scored" and d.probabilities is not None) for d in item.dimensions)):
                raise ValueError("invalid final score state")
            for dimension in item.dimensions:
                unknown[item.jobId + ":" + dimension.name] = dimension.status == "unknown"
                evidence = []
                for e in dimension.evidence:
                    path = e.sourcePath
                    if path.startswith("/job/"):
                        path = roots[item.jobId] + path[4:]
                    elif path != "/resume":
                        raise ValueError("invalid matching source path")
                    evidence.append(dict(e.model_dump(), sourcePath=path))
                checked(evidence)
            if item.status == "scored":
                ranking.append(item)
        if match.complete != all(item.status == "scored" for item in match.items):
            raise ValueError("inconsistent match completion")
        ranking = [item.jobId for item in sorted(ranking, key=lambda item: (-item.score, item.jobId))]

    prediction = Prediction(id=req.id, model=res.analyses[0].model, candidates=checked(candidate_evidence),
                            **selected, unknown=unknown, ranking=ranking,
                            latencyMs=res.latencyMs, costUsd=res.costUsd)
    return prediction, Record(id=req.id, groupId=req.groupId, source=source)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--requests", required=True, help="FrozenRequest JSONL")
    parser.add_argument("--responses", required=True, help="FrozenResponse JSONL")
    parser.add_argument("--predictions", required=True, help="New Prediction JSONL output (never overwritten)")
    parser.add_argument("--sources", required=True, help="New synchronized Record JSONL output")
    args = parser.parse_args()
    created = []
    try:
        paths = [Path(path).resolve() for path in (args.requests, args.responses, args.predictions, args.sources)]
        if len(set(paths)) != len(paths):
            raise ValueError("paths must be distinct")

        def read(path, schema):
            rows = [schema.model_validate(json.loads(line, object_pairs_hook=unique_object), strict=True)
                    for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]
            if not rows or len({row.id for row in rows}) != len(rows) or any(not row.id.strip() for row in rows):
                raise ValueError("empty or duplicate dataset")
            return rows

        requests = read(paths[0], FrozenRequest)
        responses = {row.id: row for row in read(paths[1], FrozenResponse)}
        if {row.id for row in requests} != responses.keys():
            raise ValueError("response sample mismatch")
        converted = [convert(row.model_dump(), responses[row.id].model_dump()) for row in requests]
        for index, path in enumerate(paths[2:]):
            fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            created.append(path)
            with os.fdopen(fd, "w", encoding="utf-8") as stream:
                for pair in converted:
                    stream.write(pair[index].model_dump_json() + "\n")
    except (OSError, ValueError, TypeError, RecursionError):
        for path in created:
            path.unlink(missing_ok=True)
        print(json.dumps(dict(status="unverifiable", reason="invalid, incomplete or unwritable frozen input/output")),
              file=sys.stderr)
        return 2
    print(json.dumps(dict(status="converted", samples=len(converted))))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
