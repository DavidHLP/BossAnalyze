# Independent review: AI acceptance / offline BOSS sampler

Reviewed commit: `d8945cda41fa4a1e29084a1234becea750eb9fa9`
Branch: `integration/ai-acceptance-20261121`
Diff range: `ad87f29..d8945cd` (6 commits, 3 merged task branches)

Method: static inspection of the diff and owning files; human-mode output comparison of
base (`ad87f29`) vs HEAD on synthetic pass/fail/unverifiable/invalid inputs; Python tests
run with `uv run --frozen python -m pytest tests/test_evaluate.py` (68 passed); targeted
Java run `mvn -o -f backend/pom.xml -pl crawler -am test -B -Dtest=RecruitmentSamplerTest`
(24 passed). Release evidence checked in the sibling `ai-acceptance/release` worktree at the
same HEAD.

## Verdict by review question

1. **Does AI mode enforce every metric gate human mode enforces?**
   PASS. `metrics()` and the `checks` list are shared and mode-independent
   (`ai-service/app/evaluate.py:119-204`, `:236-250`). AI mode changes only label
   provenance acceptance (`:210-217`) and the synthetic-sample eligibility clause
   (`:251-253`). Leakage, prediction coverage, core precision, cost/latency completeness,
   subgroup `--min-samples`, candidate recall, evidence/unknown consistency and ranking
   agreement remain identical. `qualityApproval=ai_accepted` is set only after
   `status == "pass"` (`:262-263`).

2. **Is human mode byte-identical to the base commit for JSON and exit codes?**
   PASS. The only human-path changes are the defaulted `--acceptance human` argument and
   the AI-guarded branches. `review_statuses={"human":"reviewed"}` with
   `review_statuses.get(...)` is equivalent to the old `reviewStatus != "reviewed"` plus
   `annotationOrigin != "human"` because model validation forces strings. The result dict
   is unchanged unless `acceptance == "ai"` (`:255-264`), and the exception path is
   unchanged for human mode (`:280-285`). Empirically, base and HEAD produced identical
   stdout and exit codes for pass (0), fail (1), unverifiable (2) and invalid telemetry (2).

3. **Can AI mode accept labels that should be rejected?**
   PASS. AI labels marked `reviewed`/`pending`, human labels marked `ai_reviewed`, blank
   reviewers, and unknown origins are all rejected by the origin→status map
   (`ai-service/app/evaluate.py:210-217`); tests cover these cases
   (`ai-service/tests/test_evaluate.py:194-201`). Reviewer identity, authorization and
   independence of the AI review cannot be certified offline; the spec and
   `QUALITY-PRIVACY.md` say so explicitly. No code path claims otherwise.

4. **Do the new tests assert behavior?**
   PARTIAL. The key AI tests assert exact CLI output, `acceptance`, `labelSource`,
   `qualityApproval`, pass/fail thresholds and failure precedence, so they would fail if the
   feature regressed. A few assertions are weak (see P3 findings 3 and 6). The Python suite
   currently passes (68 tests).

5. **Do sampler tests prove the accepted lifecycle offline?**
   PASS. They cover list-card → detail JSON → whitelisted export, SSR-only export with no
   JSON request, verification/auth stops with exit 2 and diagnostic-only output, identity and
   redirect rejection, no-overwrite, cancellation/window-close, and cleanup of owned
   page/browser/driver (with attached browser left open). They use Mockito Playwright mocks
   and synthetic fixtures only. `docs/operations/boss-sampling-acceptance-20261108.md`
   explicitly states that real BOSS list/detail/export, login-state CLI collection and real
   benefits fields are not verified. I re-ran the 24 sampler tests; all passed.

6. **Does the release doc overclaim?**
   PARTIAL. Reproducible claims were checked and are consistent: Maven 3.9.16 and Azul JDK
   17.0.20.1 (`toolchain.log`), JAR SHA-256 `be58...63e0` matching `crawler.sha256`, 87
   crawler and 45 nested-commons classes byte-identical, `ResumePrivacyTest` 6/6, preflight
   11/11 and the recorded refused JSON, frontend identity log, and `javap` 401/503 evidence.
   The doc honestly defers live 8081 503 verification and says no live BOSS/model/database
   actions were taken. One recorded count is stale/wrong relative to HEAD (P2 finding 1), and
   the evaluator test run is not recorded (P3 finding 2).

7. **Security/privacy.**
   PASS. `evaluate.py` still only reads local files and prints a summary; AI mode adds no
   provider, network, credential, database or runtime-approval path. Sampler tests mock
   Playwright and use synthetic cookies/fixtures; no real resume, BOSS session or model call
   is introduced. No source change touches `ResumePrivacy`/`AuthorizedResume`; the 401/503
   behavior is covered only by the existing mock-MVC test, and the release doc marks the live
   503 check as unverified.

## Findings

- `docs/operations/ai-acceptance-release-20261121.md:10` **P2**: The table records
  `crawler 55 项` for the listed Maven test command, but the same HEAD's integrated run
  records **60** crawler tests (`backend/target/release-evidence/test-integrated.log`:
  `RecruitmentSamplerTest` 24, total 60). The 55-test run (`test.log`) predates the sampler
  test commit `bdeeedc` and ran only 19 sampler tests. The doc therefore presents pre-change
  evidence as verification of the committed candidate. **Fix:** record 60 and identify the
  exact tree/SHA; if the evidence must describe the final tree, rerun the command on
  `d8945cd` and replace the log.

- `docs/operations/ai-acceptance-release-20261121.md:7-12` **P3**: No command or result is
  recorded for the `ai-service` evaluator, although `--acceptance ai` is the feature under
  release. **Fix:** add `cd ai-service && uv run --frozen python -m pytest
  tests/test_evaluate.py` and its result (68 passed at this revision) to the evidence table.

- `ai-service/tests/test_evaluate.py:108-116` **P3**: Each case asserts only
  `pytest.raises(ValueError)`. For `unknown={}` the rejection actually comes from the
  provenance guard (`not g.unknown`) at `evaluate.py:215`, not from the intended
  sample-set/unknown-key check, so the test can pass while the metric-side behavior
  regresses. **Fix:** match the specific message (`unreviewed or missing annotations` vs
  `incompatible predictions` / `prediction sample mismatch`) or split the cases.

- `backend/crawler/src/test/java/com/david/hlp/crawler/sample/RecruitmentSamplerTest.java:656-658`
  **P3**: For the `empty` and `wrong` boundaries the test asserts only exit 2, no
  `"status":"ok"` and empty labels. It does not assert the expected diagnostic status
  (`missing_detail` / `identity_mismatch`) or `detailComplete=false`, so a regression that
  wrote a diagnostic with the wrong status would still pass. **Fix:** assert the expected
  status for these two boundaries (for example by calling `assertDiagnosticOnly` with the
  expected value in a split loop).

- `ai-service/QUALITY-PRIVACY.md:53` **P3**: The summary sentence now omits synthetic sets
  from the unverifiable cases. Human mode still rejects synthetic samples (`evaluate.py:252`)
  and line 43 says so, but the general summary is now ambiguous. **Fix:** restore the
  qualification, e.g. "人工模式下的 synthetic 集、缺样本或无排序/阳性等不可判断情形为
  unverifiable".

- `ai-service/tests/test_evaluate.py:138,389` **P3**: Human-mode report-shape regression is
  not locked for pass/fail output. The exact-JSON assertion covers only the rejected
  AI-origin case; `test_cli_complete_old_failure_and_invalid_telemetry` checks only
  `qualityApproval`, so an accidental `acceptance`/`labelSource` key in a human-mode pass
  report would not be caught. **Fix:** add an exact-key (or exact-JSON) assertion for a
  human pass and a human fail report.

No P1 findings. No finding identified in this review blocks merging; the P2 release-document
correction should be made before release acceptance evidence is treated as final.

## What this review does not cover

- No live BOSS access, browser automation, login-state CLI run, captcha/verification handling,
  or real list/detail/export verification. Only mocked offline JUnit behavior was reviewed.
- No real human gold, no provider/model calls, no billing or latency measurements, and no
  claim that synthetic test telemetry represents production quality.
- No live 8081 check of the 401/503 boundary; the release doc itself defers this. The review
  did not exercise `AIRequirementsController`, `AuthorizedResume` or `ResumePrivacy` at
  runtime, nor re-run the full 60-test crawler suite (only the 24 sampler tests).
- No independent reproduction of the candidate JAR build from source. The existing artifact
  hash, class-identity log, toolchain log and `javap` outputs were checked, but the "isolated
  network" claim and the original build from a clean tree were not reproduced.
- No verification of GitHub issue/PR links (#46/#54/#72), no signed-commit verification
  beyond the presence of the required `Signed-off-by` trailer, and no frontend runtime test.
- The unchanged crawler main-source behavior was reviewed only as far as needed to judge the
  new tests and docs; this is not a full audit of the sampler implementation.

## Post-review fixes (commander)

Fixed in the commit that adds this file, on `integration/ai-acceptance-20261121`:

- P2: release evidence now records the integrated run (crawler 60, ai-service 265) keyed to `d8945cda41fa4a1e29084a1234becea750eb9fa9`; the earlier 55-test run is marked pre-sampler.
- P3: the four sample-set cases now assert their specific evaluator errors.
- P3: `RecruitmentSamplerTest.noJsonNavigation...` now asserts `missing_detail` / `identity_mismatch` and `detailComplete=false` for the empty/wrong boundaries.
- P3: `QUALITY-PRIVACY.md` summary again qualifies synthetic sets as unverifiable in human mode.
- P3: added an exact-shape CLI test for human-mode pass and fail reports (no `acceptance`/`labelSource` keys).

Re-run after fixes: `pytest -q` 265 passed; `RecruitmentSamplerTest` 24 passed.
