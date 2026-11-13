"""Offline, closed-vocabulary projection, not a resume parser or consent gate.

This module deliberately has no caller in matching or provider code. Its output
is still personal data, not anonymous data, and is not authorized for transmission.
"""
import json


VERSION = "resume-profile-v1"
MAX_BYTES = 4096
SKILLS = frozenset({"java", "python", "javascript", "typescript", "sql", "go",
                    "rust", "linux", "git", "docker", "kubernetes"})
EXPERIENCE_BANDS = frozenset({"under_1_year", "1_to_3_years", "3_to_5_years", "5_plus_years"})
EDUCATION_LEVELS = frozenset({"secondary", "vocational", "associate", "bachelor", "master", "doctorate"})
FIELDS = frozenset({"version", "skills", "experience_band", "education_level"})


class ProfileProjectionError(ValueError):
    """A fixed-message error which never embeds rejected input."""

    def __init__(self):
        super().__init__("invalid_resume_profile")


def _object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ProfileProjectionError()
        result[key] = value
    return result


def _constant(value):
    raise ProfileProjectionError()


def _optional_code(value, allowed):
    return value is None or (type(value) is str and value in allowed)


def project_resume_profile(raw: bytes) -> dict:
    """Validate a manually curated structured profile and return a fresh projection.

    No inference from resume prose is performed. Unknown facts must be null (or
    an empty skills list), never guessed. This is input validation only: it does
    not prove truth, user review, consent, provenance or safe matching quality.
    """
    if type(raw) is not bytes or not raw or len(raw) > MAX_BYTES:
        raise ProfileProjectionError()
    try:
        data = json.loads(raw.decode("utf-8"), object_pairs_hook=_object,
                          parse_constant=_constant)
    except (ValueError, UnicodeError, RecursionError):
        raise ProfileProjectionError() from None
    if type(data) is not dict or set(data) != FIELDS or data["version"] != VERSION:
        raise ProfileProjectionError()
    skills = data["skills"]
    if type(skills) is not list or len(skills) > len(SKILLS):
        raise ProfileProjectionError()
    if any(type(code) is not str or code not in SKILLS for code in skills):
        raise ProfileProjectionError()
    if len(set(skills)) != len(skills):
        raise ProfileProjectionError()
    if not _optional_code(data["experience_band"], EXPERIENCE_BANDS):
        raise ProfileProjectionError()
    if not _optional_code(data["education_level"], EDUCATION_LEVELS):
        raise ProfileProjectionError()
    # Rebuild rather than forwarding caller objects; no identifiers or text are
    # copied through. Ordering cannot be used to carry arbitrary input metadata.
    return {"version": VERSION, "skills": sorted(skills),
            "experience_band": data["experience_band"],
            "education_level": data["education_level"]}
