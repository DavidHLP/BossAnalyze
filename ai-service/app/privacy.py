from .jev import ServiceError


def require_safe_matching():
    """Retention headers and regex redaction are not a data-minimization boundary."""
    # Intentionally no environment/configuration bypass; reopening requires reviewed code.
    raise ServiceError("resume_matching_disabled", 503)
