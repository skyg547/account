# Master Data AI Handoff

## Issue #923 / Draft PR #939 — 2026-10-20

- Branch/worktree/base: `agent/923-screening-eligibility`; `/tmp/account-923-screening-eligibility`; `origin/main@b2912554`. Only `master-data/**` was edited, including these three module-local records.
- PR: https://github.com/skyg547/account/pull/939, Draft, `Refs #923`; implementation commit `7b7410d6`. Issue remains open for human review.
- Delivered: separate operational-active and evidence-backed screening eligibility; dedicated reviewer role and separation from submitter; explicit as-of eligibility endpoint; append-only reviewer/evidence/time audit; V11 PostgreSQL migration and local H2 support; module documentation.
- Local verification: required core/API/Batch suite and both bootJars BUILD SUCCESSFUL (core 641, API 136 with 29 PostgreSQL-only skips, Batch 5; zero failures/errors). Fresh synthetic PostgreSQL V11 migrations and immutable audit test passed. Packaged H2 API startup and unscreened eligibility GET passed. Separate read-only reviewer found no blocking code issue and Q1–Q4 PASS; evidence table is in `worklog.md`.
- Incomplete acceptance: provider-side eligibility is tested, but consuming payable/receivable services and shared contracts were outside the allowlist, so agreed consumer contract tests and enforcement remain open. Business owner approval of wider jurisdiction/type and escalation policy is also pending. Keep this PR Draft and do not claim full Issue acceptance.
- Next gates: human policy/scope decision for consumers, consumer integration tests in a separately authorized change, current-head CI, and human review before Ready/merge or Issue closure. A reviewed forward schema correction and issue-scoped revert must preserve existing partner and screening audit rows.
- Authority: implementers wrote disjoint module files; independent reviewer was read-only; parent Integrator owns Git/GitHub and module records. Human controls Ready, merge, Issue close, and branch/worktree removal.

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
