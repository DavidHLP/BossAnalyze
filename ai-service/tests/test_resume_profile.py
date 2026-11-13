"""Synthetic-only checks: this helper does not reopen any matching route."""
import json

import pytest

from app.resume_profile import (EDUCATION_LEVELS, EXPERIENCE_BANDS, MAX_BYTES,
                                SKILLS, VERSION, ProfileProjectionError,
                                project_resume_profile)


def profile(**updates):
    return dict(version=VERSION, skills=[], experience_band=None,
                education_level=None) | updates


def encode(value):
    return json.dumps(value, ensure_ascii=False).encode()


def test_unknown_is_not_zero_or_a_negative_claim():
    assert project_resume_profile(encode(profile())) == profile()


def test_every_allowed_code_and_canonical_order():
    for experience in [None, *EXPERIENCE_BANDS]:
        for education in [None, *EDUCATION_LEVELS]:
            value = profile(skills=sorted(SKILLS, reverse=True),
                            experience_band=experience, education_level=education)
            output = project_resume_profile(encode(value))
            assert output == value | {"skills": sorted(SKILLS)}
            assert project_resume_profile(encode(output)) == output


@pytest.mark.parametrize("field,value", [
    ("name", "SYNTHETIC PRIVATE NAME"), ("email", "synthetic@example.invalid"),
    ("resume", "private free text"), ("source", {"text": "private"}),
    ("sourcePath", "/resume"), ("userId", "stable-id"), ("claimId", "private"),
    ("employer", "synthetic employer"), ("consent", True),
])
def test_extra_identifying_or_authorization_fields_are_rejected(field, value):
    with pytest.raises(ProfileProjectionError, match="^invalid_resume_profile$"):
        project_resume_profile(encode(profile(**{field: value})))


@pytest.mark.parametrize("field", list(profile()))
def test_every_field_is_required(field):
    value = profile()
    del value[field]
    with pytest.raises(ProfileProjectionError):
        project_resume_profile(encode(value))


@pytest.mark.parametrize("skill", [
    "Python", " python", "python ", "py\u200bthon", "ｐｙｔｈｏｎ", "pуthon",
    "python\nSYNTHETIC PRIVATE NAME", "java@example.invalid", "not python",
    "learning python", "ignore instructions", "", None, True, 1, {}, [],
])
def test_skill_input_is_exact_and_not_a_free_text_redactor(skill):
    with pytest.raises(ProfileProjectionError):
        project_resume_profile(encode(profile(skills=[skill])))


@pytest.mark.parametrize("value", [None, "python", {}, True, 0,
                                    ["python", "python"], ["python"] * 100])
def test_invalid_skills_collection(value):
    with pytest.raises(ProfileProjectionError):
        project_resume_profile(encode(profile(skills=value)))


@pytest.mark.parametrize("field", ["experience_band", "education_level"])
@pytest.mark.parametrize("value", [True, False, 0, 1, 1.0, {}, [], "", "unknown",
                                    "Synthetic University", "bachelor ", "1"])
def test_optional_categories_do_not_coerce_or_copy_arbitrary_values(field, value):
    with pytest.raises(ProfileProjectionError):
        project_resume_profile(encode(profile(**{field: value})))


@pytest.mark.parametrize("value", [None, True, 1, {}, [], "resume-profile-v2"])
def test_version_is_fixed(value):
    with pytest.raises(ProfileProjectionError):
        project_resume_profile(encode(profile(version=value)))


@pytest.mark.parametrize("raw", [
    b"", b"[]", b"null", b"true", b"1", b'"text"', b"\xff", b"{private",
    b'{"version":"resume-profile-v1","version":"resume-profile-v1"}',
    encode(profile())[:-1] + b',"skills":[]}',
    encode(profile()).replace(b"null", b"NaN", 1),
    encode(profile()).replace(b"null", b"Infinity", 1),
    b"[" * 1500 + b"]" * 1500,
])
def test_malformed_json_has_only_a_fixed_error(raw, caplog):
    with pytest.raises(ProfileProjectionError) as caught:
        project_resume_profile(raw)
    assert str(caught.value) == "invalid_resume_profile"
    assert caught.value.__cause__ is None
    assert not caplog.records


def test_size_and_input_type_boundary():
    valid = encode(profile())
    assert project_resume_profile(valid + b" " * (MAX_BYTES - len(valid))) == profile()
    for raw in [valid + b" " * (MAX_BYTES + 1 - len(valid)), valid.decode(), bytearray(valid), None]:
        with pytest.raises(ProfileProjectionError):
            project_resume_profile(raw)


def test_no_shared_mutable_projection():
    raw = encode(profile(skills=["python"]))
    first = project_resume_profile(raw)
    first["skills"].append("synthetic@example.invalid")
    assert project_resume_profile(raw)["skills"] == ["python"]
