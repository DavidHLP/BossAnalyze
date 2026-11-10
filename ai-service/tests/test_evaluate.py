from copy import deepcopy

import pytest

from app.evaluate import Gold, Prediction, Record, evaluate, load


def sample():
    evidence = dict(sourcePath="/jobDescription", start=0, end=6, text="Python")
    gold = dict(id="unit-only", groupId="unit-group", source={"/jobDescription": "Python；五险"},
                split="test", reviewStatus="reviewed", reviewer="synthetic-unit-reviewer",
                annotationOrigin="human", dataOrigin="synthetic", subgroup="unit",
                requirements=[evidence], benefits=[dict(sourcePath="/jobDescription", start=7, end=9, text="五险")],
                unknown={"skills": False, "education": True}, ranking=["a", "b", "c"])
    prediction = {k: deepcopy(gold[k]) for k in ("id", "requirements", "benefits", "unknown", "ranking")}
    prediction.update(model="typesafe/jev-1.14.0", candidates=gold["requirements"] + gold["benefits"])
    return gold, prediction


def run(g, p, development=()):
    return evaluate([Gold.model_validate(g)], [Prediction.model_validate(p)], list(development), 1, 1)


def test_perfect_synthetic_metrics_never_approve_quality():
    g, p = sample()
    result = run(g, p)
    assert result["status"] == result["qualityApproval"] == "unverifiable"
    assert result["samples"] == 1
    metrics = result["metrics"]
    assert metrics["candidateRecall"] == metrics["evidenceConsistency"] == metrics["rankingAgreement"] == 1
    assert metrics["classes"]["requirements"]["precision"] == metrics["classes"]["benefits"]["recall"] == 1
    assert metrics["unknownCoverage"] == .5 and metrics["unknownAgreement"] == 1


def test_errors_are_measured_not_disguised_as_unknown():
    g, p = sample()
    p["benefits"] = []
    p["requirements"][0]["text"] = "FAKE!!"
    p["candidates"] = []
    p["ranking"] = ["b", "a"]
    p["unknown"]["skills"] = True
    result = run(g, p)
    assert result["status"] == "fail"
    m = result["metrics"]
    assert m["classes"]["benefits"]["recall"] == 0
    assert m["classes"]["benefits"]["precision"] is None
    assert m["candidateRecall"] == m["evidenceConsistency"] == m["rankingAgreement"] == 0
    assert m["counts"]["rankingPairs"] == 3
    assert m["unknownCoverage"] == 1 and m["unknownAgreement"] == .5


@pytest.mark.parametrize("field,value", [("reviewStatus", "pending"), ("reviewer", ""),
    ("annotationOrigin", "jev"), ("split", "train"), ("unknown", {}), ("subgroup", "")])
def test_unreviewed_incomplete_or_self_labelled_gold_rejected(field, value):
    g, p = sample()
    g[field] = value
    with pytest.raises(ValueError):
        run(g, p)


@pytest.mark.parametrize("source,group", [("completely distinct text", "unit-group"),
    ("Python；五险", "other"), ("Python;五险！", "other")])
def test_split_group_exact_and_normalized_content_leakage_rejected(source, group):
    g, p = sample()
    with pytest.raises(ValueError):
        run(g, p, [Record(id="development", groupId=group, source={"/jobDescription": source})])


@pytest.mark.parametrize("path", ["/jobDescription", "/job/jobDescription/requirements",
                                  "/jobs/one/jobDescription/requirements",
                                  "/job/requirements", "/jobs/one/text"])
def test_same_job_with_different_resumes_cannot_cross_splits(path):
    g, p = sample()
    jd = g["source"].pop("/jobDescription")
    g["source"] = {path: jd, "/resume": "甲" * 200}
    for record in (g, p):
        for field in ("requirements", "benefits", "candidates"):
            for evidence in record.get(field, []):
                evidence["sourcePath"] = path
    development = Record(id="dev", groupId="other", source={"/jobs/dev/jobDescription": jd,
                                                            "/resume": "乙" * 200})
    with pytest.raises(ValueError, match="test leakage"):
        run(g, p, [development])


def test_missing_annotations_and_duplicate_json_keys_rejected(tmp_path):
    g, _ = sample()
    del g["benefits"]
    with pytest.raises(ValueError):
        Gold.model_validate(g)
    path = tmp_path / "invalid.jsonl"
    path.write_text('{"id":"a","id":"b"}')
    with pytest.raises(ValueError):
        load(path, Record)


def test_no_comparable_ranking_or_zero_denominators_are_unverifiable():
    g, p = sample()
    g["ranking"] = p["ranking"] = []
    result = run(g, p)
    assert result["status"] == "unverifiable"
    assert result["metrics"]["rankingAgreement"] is None


def test_sample_sets_unknown_keys_and_ranking_ids_must_match():
    for field, value in (("id", "other"), ("unknown", {}), ("ranking", ["unexpected"]),
                         ("model", "fallback")):
        g, p = sample()
        p[field] = value
        with pytest.raises(ValueError):
            run(g, p)


def test_near_duplicate_not_just_exact_text_is_rejected():
    g, p = sample()
    g["source"]["/extra"] = "这是一个仅供单元测试使用的长描述，包含许多不同的经历条件和示例文字。" * 5
    source = dict(g["source"])
    source["/extra"] += "新增几个字"
    with pytest.raises(ValueError):
        run(g, p, [Record(id="dev", groupId="different", source=source)])


def test_cli_missing_gold_returns_safe_unverifiable(tmp_path, monkeypatch, capsys):
    from app.evaluate import main
    empty = tmp_path / "empty.jsonl"
    empty.write_text("")
    monkeypatch.setattr("sys.argv", ["evaluate", "--gold", str(empty), "--predictions", str(empty),
                                   "--development", str(empty), "--min-samples", "100",
                                   "--min-ranking-agreement", "0.9"])
    assert main() == 2
    import json
    result = json.loads(capsys.readouterr().out)
    assert result["status"] == "unverifiable" and result["samples"] == 0


def test_gate_status_with_simulated_provenance_is_not_quality_approval():
    g, p = complete_sample()
    # Unit-only metadata simulation, not a reviewed public-data artifact.
    g["dataOrigin"] = "real_public"
    assert run(g, p)["status"] == "pass"
    assert run(g, p)["qualityApproval"] == "unverifiable"
    assert evaluate([Gold.model_validate(g)], [Prediction.model_validate(p)], [], 2, 1)["status"] == "unverifiable"


def complete_sample():
    g, p = sample()
    # Entirely simulated labels, provenance and telemetry for runner contract tests.
    g["coreRequirements"] = deepcopy(g["requirements"])
    p.update(coreRequirements=deepcopy(p["requirements"]), latencyMs=0, costUsd="0")
    return g, p


def test_old_jsonl_loads_but_missing_observations_cannot_pass(tmp_path):
    import json
    g, p = sample()
    g["dataOrigin"] = "real_public"  # Simulated provenance only.
    for name, row, schema in [("gold", g, Gold), ("prediction", p, Prediction)]:
        path = tmp_path / name
        path.write_text(json.dumps(row))
        assert load(path, schema)[0].coreRequirements is None
    result = run(g, p)
    assert result["status"] == "unverifiable"
    m = result["metrics"]
    assert m["coreSelection"] == dict(tp=0, predicted=0, gold=0, precision=None, recall=None)
    assert m["cost"] == dict(totalUsd=None, knownSamples=0, coverage=0)
    assert m["latency"] == dict(p95Ms=None, knownSamples=0, coverage=0, percentileMethod="nearest-rank")


def test_core_wrong_selection_and_missing_prediction_are_distinct():
    g, p = complete_sample()
    second = dict(sourcePath="/jobDescription", start=7, end=9, text="五险")
    g["requirements"].append(second)
    p["requirements"].append(second)
    p["coreRequirements"] = [second]
    result = run(g, p)
    assert result["status"] == "fail"
    assert result["metrics"]["coreSelection"] == dict(tp=0, predicted=1, gold=1, precision=0, recall=0)
    p["coreRequirements"] = None
    assert run(g, p)["status"] == "unverifiable"
    p["coreRequirements"] = []
    m = run(g, p)["metrics"]["coreSelection"]
    assert m["recall"] == 0 and m["precision"] is None


@pytest.mark.parametrize("side", ["gold", "prediction"])
@pytest.mark.parametrize("error", ["outside", "duplicate", "raw", "missing-source"])
def test_core_evidence_validation(side, error):
    g, p = complete_sample()
    row = g if side == "gold" else p
    if error == "outside":
        row["coreRequirements"] = deepcopy(row["benefits"])
    elif error == "duplicate":
        row["coreRequirements"] *= 2
    else:
        row["coreRequirements"][0]["text"] = "FAKE!!"
        if error == "missing-source":
            row["coreRequirements"][0]["sourcePath"] = "/absent"
        row["requirements"] = deepcopy(row["coreRequirements"])
    if side == "prediction" and error in {"raw", "missing-source"}:
        assert run(g, p)["status"] == "fail"
    else:
        with pytest.raises(ValueError):
            run(g, p)


@pytest.mark.parametrize("field,values", [
    ("latencyMs", [True, False, "0", -1, float("nan"), float("inf"), -float("inf")]),
    ("costUsd", [True, False, 0, 0.1, -1, "-0.1", "NaN", "Infinity", "1e-3", "", " 1", "+1", ".1"])])
def test_invalid_telemetry_rejected(field, values):
    for value in values:
        _, p = complete_sample()
        p[field] = value
        with pytest.raises(ValueError):
            Prediction.model_validate(p)


def test_zero_observation_is_not_unknown_and_core_empty_is_not_missing():
    g, p = complete_sample()
    m = run(g, p)["metrics"]
    assert m["cost"] == dict(totalUsd="0", knownSamples=1, coverage=1)
    assert m["latency"]["p95Ms"] == 0 and m["latency"]["coverage"] == 1
    g["coreRequirements"] = p["coreRequirements"] = []
    assert run(g, p)["metrics"]["coreSelection"]["precision"] is None
    assert run(g, p)["status"] == "unverifiable"
    result = evaluate([], [], [], 1, 1)
    assert result["metrics"]["cost"]["coverage"] is None
    assert result["metrics"]["latency"]["p95Ms"] is None


def population(size):
    gold, predictions = [], []
    for i in range(size):
        g, p = complete_sample()
        # Distinct raw source characters prevent the existing near-duplicate leak guard.
        source = chr(0x4e00 + i) * 10
        g.update(id=f"unit-only-{i}", groupId=f"unit-group-{i}",
                 source={"/jobDescription": source}, subgroup="first" if i < 20 else "last")
        evidence = dict(sourcePath="/jobDescription", start=0, end=1, text=source[0])
        for row in (g, p):
            row.update(requirements=[deepcopy(evidence)], benefits=[deepcopy(evidence)],
                       coreRequirements=[deepcopy(evidence)])
        p.update(id=g["id"], candidates=[deepcopy(evidence)], latencyMs=i + 1, costUsd="0.01")
        gold.append(g)
        predictions.append(p)
    return gold, predictions


def run_population(gold, predictions):
    return evaluate([Gold.model_validate(g) for g in gold],
                    [Prediction.model_validate(p) for p in predictions], [], 1, 1)


@pytest.mark.parametrize("size,p95", [(1, 1), (20, 19), (21, 20)])
def test_nearest_rank_complete_population_and_subgroups(size, p95):
    g, p = population(size)
    result = run_population(g, p)
    assert result["metrics"]["latency"] == dict(p95Ms=p95, knownSamples=size,
                                               coverage=1, percentileMethod="nearest-rank")
    assert result["metrics"]["coreSelection"] == dict(tp=size, predicted=size, gold=size, precision=1, recall=1)
    if size == 21:
        assert result["subgroups"]["first"]["latency"]["p95Ms"] == 19
        assert result["subgroups"]["last"]["latency"]["p95Ms"] == 21
        assert result["subgroups"]["first"]["cost"]["totalUsd"] == "0.20"
        assert result["subgroups"]["last"]["coreSelection"]["tp"] == 1


def test_partial_observations_null_aggregates_but_preserve_known_failure():
    g, p = population(21)
    p[-1].update(latencyMs=None, costUsd=None, coreRequirements=None)
    result = run_population(g, p)
    assert result["status"] == "unverifiable"
    assert result["metrics"]["cost"] == dict(totalUsd=None, knownSamples=20, coverage=20/21)
    assert result["metrics"]["latency"]["p95Ms"] is None
    assert result["metrics"]["latency"]["knownSamples"] == 20
    assert result["metrics"]["coreSelection"]["precision"] is None
    assert result["subgroups"]["first"]["cost"]["totalUsd"] == "0.20"
    assert result["subgroups"]["last"]["cost"]["totalUsd"] is None
    # A wrong core choice must fail even when another record lacks labels/telemetry.
    for i in range(2):
        second = dict(sourcePath="/jobDescription", start=1, end=2, text=g[i]["source"]["/jobDescription"][1])
        g[i]["requirements"].append(deepcopy(second))
        p[i]["requirements"].append(deepcopy(second))
        p[i]["candidates"].append(deepcopy(second))
        p[i]["coreRequirements"] = [second]
    failed = run_population(g, p)
    assert failed["status"] == "fail"
    assert failed["metrics"]["classes"]["requirements"]["precision"] == 1
    assert failed["metrics"]["candidateRecall"] == failed["metrics"]["evidenceConsistency"] == 1
    g[-1]["subgroup"] = "first"
    assert run_population(g, p)["status"] == "fail"
    g[-1]["coreRequirements"] = None
    p[-1]["coreRequirements"] = deepcopy(p[-1]["requirements"])
    assert run_population(g, p)["status"] == "fail"


def test_exact_cost_does_not_use_default_decimal_precision():
    from decimal import localcontext
    g, p = population(2)
    p[0]["costUsd"] = "123456789012345678901234567890.123456789012345678901234567890"
    p[1]["costUsd"] = "0.000000000000000000000000000001"
    with localcontext() as context:
        context.prec = 3
        result = run_population(g, p)
    assert result["metrics"]["cost"]["totalUsd"] == "123456789012345678901234567890.123456789012345678901234567891"


@pytest.mark.parametrize("case,exit_code", [("complete", 0), ("old", 2), ("wrong", 1), ("invalid", 2)])
def test_cli_complete_old_failure_and_invalid_telemetry(case, exit_code, tmp_path, monkeypatch, capsys):
    import json
    from app.evaluate import main
    g, p = sample() if case == "old" else complete_sample()
    g["dataOrigin"] = "real_public"  # Unit-only provenance simulation, never a real human gold set.
    if case == "wrong":
        p["ranking"].reverse()
    if case == "invalid":
        p["latencyMs"] = True
    for name, row in [("gold", g), ("predictions", p)]:
        (tmp_path / name).write_text(json.dumps(row))
    (tmp_path / "development").write_text("")
    monkeypatch.setattr("sys.argv", ["evaluate", "--gold", str(tmp_path / "gold"),
        "--predictions", str(tmp_path / "predictions"), "--development", str(tmp_path / "development"),
        "--min-samples", "1", "--min-ranking-agreement", "1"])
    assert main() == exit_code
    result = json.loads(capsys.readouterr().out)
    assert result["qualityApproval"] == "unverifiable"


@pytest.mark.parametrize("side,field", [("gold", "coreRequirements"), ("prediction", "coreRequirements"),
                                       ("prediction", "latencyMs"), ("prediction", "costUsd")])
def test_each_missing_observation_blocks_otherwise_passing_report(side, field):
    g, p = complete_sample()
    g["dataOrigin"] = "real_public"  # Simulated metadata only.
    assert run(g, p)["status"] == "pass"
    (g if side == "gold" else p)[field] = None
    assert run(g, p)["status"] == "unverifiable"
    p["ranking"].reverse()
    assert run(g, p)["status"] == "fail"


@pytest.mark.parametrize("side", ["gold", "prediction"])
@pytest.mark.parametrize("identity", ["", "   "])
def test_blank_ranking_job_identity_rejected(side, identity):
    g, p = complete_sample()
    g["ranking"] = [identity, "valid"]
    p["ranking"] = [identity, "valid"] if side == "prediction" else ["valid"]
    with pytest.raises(ValueError, match="ranking ids"):
        run(g, p)


def test_group_ranking_ids_are_request_jobs_not_gold_record_ids():
    g, p = complete_sample()
    # One request record can rank several jobs; these are not dataset row IDs.
    g["source"] = {"/jobs/a/text": "Python；五险", "/jobs/b/text": "Java", "/jobs/c/text": "Go"}
    for row in (g, p):
        for field in ("requirements", "benefits", "coreRequirements", "candidates"):
            for evidence in row.get(field, []):
                evidence["sourcePath"] = "/jobs/a/text"
    result = run(g, p)
    assert result["metrics"]["counts"]["rankingPairs"] == 3
    assert result["metrics"]["rankingAgreement"] == 1
    assert result["qualityApproval"] == "unverifiable"


def test_distinct_jobs_with_shared_resume_are_not_job_leakage():
    g, p = sample()
    g["source"]["/resume"] = "甲" * 10
    development = Record(id="dev", groupId="other", source={"/job/requirements": "乙" * 200,
                                                            "/resume": "甲" * 10})
    assert run(g, p, [development])["status"] == "unverifiable"
