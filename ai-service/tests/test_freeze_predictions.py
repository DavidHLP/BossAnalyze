"""Synthetic conversion contracts; these fixtures are not human quality evidence."""
from copy import deepcopy
import json
import subprocess
import sys

import pytest

from app.freeze_predictions import convert
from app.evaluate import Gold, Prediction, Record, load


MODEL = "typesafe/jev-1.14.0"
DIMENSIONS = ("skills", "relevant_experience", "experience", "education")


def frozen():
    request = dict(id="synthetic-request", groupId="synthetic-jd-group", resume="Python 😀",
                   jobs=[dict(jobId="岗位/~甲", detailData={
                       "jobDescription": {"技能/~": "Python 😀；五险"},
                       "jobTags": ["Python 😀"], "analysisReport": "不是事实来源"})])
    evidence = dict(sourcePath="/jobDescription/技能~1~0", start=0, end=8, text="Python 😀")
    benefit = dict(sourcePath="/jobDescription/技能~1~0", start=9, end=11, text="五险")
    extraction = dict(candidateId="c0", text="Python 😀", evidence=[evidence, deepcopy(evidence)],
                      probability=.99, importance=3.75)
    analysis = dict(jobId="岗位/~甲", status="needs_review", requirements=[extraction],
                    benefits=[dict(candidateId="c1", text="五险", evidence=[benefit],
                                   probability=.99, importance=None)],
                    coreRequirements=[deepcopy(extraction)], model=MODEL, pipelineVersion="jev-1",
                    policyVersion="jev-policy-1", requestId="synthetic-analysis", usage=dict(inputTokens=None, outputTokens=None))
    dimensions = [dict(name=name, status="scored", rawScore=3.5, confidence=1.,
                       probabilities={"3": .5, "4": .5}, evidence=[
                           dict(evidence, sourcePath="/job/jobDescription/技能~1~0"),
                           dict(sourcePath="/resume", start=0, end=8, text="Python 😀")])
                  for name in DIMENSIONS]
    match = dict(items=[dict(jobId="岗位/~甲", status="scored", score=8.749, dimensions=dimensions)],
                 complete=True, model=MODEL, policyVersion="jev-policy-1", requestId="synthetic-match",
                 usage=dict(inputTokens=None, outputTokens=None))
    return request, dict(id="synthetic-request", analyses=[analysis], match=match)


def test_single_job_preserves_unicode_sources_and_deduplicates_evidence():
    request, response = frozen()
    before = deepcopy((request, response))
    prediction, record = convert(request, response)
    assert record.source == {"/job/jobDescription/技能~1~0": "Python 😀；五险",
                             "/job/jobTags/0": "Python 😀", "/resume": "Python 😀"}
    assert [e.model_dump() for e in prediction.requirements] == [
        dict(sourcePath="/job/jobDescription/技能~1~0", start=0, end=8, text="Python 😀")]
    assert len(prediction.candidates) == 4  # Same text at different sources remains separate.
    assert prediction.coreRequirements == prediction.requirements
    assert prediction.ranking == ["岗位/~甲"]
    assert prediction.unknown == {"岗位/~甲:skills": False, "岗位/~甲:relevant_experience": False,
                                  "岗位/~甲:experience": False, "岗位/~甲:education": False}
    assert prediction.latencyMs is prediction.costUsd is None
    assert (request, response) == before


def test_unknown_cannot_become_a_ranked_score():
    request, response = frozen()
    dimension = response["match"]["items"][0]["dimensions"][0]
    dimension.update(status="unknown", rawScore=None, probabilities=None)
    with pytest.raises(ValueError, match="score state"):
        convert(request, response)


def test_cli_multijob_conversion_is_consumed_by_existing_runner(tmp_path):
    request, response = frozen()
    for job_id, score in [("岗位乙", 8.751), ("unknown", None)]:
        request["jobs"].append(dict(request["jobs"][0], jobId=job_id))
        response["analyses"].append(dict(response["analyses"][0], jobId=job_id))
        item = deepcopy(response["match"]["items"][0])
        item.update(jobId=job_id, score=score)
        if score is None:
            item["status"] = "insufficient_evidence"
            item["dimensions"][0].update(status="unknown", rawScore=None, probabilities=None)
            item["dimensions"][1].update(status="not_applicable", rawScore=None, probabilities=None)
        response["match"]["items"].append(item)
    response["match"]["complete"] = False
    response["analyses"].reverse()
    response["match"]["items"].reverse()
    requests, responses = tmp_path / "requests.jsonl", tmp_path / "responses.jsonl"
    predictions, sources = tmp_path / "predictions.jsonl", tmp_path / "sources.jsonl"
    requests.write_text(json.dumps(request, ensure_ascii=False) + "\n", encoding="utf-8")
    responses.write_text(json.dumps(response, ensure_ascii=False) + "\n", encoding="utf-8")
    result = subprocess.run([sys.executable, "-m", "app.freeze_predictions", "--requests", str(requests),
                             "--responses", str(responses), "--predictions", str(predictions),
                             "--sources", str(sources)], capture_output=True, text=True)
    assert result.returncode == 0, result.stderr
    prediction = load(predictions, Prediction)[0]
    source = load(sources, Record)[0].source
    assert sources.stat().st_mode & 0o777 == predictions.stat().st_mode & 0o777 == 0o600
    assert prediction.ranking == ["岗位乙", "岗位/~甲"]  # These round to the same legacy integer.
    assert prediction.unknown["unknown:skills"] is True
    assert prediction.unknown["unknown:relevant_experience"] is False
    assert "/jobs/岗位~1~0甲/jobDescription/技能~1~0" in source
    assert {e.sourcePath for e in prediction.requirements} == {
        "/jobs/岗位~1~0甲/jobDescription/技能~1~0", "/jobs/岗位乙/jobDescription/技能~1~0",
        "/jobs/unknown/jobDescription/技能~1~0"}
    # Synthetic runner contract only: no claim of independent human truth.
    requirements = [dict(sourcePath=path, start=0, end=8, text="Python 😀") for path in (
        "/jobs/岗位~1~0甲/jobDescription/技能~1~0", "/jobs/岗位乙/jobDescription/技能~1~0",
        "/jobs/unknown/jobDescription/技能~1~0")]
    benefits = [dict(e, start=9, end=11, text="五险") for e in requirements]
    gold = Gold(id="synthetic-request", groupId="synthetic-jd-group", source=source, split="test",
                reviewStatus="reviewed", reviewer="synthetic-test-only", annotationOrigin="human",
                dataOrigin="synthetic", subgroup="synthetic", requirements=requirements,
                benefits=benefits, coreRequirements=requirements,
                unknown={f"{job_id}:{name}": job_id == "unknown" and name == "skills"
                         for job_id in ("岗位/~甲", "岗位乙", "unknown") for name in DIMENSIONS},
                ranking=["岗位乙", "岗位/~甲"])
    gold_path, development = tmp_path / "gold.jsonl", tmp_path / "development.jsonl"
    gold_path.write_text(gold.model_dump_json() + "\n", encoding="utf-8")
    development.write_text("", encoding="utf-8")
    runner = subprocess.run([sys.executable, "-m", "app.evaluate", "--gold", str(gold_path),
                             "--predictions", str(predictions), "--development", str(development),
                             "--min-samples", "1", "--min-ranking-agreement", "1"],
                            capture_output=True, text=True)
    assert runner.returncode == 2, runner.stderr
    report = json.loads(runner.stdout)
    assert report["samples"] == 1
    assert report["status"] == report["qualityApproval"] == "unverifiable"
    assert report["metrics"]["evidenceConsistency"] == report["metrics"]["rankingAgreement"] == 1
    assert report["metrics"]["cost"]["totalUsd"] is None
    assert report["metrics"]["latency"]["p95Ms"] is None


@pytest.mark.parametrize("case", ["sample", "job", "duplicate-job", "missing-analysis", "analysis-job",
                                 "match-job", "duplicate-match", "missing-match-job", "missing-dimension",
                                 "failed", "missing-match", "bad-span", "cross-job-path", "foreign-model",
                                 "match-policy", "bool-score"])
def test_invalid_or_failed_frozen_responses_are_rejected_without_dropping_samples(case):
    request, response = frozen()
    if case == "sample":
        response["id"] = "another-request"
    elif case == "job":
        request["jobs"][0]["jobId"] = " "
    elif case == "duplicate-job":
        request["jobs"].append(deepcopy(request["jobs"][0]))
    elif case == "missing-analysis":
        response["analyses"] = []
    elif case == "analysis-job":
        response["analyses"][0]["jobId"] = "foreign-job"
    elif case == "match-job":
        response["match"]["items"][0]["jobId"] = "foreign-job"
    elif case == "duplicate-match":
        response["match"]["items"].append(deepcopy(response["match"]["items"][0]))
    elif case == "missing-match-job":
        response["match"]["items"] = []
    elif case == "missing-dimension":
        response["match"]["items"][0]["dimensions"].pop()
    elif case == "failed":
        response["match"]["items"][0].update(status="failed", score=None, dimensions=[])
        response["match"]["complete"] = False
    elif case == "missing-match":
        del response["match"]
    elif case == "bad-span":
        response["analyses"][0]["requirements"][0]["evidence"][0]["text"] = "Faked!!!"
    elif case == "cross-job-path":
        response["match"]["items"][0]["dimensions"][0]["evidence"][0]["sourcePath"] = "/jobs/foreign/jobDescription"
    elif case == "foreign-model":
        response["analyses"][0]["model"] = "unapproved-model"
    elif case == "match-policy":
        response["match"]["policyVersion"] = "foreign-policy"
    elif case == "bool-score":
        response["match"]["items"][0]["score"] = True
    with pytest.raises(ValueError):
        convert(request, response)


@pytest.mark.parametrize("latency,cost", [(None, None), (0, "0"), (12.75, "0.00000000000000000001")])
def test_telemetry_preserves_observed_values_and_unknown(latency, cost):
    request, response = frozen()
    response.update(latencyMs=latency, costUsd=cost)
    prediction, _ = convert(request, response)
    assert prediction.latencyMs == latency
    assert prediction.costUsd == cost


@pytest.mark.parametrize("field,value", [("latencyMs", True), ("latencyMs", "12"),
                                        ("costUsd", 0), ("costUsd", "NaN")])
def test_invalid_telemetry_is_not_coerced(field, value):
    request, response = frozen()
    response[field] = value
    with pytest.raises(ValueError):
        convert(request, response)


@pytest.mark.parametrize("case", ["later-failure", "http-error", "sample-set", "duplicate-id", "duplicate-key",
                                 "existing-output", "aliased-output"])
def test_cli_rejects_incomplete_datasets_and_preserves_existing_files(tmp_path, case):
    request, response = frozen()
    requests = tmp_path / "requests.jsonl"
    responses = tmp_path / "responses.jsonl"
    predictions = tmp_path / "predictions.jsonl"
    sources = tmp_path / "sources.jsonl"
    request_text = json.dumps(request, ensure_ascii=False) + "\n"
    response_text = json.dumps(response, ensure_ascii=False) + "\n"
    if case == "later-failure":
        request_text += json.dumps(dict(request, id="second")) + "\n"
        broken = deepcopy(response)
        broken["id"] = "second"
        broken["match"]["items"][0].update(status="failed", score=None, dimensions=[])
        response_text += json.dumps(broken) + "\n"
    elif case == "http-error":
        response_text = json.dumps(dict(id=request["id"], analyses=[dict(error=dict(code="upstream_error"))]))
    elif case == "sample-set":
        response_text = json.dumps(dict(response, id="another-request"))
    elif case == "duplicate-id":
        response_text *= 2
    elif case == "duplicate-key":
        response_text = response_text.replace('"analyses":', '"id":"duplicate", "analyses":', 1)
    elif case == "existing-output":
        sources.write_text("preexisting-output", encoding="utf-8")
    elif case == "aliased-output":
        sources = requests
    requests.write_text(request_text, encoding="utf-8")
    responses.write_text(response_text, encoding="utf-8")
    result = subprocess.run([sys.executable, "-m", "app.freeze_predictions", "--requests", str(requests),
                             "--responses", str(responses), "--predictions", str(predictions),
                             "--sources", str(sources)], capture_output=True, text=True)
    assert result.returncode == 2
    assert result.stdout == ""
    assert json.loads(result.stderr)["status"] == "unverifiable"
    assert "Python" not in result.stderr and "upstream_error" not in result.stderr
    assert requests.read_text(encoding="utf-8") == request_text
    assert responses.read_text(encoding="utf-8") == response_text
    assert not predictions.exists()
    if case == "existing-output":
        assert sources.read_text(encoding="utf-8") == "preexisting-output"
    elif case != "aliased-output":
        assert not sources.exists()


def test_analysis_only_does_not_invent_matching_unknown_or_ranking():
    request, response = frozen()
    del request["resume"]
    del response["match"]
    prediction, record = convert(request, response)
    assert prediction.unknown == {} and prediction.ranking == []
    assert "/resume" not in record.source
    assert len(prediction.candidates) == 3
