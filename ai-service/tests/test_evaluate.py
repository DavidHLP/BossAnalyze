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
    g, p = sample()
    # Unit-only metadata simulation, not a reviewed public-data artifact.
    g["dataOrigin"] = "real_public"
    assert run(g, p)["status"] == "pass"
    assert run(g, p)["qualityApproval"] == "unverifiable"
    assert evaluate([Gold.model_validate(g)], [Prediction.model_validate(p)], [], 2, 1)["status"] == "unverifiable"
