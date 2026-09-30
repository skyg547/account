# GH-753 worklog

## 2026-10-01 KST — intake and baseline

Created the isolated branch/worktree from fetched `origin/main@b06e7de3`. Preserved the dirty primary checkout. Issue claim: https://github.com/skyg547/account/issues/753#issuecomment-5918115498.

Before implementation, the requested `./gradlew :master-data:test --offline --no-daemon --console=plain --max-workers=2` returned success with `NO-SOURCE`: the parent project did not run child tests. Added a module-local aggregate dependency so this command will execute core/API/Batch tests.

Baseline explicit core/API/Batch suite: core444 + API68 + Batch5 = 517 tests, failures/errors/skips0, exit0. Existing H2 tests do not prove PostgreSQL concurrency. Added acceptance regressions before production changes and prepared a dedicated PostgreSQL16 container using an already-cached image, synthetic fixtures only.

Design: exact tuple lock row for absent/existing keys; transaction-owned key locks precede version recheck and mutation. Stable lock order for request operations. V8 portable lock/validity schema; V9 PostgreSQL all-history inclusive exclusion. H2 local target8 is explicit; production release SQL packaging continues to include both migrations. Existing bad data causes migration failure and requires reviewed remediation, without automatic repair.

## Regression and independent review checkpoints

- First real PostgreSQL RED:33 tests/24 failures/errors0/skips0, exit1. Every failure expected409 but received200; all4 types reproduced distinct-approved and direct/approved races and missing exclusion. No production fixes had been applied.
- First implementation targeted service check:370 tests PASS (3 suites),0 failures/errors/skips. PostgreSQL first GREEN:61 tests (49 concurrency+12 migrations),0 failures/errors/skips, exit0. These are checkpoints, not final-head evidence.
- Independent read-only reviewer identified P2: a DEACTIVATE ending today leaves the row active and history count unchanged; a stale approved open-ended UPDATE returned400. Narrow application interval conflict validation now distinguishes this409 from malformed input400.
- Reviewer also identified P1: an outer transaction can preload JPA entities before the key lock. New regression RED4/4 (expected400, actual200) reproduced a losing DEACTIVATE extending the winner's shortened end in all4 types. Added explicit fresh mutation-read ports/adapters so only the selected entity is refreshed after key lock; approved applier checks also use these fresh reads. Global clear is intentionally avoided.
- Related beginner documentation was corrected after Q3 review. Shared documents remain untouched.

Static deployment evidence: migration-runner/build.gradle:43 and70–76 already copy all module migration SQL to `db/contexts/master-data`. Runtime grant script `postgres/runtime/grant-runtime-privileges.sh:27–39` grants schema usage and SELECT/INSERT/UPDATE/DELETE on every public table except Flyway history, including the new lock table when run after migrations. This is source evidence; no production grants or release deployment were performed.

- Full core initially exposed one implicit H2 Flyway composition (`BusinessPartnerHistoricalActiveIntegrationTest`):454 tests/2 context failures because V9 was attempted in H2. Pinning this H2 fixture to target8 produced454/454 PASS, failures/errors/skips0. No domain assertions were relaxed.
- Expanded full API checkpoint:142 tests/2 failures; new same-request and applyDue/single-request races returned500 because the request repository attempted an optimistic-version-aware lock before refreshing stale cached state. Request mutation read now flushes pending local state and uses one `refresh(PESSIMISTIC_WRITE)` for row lock/current reload; a same-outer-transaction request→approve→apply regression guards preservation of the local decision. Final full verification follows this correction.

## Final module verification and integration boundary

Final forced command:

```bash
MASTER_DATA_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:17533/master_data_753 \
JAVA_HOME=/home/ho/.jdks/jdk-17.0.20.1+1 \
./gradlew :master-data:test :master-data:api:bootJar :master-data:batch:bootJar \
  --offline --no-daemon --console=plain --max-workers=2 --rerun-tasks
```

Result: exit0,3m12s,22tasks executed. Core454/API143/Batch5 = **602 tests**,28 suites, failures/errors/skips0. API includes **75 actual PostgreSQL tests** (63 concurrency+12 migration); these are included in602, not additive. Both bootJars contain the core V8/V9 migrations and PostgreSQL driver. The40 frozen source/build/test hashes match; whitespace, exact-scope conflict-marker and local Markdown-link checks pass. Independent read-only `/root/review_753` inspected the same XML and hashes.

The deployment-consumer check actually ran:

```bash
./gradlew :migration-runner:test \
  --tests '*MigrationExecutorH2Test.masterDataBaselineContainsEveryJpaOwnedTableAndConstraint' \
  --tests '*MigrationExecutorH2Test.masterDataPublishedHistoryUpgradesForwardFromV5ToV6' \
  --offline --no-daemon --console=plain --max-workers=2
```

Result: exit1,33s,2 tests/2 failures in existing H2 fixtures attempting PostgreSQL-only V9. This is a real integration regression, not a missing dependency or a passing test. Runner `processResources` copied V8/V9 byte-identically, confirming release delivery. The user forbids other-module edits, so a reviewed one-file H2 test-fixture correction is prepared separately and an explicit scope exception was requested before any such edit. Q1/integration remains held while this boundary is unresolved; module test success is not overall integration approval.
