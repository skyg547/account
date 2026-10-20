# Master Data Agent Status

## 2026-10-20 — Issue #919 / Draft PR #933

- State: Draft handoff for human review. Branch `agent/919-master-change-idempotency`; worktree `/tmp/account-919-master-change-idempotency`; base `origin/main@b29125549504d245aa10b3d275923e407bff8d56`; functional commit `ef93e53d`.
- Scope: only `master-data/**` change-request core code, migration, targeted core/API tests, feature documentation, and these three module-local AI harness records. Shared harness/history files, other modules, and production data were untouched.
- Result: atomic create-or-load makes identical concurrent `sourceReference` submissions share one result and rejects mismatched reuse. An already `APPLIED` request returns the stable prior result without a new SCD2 version or changed `appliedAt`.
- Verification: synthetic PostgreSQL 16 plus H2; requested core/API/Batch run passed 806/806 (620/181/5), failures/errors/skips 0. PostgreSQL rollback-only unique-violation and blocked concurrent insert cases passed. Diff/marker gates and independent read-only Q1–Q4 review passed.
- Draft CI: harness validation and merge-guard candidate tests passed; PR discipline failed only `DRAFT_NOT_MERGE_READY` and repository `TRUST_POLICY_UNCONFIGURED`. Current-head CI is not fully green or merge-ready.
- Draft PR: https://github.com/skyg547/account/pull/933 (`Refs #919`). Existing HTTP create DTO lacks `sourceReference`; legacy rejected rows fail closed; PostgreSQL lookup assumes `READ COMMITTED`. Current-head CI and human review control Ready/merge; Issue closure and resource removal remain human gates.

## 2026-10-20 — Issue #911 / Draft PR #927

- State: Draft handoff for human review. Branch `agent/911-bp-bulk-fail-closed`; worktree `/tmp/account-911-bp-bulk-fail-closed`; base `origin/main@a24ceb024602eb2c1915bf90ae8acb8ce242a4a0`.
- Scope: four core functional files and `master-data/docs/process-flow.md`; module-local harness records and Codex history. No other module, shared harness document, contract, repository query, schema, or production data change.
- Result: Monolith bulk reference lookup throws `DuplicateBusinessPartnerReferenceException` on a repeated active result code and returns no partial Map. Equal/unequal validFrom, repeated input, distinct codes, empty input, and H2 JPA result cardinality are covered.
- Verification: focused RED 2/5 duplicate cases failed before fix; focused GREEN 12/12 passed. Full requested core/API/Batch run passed 612 tests (523/84/5), zero failures/errors; 24 PostgreSQL-only API tests skipped. Diff and marker gates passed; separate read-only review found no findings and judged Q1–Q4 PASS.
- Draft PR: https://github.com/skyg547/account/pull/927 (`Refs #911`); functional commit `49a8263c`. Tax HTTP round trip and PostgreSQL validation remain untested in this scope. Human review and current-head CI control Ready/merge; Issue closure and resource removal remain separate gates.

## 2026-10-01 — Issue #754 / Draft PR #821

- State: `status:needs-review` handoff. Branch `agent/754-future-create-intervals`; worktree `/tmp/account-754-future-create-intervals`; base `origin/main@b06e7de3a2e26c1e14a15d73486c02fa7216e8b8`.
- Scope: `master-data/**` only. Four direct CREATE services, Department JPA adapter, BusinessPartner repository, targeted core/API/JPA tests, and module feature documentation. No shared harness file, other module, schema, or production data changed.
- Result: duplicate CREATE on any reused business key is rejected before a second save, including future and historical rows; direct and approved conflicts use HTTP 409. Approved CREATE history-count checks remain. New keys can be created; expired-only and future-only keys cannot currently be reactivated by CREATE or UPDATE.
- Verification: core 470 + API 72 + Batch 5 = 547 tests PASS, zero failures/errors/skips; `./gradlew :master-data:test` is NO-SOURCE. Independent read-only review found no blocking issue and judged Q1–Q4 PASS.
- Draft PR: https://github.com/skyg547/account/pull/821 (`Refs #754`); implementation commit `19b7ade5`.
- Remote CI on head `56598909` did not start any steps: Module Validation, Harness Validation, and Agent Merge Guard failed at GitHub billing/spending-limit admission. Local equivalents passed (harness schema, Python 22, Node PR 32, merge-guard 89); remote CI must rerun after the repository owner resolves billing.
- Residual gate: PostgreSQL and concurrent writer constraints remain in F07/#753, Draft PR #820. Human review handles any overlapping-file rebase, current-head CI, Ready/merge, and Issue closure.
