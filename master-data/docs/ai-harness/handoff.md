# Master Data AI Handoff

## Issue #919 / Draft PR #933 — 2026-10-20

- Branch/worktree/base: `agent/919-master-change-idempotency`; `/tmp/account-919-master-change-idempotency`; `origin/main@b29125549504d245aa10b3d275923e407bff8d56`. Functional commit `ef93e53d`.
- PR: https://github.com/skyg547/account/pull/933, Draft, `Refs #919`; Issue remains open. The PR body records the command, quality review, limitations, and human merge authority.
- Changed scope: 17 implementation/test/module documentation files under `master-data/**` plus these three module-local records. No other module, shared harness/history document, or production data was changed. A separate reviewer was read-only.
- Acceptance: identical concurrent `sourceReference` commands share one persisted request/result; mismatched reuse yields typed conflict/409; repeated apply keeps the original `appliedAt` and SCD2 count. PostgreSQL regression covers blocked contenders and rollback-only unique violations without continuing a failed transaction.
- Verification: synthetic PostgreSQL 16 and H2; requested offline core/API/Batch command BUILD SUCCESSFUL with 620 + 181 + 5 = 806 tests, failures/errors/skips 0. Scoped whitespace and conflict-marker checks passed. Independent Q1–Q4 PASS evidence is in `worklog.md` and the PR.
- Draft CI: remote harness validation and merge-guard candidate tests passed. PR discipline is red only for `DRAFT_NOT_MERGE_READY` and `TRUST_POLICY_UNCONFIGURED`; the repository owner must configure the trusted review policy before that gate can pass after an authorized Ready transition. No Ready transition is authorized by this handoff.
- Limits and next gate: HTTP create DTO does not accept `sourceReference`; a future API contract change requires separate scope. Legacy `REJECTED` rows with lost original reason fail closed. PostgreSQL create-or-load assumes `READ COMMITTED`; production throughput was not measured. Current-head CI and human review precede Ready/merge.
- Rollback: reviewed issue-scoped revert; preserve request, master-history, and audit rows, and apply forward schema correction if needed. Parent Integrator owns publication and module records. Human review controls Ready, merge, Issue close, and branch/worktree deletion.

## Issue #914 / Draft PR #938 — 2026-10-20

- Issue/branch/worktree/base: `#914`; `agent/914-mask-change-responses`; `/tmp/account-914-mask-change-responses`; original `origin/main@ff5466f1`, integrated `origin/main@4d0273fe`.
- Changed files include `master-data/api` response DTO/Controller/authentication filter and two API tests, `master-data/docs/process-flow.md`, `local-run.md`, `beginner-guide.md`, `frontend/src/services/masterDataService.ts`, `frontend/src/app/master-data/partner/page.tsx`, and these three module-local records.
- Acceptance evidence: six endpoint response JSON bodies exclude synthetic registration text, including escaped strings; real JWT HTTP test shows unauthenticated 401 and authenticated 403 plus WARN audit event without payload; frontend approval UI supports masked summary responses.
- Rollback: reviewed revert of this issue's API/frontend/tests/module docs. No schema or production data changes; preserve existing business/audit records.

## Issue #920 / Draft PR #934 — 2026-10-20

- Branch/worktree/base: `agent/920-bound-foundation-lists`; `/tmp/account-920-bound-foundation-lists`; `origin/main@b29125549504d245aa10b3d275923e407bff8d56`.
- Draft PR: https://github.com/skyg547/account/pull/934 (`Refs #920`), implementation commit `12f27e608a074ffb26fe2f5c07e179d008349512`; current-head hosted CI has not been accepted as verification.
- Changed scope: master-data/auth API/core ports, adapters, tests, module docs and auth V75 indexes, plus three master-data module-local harness records. No shared harness, frontend, Gateway source, contracts or other module changes.
- Acceptance evidence: default 20/maximum 100, 400 on invalid bounds, stable SQL pages, effective-date filtering, 601 partner/2,400 account synthetic history with zero aggregate loads, PAT owner/admin tie-break tests, explicit consumer page-loop instructions. Existing frontend consumers still need migration and can show only the first 20; this is the remaining integration gate.
- Verification: five-project offline Gradle command passed with 1,069 passing tests, zero failures/errors and 27 PostgreSQL-only skips. Latest auth PAT JPA 2/2 and full auth core 199/199 passed after review fix. H2 is not PostgreSQL query-plan evidence. Independent master-data/auth reviewers judged Q1–Q4 PASS; their consumer finding remains open. `git diff --check` passed.
- Rollback: reviewed issue-scoped code/doc revert; if V75 has run, use a reviewed forward index correction rather than erasing data. Preserve historical and audit rows.
- Authority: module writers implemented, separate reviewers remained read-only, and parent Integrator owns records, commit, push and Draft PR. Human review and frontend migration decision precede Ready/merge; Issue close and branch/worktree removal are separate gates.

## Issue #915 / Draft PR #931 — 2026-10-20

- Branch/worktree/base: `agent/915-master-data-http-errors`; `/tmp/account-915-master-data-http-errors`; `origin/main@b29125549504d245aa10b3d275923e407bff8d56`. PR https://github.com/skyg547/account/pull/931 is Draft with `Refs #915`; Issue remains open.
- Implementation: `MasterDataExceptionHandler` maps expected MVC, missing-record and state errors without leaking raw input/internal details; #913's trusted field-rule messages remain available. Two HTTP test files cover current status contracts with real advice and filter; `docs/process-flow.md` explains the mapping and test command. Only `master-data/**` code, module document, and module-local records changed. Shared kernel, common harness/history, schema, and production data were untouched.
- Verification: prior RED 400-for-404 and 500-for-409; final required core/API/batch run passed 613/105/5 tests (723 total), zero failures/errors, with 27 PostgreSQL-only API tests skipped. Targeted compatibility run passed 23/23; whitespace/marker gates passed. Independent reviewer was read-only, found no findings, and rated Q1–Q4 PASS; detailed evidence is in the module worklog and PR body.
- Residual: full-context tests mock the core exceptions, and idempotency/PostgreSQL SCD2 409 paths have separate rather than full-context evidence. The 404/409 adapter checks current core message prefixes, so core wording changes require paired updates. H2 is not PostgreSQL evidence.
- Remote gate: Harness Validation passed. Agent Merge Guard's implementer-discipline job failed with `TRUST_POLICY_UNCONFIGURED` plus implementation-owner, verification, merge-authority, and Draft-readiness requirements because the workflow explicitly supplies `policy: undefined`. The repository policy owner must configure/review this trust gate before Ready/merge; it cannot be changed inside the module-only allowlist. Keep the PR Draft.
- Rollback: reviewed issue-scoped revert of code, tests, and module feature document; preserve existing business/audit records. Parent Integrator owns Git/PR and these records. Human reviewer controls Ready, merge, Issue close, and branch/worktree removal.

## Issue #911 / Draft PR #927 — 2026-10-20

- Branch/worktree/base: `agent/911-bp-bulk-fail-closed`; `/tmp/account-911-bp-bulk-fail-closed`; `origin/main@a24ceb024602eb2c1915bf90ae8acb8ce242a4a0`.
- PR: https://github.com/skyg547/account/pull/927, Draft, `Refs #911`; functional commit `49a8263c`. Issue remains open for human review.
- Changed scope: five functional files under `master-data/**`, these three module-local harness records, and `docs/history/CODEX_WORKLOG.md` as the required Codex history. Other modules, shared harness documents, shared contracts, repository SQL, migration, and production data are untouched.
- Acceptance: same-code active versions fail with a typed exception and no partial Map; equal/unequal start dates, repeated requested codes, distinct keys, empty input, and H2 query/cardinality are covered. Existing Tax error propagation was inspected; its actual HTTP round trip was not exercised.
- Verification: focused RED 2/5 as expected, then GREEN 12/12. Requested full suite BUILD SUCCESSFUL: core 523, API 84, Batch 5 passed; API 24 PostgreSQL-only tests skipped, failures/errors 0. Scoped whitespace/marker gates passed. Independent read-only reviewer found no remaining findings; Q1–Q4 PASS evidence is in `worklog.md` and PR body.
- Remaining gate: current-head remote CI and human review before Ready/merge. H2 does not prove PostgreSQL V9 behavior; run synthetic PostgreSQL concurrency/migration tests before claiming that guarantee. Tax HTTP integration was not run under this module-only allowlist.
- Rollback: reviewed revert of this issue's functional code, tests, and module document; preserve historical/business data. Implementer and reviewer were separate; parent Integrator owns publication and records. Ready, merge, Issue close, and branch/worktree removal require separate human authority.

## Issue #754 / Draft PR #821 — 2026-10-01

- Branch/worktree/base: `agent/754-future-create-intervals`; `/tmp/account-754-future-create-intervals`; `origin/main@b06e7de3a2e26c1e14a15d73486c02fa7216e8b8`.
- PR: https://github.com/skyg547/account/pull/821, Draft, `Refs #754`. Issue is OPEN and handed to human review. Implementation commit: `19b7ade5`.
- Changed scope: only `master-data/**` — four CREATE services, exception description, Department adapter, BusinessPartner repository, regression tests and module docs. The three files in this directory are module-local AI harness records; shared harness/history files remain untouched.
- Acceptance: sequential future, historical overlap, inclusive touching, adjacent and gapped disjoint duplicate CREATEs reject the second and preserve the first for all four types. Approved CREATE retains its history-count guard. Direct HTTP duplicates return 409. The policy forbids CREATE reactivation; UPDATE currently requires an active version.
- Verification: issue core/API/Batch command passed 547/547 tests (470/72/5), failures/errors/skips 0. The exact `./gradlew :master-data:test` command succeeded but was NO-SOURCE. Focused RED/GREEN evidence and Q1–Q4 PASS table are in `worklog.md`. Independent read-only reviewer cleared the final code diff; staged whitespace and conflict-marker gates passed.
- Remote CI: checks on `56598909` were rejected before steps by GitHub billing/spending-limit admission; Module Validation, Harness Validation, and Agent Merge Guard did not execute. Local harness schema, Python 22/22, Node PR 32/32, and merge-guard 89/89 passed, but do not replace remote current-head CI. Repository owner must fix billing and rerun before Ready/merge.
- Remaining risk: PostgreSQL/concurrent writer safety is not established here. F07/#753 Draft PR #820 supplies the separate key-lock/exclusion work and overlaps files; review the merge order and recheck tests after any integration. Current-head CI and human policy review remain.
- Rollback: reviewed issue-scoped revert of code/tests/module docs; no migration or production data change. Keep the first accepted master row and all historical/audit records.
- Authority: writer and independent reviewer were separate; reviewer changed no code. Parent Integrator owns Git/GitHub and these records. Human review is the gate for Ready, merge, Issue close, branch/worktree removal. No such final actions are part of this handoff.
