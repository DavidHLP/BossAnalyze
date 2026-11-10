# Resume matching: staged reopening proposal

Status: proposal, not an enabled feature or outbound-data approval.
Baseline: main `13cbe7e` (2026-11-10). Tracking: #54 and #48.

## Preserve the safety boundary

Keep Java inline/stored-ID and Python HTTP/direct-pipeline matching disabled. Do not add a flag that skips the guard. Do not reuse job-analysis as a resume transport. No real resume, provider call, database write, service restart, scheduling, or quality approval is required to develop this proposal.

## Recommended first implementation

Use a local-only projection into an explicit, versioned vocabulary instead of trying to redact arbitrary resume prose. A first version can support a reviewed list of skill codes and coarse, bounded experience/education categories. Names, contacts, dates of birth, addresses, employers, schools, URLs, original filenames, arbitrary text and sensitive attributes are excluded. Unknown or unrepresentable facts stay unknown, never become a negative score.

Projection must not infer a claim from a keyword occurrence alone: negation, desired skills and third-party descriptions are not candidate experience. Require the user to review the proposed structured claims before enabling outbound use. Retain original source spans and the mapping to generated claim IDs locally; they are not provider payload fields. Use request-local opaque claim IDs rather than stable user/resume IDs. The UI must disclose the exact projected fields, provider, retention policy and purpose when collecting outbound authorization.

The existing free-text matching protocol and evidence offsets cannot simply be reused: provider evidence would refer to the minimal projection, not the original resume. Define a new explicit protocol version and translate only validated claim IDs back to local evidence. Never accept provider-created raw spans or source paths as proof of local resume content.

## Independent checks before reopening

1. Audit all entry points and serializers, including legacy, stored-ID, direct Java/Python calls, error logging, tracing and retry payloads.
2. Add adversarial synthetic cases for identifiers embedded in skills, mixed-script/zero-width text, Unicode confusables, prompt-like text, nested/extra fields, nulls, coercion, size limits, and negation. Reject unrecognized codes rather than copying strings.
3. Capture the final HTTP body through a mock transport and assert that no free text, source span, stable ID or disallowed field leaves the process. Assert failure before provider/budget use when projection or authorization is missing.
4. Separately validate authorization identity and scope: no cross-user reuse, no persisted-ID substitution and no stale approval after projected facts or destination change.
5. Keep quality approval, scheduling and automatic writeback disabled. Validate all supported dimensions against independently human-reviewed gold; unsupported dimensions must remain unknown. Synthetic tests validate mechanics, not extraction accuracy or matching quality.
6. Perform a reviewed code change to remove each guard only after its replacement boundary exists, then controlled real-service acceptance with approved data. Removing ZDR as a requirement does not authorize transmitting resume data.

## Dependencies that cannot be fabricated

- Independent human gold/holdout with frozen criteria and actual observations.
- Explicit outbound authorization for the exact projected data and provider.
- The owning runtime for actual restart/no-login recovery acceptance. A cloud test is not evidence of that machine surviving reboot.

Until these steps are completed, the honest result is a working ordinary job-analysis route and intentionally unavailable resume matching.

## Offline prototype in this change

`ai-service/app/resume_profile.py` accepts at most 4096 UTF-8 JSON bytes with exactly
`version`, `skills`, `experience_band` and `education_level`. The version is
`resume-profile-v1`; each value is drawn from the finite vocabularies in the
module. Null categories and an empty skills list mean not provided/unknown, not
zero experience or lack of skills. Unsupported facts must not be coerced to the
nearest supported category. Unknown, additional, duplicate, malformed, or
implicitly coerced fields are rejected with a fixed error code. No Unicode or
whitespace normalization is used to turn near-matches into accepted codes.

This prototype is deliberately not imported by any HTTP endpoint, matching
pipeline, serializer or provider. It does not extract assertions from raw prose,
prove that assertions are true, provide consent, or certify anonymity. A profile
can still be personally identifying. It provides no transmission operation or
switch to reopen matching. The initial vocabulary is an implementation sample,
not a claim of complete domain coverage or validated scoring suitability.

Synthetic tests cover the complete finite category combinations, malformed and
repeated keys, identifiers in extra fields, free text, Unicode near-matches,
coercion, nulls, duplicate skills, nested input, invalid UTF-8 and byte boundaries.
Existing privacy tests still exercise actual rejection before body/model use.
The final outgoing-body and cross-user authorization tests remain future work
because there is no outgoing path or authorization implementation in this change.
