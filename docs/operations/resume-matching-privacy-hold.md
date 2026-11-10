# Resume matching privacy hold

Resume matching is temporarily unavailable. Both public matching routes (including the legacy route), direct Java matching entry points, the Python HTTP endpoint, and the Python pipeline fail closed with HTTP 503 before retrieving a stored resume, querying job batches, or contacting a model.

- Java error: `AI_RESUME_MATCHING_DISABLED`.
- Python error: `resume_matching_disabled`.
- Python readiness reports `resumeMatching.enabled: false`; job analysis and liveness retain their existing behavior.
- There is deliberately no environment variable or synthetic-mode switch that enables matching. ZDR controls retention; it does not establish data minimization or permission to transmit resume data. Existing regex redaction is not considered a safety boundary.

This hold covers the explicit resume-matching workflows. It does not certify that arbitrary strings submitted as job facts are free of personal data. Do not put resumes or private data into job-analysis inputs as a workaround. No real model request, production deployment, quality approval, scheduling, or writeback is authorized by this change.

## Verification and reopening

`tests/test_resume_privacy.py` and `ResumePrivacyTest` exercise the real, unmocked guards with synthetic text, stored-ID requests, direct callers, empty batches, Unicode and long inputs. They assert no network request, budget creation, database query, score, or input echo. Existing algorithm/transport regression tests explicitly mock the guard **inside tests only** so future changes do not silently lose those contracts; they are not evidence that production matching is enabled.

The older controlled-workflow fixtures and operation reports describe the former enabled matching behavior. Their scored/partial-match assertions are intentionally incompatible with this privacy hold and must not be used to claim current end-to-end matching acceptance. Do not rerun those stateful acceptance scripts to bypass the hold.

Reopening requires a reviewed data-minimization and explicit outbound-authorization design, independent adversarial tests covering every resume representation and entry point, a verified provider contract, and separate real-service acceptance. Removing this guard solely because CI is green or ZDR is enabled is not sufficient.
