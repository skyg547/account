# GH-757 handoff

Issue #757 regression proof, required module verification and independent review are complete. [Draft PR #793](https://github.com/skyg547/account/pull/793) is open with `Refs #757`; current `main` already contains the production remediation from merged PR #792, and this branch supplies the missing explicit audited RED/current GREEN evidence without duplicating behavior.

- Issue/branch/worktree: [#757](https://github.com/skyg547/account/issues/757); `agent/757-trusted-journal-audit-actors`; `/tmp/account-757-trusted-journal-audit-actors`; base `894e95e4`.
- Scope: `journal-ledger/**` only, including these module-local records. Other modules, shared contracts and repository-level shared harness/history files are unchanged.
- Behavior: forged, blank and conflicting request-body `createdBy`/`auditUser` cannot outrank the trusted caller at either event or posting-contract MVC boundary. Payload actors are discarded and are not approval identities; Kafka/Spring-event service principals and maker-checker controls remain unchanged.
- RED/GREEN: byte-identical regression SHA `d6574349…` fails all6 cases on exact audited `a97d10ab` production and passes all6 on current production.
- Verification: literal `./gradlew :journal-ledger:test` passes250 tests (core188/API51/batch11), failures/errors/skips0; independent related-path selection35/35 and static gates pass.
- Independent review: `/root/issue757_review` remained read-only, found no P0-P3 and confirmed Q1/Q2/Q4 PASS plus Q3 N/A because current functional documentation already matches behavior.
- Rollback: remove/revert the regression and this Issue's records. No production code, schema, financial amount or data changes exist.
- Limits: no live Gateway/JWT, deployment, Kafka broker, PostgreSQL, production data or load test. MockMvc treats `X-Auth-*` as already trusted Gateway context.
- Remote CI prerequisite: three GitHub Actions entry jobs on publication head `b8f02fd0` did not execute any steps; annotations cite failed recent account payments or a spending limit needing increase. The account owner must resolve Billing & plans and rerun checks. This is neither a remote test pass nor an executed code-test failure.
- Authority separation: the module writer changed only the regression; the reviewer made no edits; the parent Integrator owns records, commit, push and Draft PR. Ready, merge, Issue close, deployment and cleanup remain later human gates.
- Publication: reviewed commit `3c1c2069` is pushed; Draft PR #793 records verification and authority separation. Issue #757 is handed off for `status:needs-review`.
- Next owner: a human reviewer owns Ready, merge, Issue close, deployment and cleanup; the parent retains the branch/worktree/archive for review.

Details, exact commands and Q1-Q4 evidence are in [worklog.md](worklog.md).

---

The following is retained historical handoff and is not a current GH-757 report.

# GH-770 handoff

Issue #770 implementation, audited behavioral RED, H2/PostgreSQL verification and independent review are complete. [Draft PR #786](https://github.com/skyg547/account/pull/786) is open with `Refs #770`; reviewed implementation/record commit `86d2a39e`.

- Issue/branch/worktrees: [#770](https://github.com/skyg547/account/issues/770); `agent/770-unsettled-settlement-lock`; `/tmp/account-770-unsettled-settlement-lock`; base `c0b4f204`; detached proof `/tmp/account-770-regression-proof@a97d10ab`.
- Scope: `journal-ledger/**` only, including these module-local records. Other modules, shared contracts and repository-level shared harness/history files are unchanged and not claimed synchronized.
- Behavior: settlement locks the existing parent ID row through commit/rollback, refreshes stale parent/reference state, and then applies unchanged precision, durable replay and state-transition rules. Concurrent40+50 yields90/10 with both refs; concurrent/cleared known-ref replay is a no-op; a waiting over-settlement validates the latest remaining balance.
- RED: byte-identical fixture SHA `48d4dbb…` on unchanged audited production fails3/5 as expected: amount loss, same-ref unique collision and stale near-balance acceptance.
- Verification: fixed H25/5 and disposable PostgreSQL5/5 PASS; PostgreSQL confirms independent backend PIDs and real lock wait. Requested exact module command succeeds; forced writer/parent and independent reviewer runs each pass229 tests (core178/API40/batch11), failures/errors/skips0. Quality/static gates pass.
- Independent review: `/root/issue770_review` remained read-only, reports no P0-P3, and marks Q1-Q4 PASS.
- Rollback: reviewed scoped revert only; no migration/data rollback. Do not continue concurrent settlement traffic on the reverted version because the lost-update race would return. Historical divergence requires separate reconciliation.
- Limits: hot-item lock-wait/load, explicit deadlock/serialization/timeout injection, distributed retry, direct SQL/bypass writers, production data and deployment are not covered.
- Remote CI prerequisite: Module Validation, Harness Validation and Agent Merge Guard did not start; annotations cite failed recent account payments or a spending limit requiring increase, and failed jobs have no executed steps. The account owner must resolve billing and rerun checks; this is neither a remote test pass nor an executed code-test failure.
- Test resource: task-owned `account-770-postgres` is stopped and retained for review evidence; it is not an operational database.
- Authority separation: Planner/Reviewer read-only; Service/SQL/Test/Documentation writers used disjoint allowlists; parent owns records, commit, push and Draft PR. Ready, merge, Issue close, deployment and cleanup remain later human gates.
- Next owner: repository/human reviewer handles remote CI and later readiness/merge/closure/deployment gates. Branch, worktrees and stopped test container remain retained.

Details, commands and Q1-Q4 evidence are in [worklog.md](worklog.md).

---

The following is retained historical handoff and is not a current GH-770 report.

# GH-767 handoff

Issue #767 implementation, three RED proofs, requested local verification and independent review are complete. [Draft PR #785](https://github.com/skyg547/account/pull/785) is open with `Refs #767`; implementation commit `06e5ccc3`.

- Issue/branch/worktree: [#767](https://github.com/skyg547/account/issues/767); `agent/767-reaggregation-consistency`; `/tmp/account-767-reaggregation-consistency`; base `795c494a`.
- Scope: `journal-ledger/**` only, including these module-local records. Other modules, shared contracts and repository-level shared harness files are unchanged.
- Behavior: V15 persists a fail-closed JobInstance owner barrier. Start freezes the range and closes publication; owner cleanup and checkpointed 100-detail chunks rebuild; exact GL/SL reconciliation alone reopens publication. Posting and reads participate in stripe/epoch fencing.
- Restart: failure never auto-releases. Use the same identifying JobParameters/JobInstance so completed cleanup and chunks are not replayed. Different instances are rejected while an owner is active.
- Verification: audited migration and cleanup-exposure regressions fail as expected. Review-found API-local HTTP500 also fails before correction. Final writer and independent forced runs each pass224 tests (core173/API40/batch11), failures/errors/skips0; API GET smoke is HTTP200 `[]`, Batch local lifecycle/boot and static/quality gates pass.
- Independent review: `/root/review_767` remained read-only. Its initial P2 missing local V15 schema was corrected by the writer; final review reports no P0-P3 and Q1-Q4 PASS.
- Rollback/deployment: stop every writer/reader, preserve applied V14/V15 and singleton, revert application under review, and restore one barrier-aware version before traffic. Validate runtime SELECT/UPDATE rights, lock waits and cardinality on approved PostgreSQL before deployment.
- Limits: no live PostgreSQL, production data, distributed process kill or load test. Direct SQL/old binaries bypass the barrier; privileged DB readers can see in-place partial rows even though application consumers reject them.
- Remote CI prerequisite: Module Validation, Harness Validation and Agent Merge Guard did not start; annotations cite failed recent account payments or a spending limit needing increase. The account owner must resolve billing and rerun checks; this is not a remote test pass or code-test failure.
- Authority separation: module writer implemented; explorer/reviewer were read-only; parent owns records and Git/GitHub. User authorized push and Draft PR only. Ready, merge, Issue close, deployment and cleanup remain later human gates.
- Next owner: human reviewer owns subsequent Ready/merge/Issue-close and deployment gates. The branch and worktree remain retained.

Details and Q1-Q4 evidence are in [worklog.md](worklog.md).

---

The following is retained historical handoff and is not a current GH-767 report.

# GH-764 handoff

Issue #764 implementation, audited regression proof, requested local verification and independent review are complete. [Draft PR #784](https://github.com/skyg547/account/pull/784) is OPEN/MERGEABLE with `Refs #764`; implementation commit `6d96ba0d`.

- Issue/branch/worktree: [#764](https://github.com/skyg547/account/issues/764); `agent/764-preserve-event-decimals`; `/tmp/account-issue-764`; base `bd2707af`.
- Scope: `journal-ledger/**` only, including these module-local records. Other modules, shared contracts and repository-level shared harness files are unchanged.
- Behavior: untyped HTTP and Kafka JSON decimals become exact `BigDecimal` values before rule interpolation. `900719925474099.11` is preserved; `100.000000000000001` is rejected by unchanged `AccountingPrecision` before rounding. JSON integers stay integral.
- Verification: audited source `a97d10ab` fails both byte-identical MVC regressions in the expected directions; fixed focused5 PASS; exact module208 PASS; API bootJar and static gates PASS. Details are in [worklog.md](worklog.md).
- Independent review: `/root/review_764` is read-only. After correction of the production factory-wiring assertion and DLQ overclaim, no P0–P3 remains and Q1–Q4 are PASS.
- Rollback: reviewed scoped revert of API deserialization configuration, tests and docs. No schema/data rollback is needed.
- Limits: no live Kafka broker, deployed HTTP server, production data/PostgreSQL or load test. Retry count/DLQ delivery is deployment configuration, not guaranteed by this change.
- Authority separation: Controller writer changed only inbound production configuration; Test writer changed only regressions; parent owns module docs/records and Git/GitHub; Reviewer did not edit. User authorized push and Draft PR only. Ready, merge, Issue close, deployment and resource cleanup remain later human gates.
- Remote CI jobs did not start: annotations for Module Validation, Harness Validation and Agent Merge Guard report failed recent account payments or a spending limit requiring increase. This is an external execution prerequisite, not an executed test failure or pass.
- Next owner: repository/account owner resolves the billing prerequisite and reruns checks; human reviewer handles later readiness and merge gates. Both task/proof branches and worktrees stay retained.

---

The following is the retained historical GH-761 handoff; its remote state is not a current status report.

# GH-761 handoff

Implementation, requested local verification and independent review are complete. [Draft PR #783](https://github.com/skyg547/account/pull/783) is open with `Refs #761`; implementation commit `b2af5823`.

- Issue: [#761](https://github.com/skyg547/account/issues/761).
- Branch/worktree: `agent/761-concurrent-balances`, `/tmp/account-761-concurrent-balances`.
- Scope: `journal-ledger/**` only, including these module-local harness records. Shared harness and other modules are unchanged.
- Behavior: JPA/JDBC hold sorted account/currency transaction locks before GL/SL reads, including missing and nullable keys. Refresh/detach prevents stale persistence-context overwrites; V14 seeds256 locks. Domain precision, posting identity and closing checks remain intact.
- Verification: requested module203 PASS; new real PostgreSQL26 PASS; initial real PostgreSQL posting9 and V14 upgrade1 PASS; audited16 expected failures. Exact commands/artifacts are in [worklog.md](worklog.md).
- Independent review: `/root/balance_review`, no remaining P0–P3, Q1–Q4 PASS. Independently executed Loan consumer1 PASS and both bootJARs contain byte-identical V14.
- Deployment/rollback: quiesce every writer, including embedded journal core users, for V14 and coordinated binary cutover. Never mix pre-lock/new writers. Retain applied migrations on rollback and keep traffic stopped until protected code is restored.
- Limits:256-stripe contention, actual READ_COMMITTED requirement, whole-transaction caller retry after deadlock, and posting shutdown throughout chunk-based Batch rebuild. Production load/crash/deployment and historical data reconciliation were not performed.
- Authority separation: writers implement; `/root/balance_review` independently reviews read-only; parent Integrator alone owns records and Git/GitHub. User authorized Draft PR/push with `Refs #761`; Ready, merge, Issue close, deployment and resource deletion remain later gates.
- Next owner: repository/account owner resolves the GitHub Billing & plans prerequisite and reruns CI; human reviewer owns subsequent Ready/merge/Issue-close and deployment gates. Issue remains OPEN/needs-review.

Remote CI jobs did not start: GitHub annotations report failed recent account payments or a spending limit needing increase. Local verification passed; remote CI is unverified. No billing settings or workflows were changed.

Task-owned disposable PostgreSQL container `account-761-postgres`, regression worktree `/tmp/account-761-regression-proof` and external `/tmp/account-761-*` evidence are retained for handoff; the test-only container is stopped after verification. No production DB or credentials were used.

---

The following is the retained historical GH-760 handoff; its remote state is not a current status report.

# GH-760 handoff

Implementation, verification and independent substantive review are complete. Follow [agent-status.md](agent-status.md) and [worklog.md](worklog.md) for the full evidence.

- Issue: [#760](https://github.com/skyg547/account/issues/760)
- Branch/worktree: `agent/760-single-posting`, `/tmp/account-760-single-posting`
- PR: [Draft #782](https://github.com/skyg547/account/pull/782), `Refs #760`; implementation commit `65a799f4`.
- Allowed files: `journal-ledger/**` only. Shared harness and other modules must remain unchanged.
- Rollback: reviewed scoped code revert before release. Applied migration files are immutable; retain duplicate-posting constraints and use a reviewed forward migration for schema correction. Existing duplicate rows must be investigated, never silently deleted or merged.
- Next owner: parent Integrator for verification and Draft PR; human reviewer for subsequent Ready/merge/Issue-close decisions.
- Authority separation: module writer implements, independent reviewer stays read-only, parent owns Git/GitHub and module-local records. Draft PR authorization does not authorize Ready, merge, Issue close, deployment, or resource deletion.

## Change and verification

Posting holds a database header lock through ledger/balance commit, refreshes stale managed state, and retains approval/precision/closing controls. V13 enforces unique non-null source-detail identities in GL and SL. Completed retries retain the existing domain conflict and HTTP400 behavior.

Changed areas: core persistence adapter/repository and port documentation; GL/SL mappings and V13; concurrency/rollback/upgrade tests; API/Batch schema assertions; module test aggregation; README and functional documentation. All23 changed files are within this module.

- Exact `./gradlew :journal-ledger:test`:170 passed (core131/API34/batch5).
- PostgreSQL16.13 targeted regressions:14 passed; audited source fails both concurrent cases with4 GL rows instead of2.
- Independent Loan compatibility:1 passed; API/Batch bootJARs contain exact V13 resource.
- Successful suites: failures/errors/skips0. Independent `/root/posting_review`: no P0–P3, Q1–Q4 PASS.
- Detailed behavior, migration preflight, retry and rollback: [posting-concurrency.md](../posting-concurrency.md).

## Remaining gates and resources

Remote CI did not execute: GitHub check annotations report failed account payments or a spending limit needing increase. The account owner must check GitHub Billing & plans, then rerun the checks. Local tests do not replace this remaining remote CI gate; PR stays Draft. See the exact run evidence in the worklog.

Legacy duplicates/NULL references require separately approved financial reconciliation before V13. Constraint creation needs a deployment window appropriate for table size. Production load, process crash injection, concurrent closing and different journals updating shared balances were not verified. No deployment or financial data repair was performed.

Main worktree: `/tmp/account-760-single-posting`; regression-proof worktree: `/tmp/account-760-regression-proof`. Evidence logs and saved PostgreSQL XML remain under `/tmp/account-760-*`. Task-only PostgreSQL container `account-760-postgres` is stopped and retained. Branches/worktrees are retained for human review; shared harness history remains unchanged under the user's allowlist.

---

# GH-756 reviewed handoff

Issue #756 is implemented and independently reviewed on `agent/756-maker-checker-approval` in
`/tmp/account-756-maker-checker-approval`, based on `origin/main@054cdf13`. This checkpoint is not yet
published.

- Core owns canonical identity and lifecycle invariants: every entry has a maker, only that maker submits
  DRAFT, only a different checker approves REQUESTED, and posting preserves `approvedBy` separately from
  the final `auditUser`.
- API owns trusted header parsing and command roles. Missing actor returns401, missing/insufficient role403,
  and invalid state/self-approval400 without widening domain or persistence responsibilities.
- HTTP event payload identities are overwritten; Kafka and Spring contract events use separate fixed
  listener makers; machine auto-post requires a checker service principal distinct from that listener
  maker. V16 backfills only nonblank, canonically distinct legacy maker/checker evidence; self-approved
  and makerless rows remain unpostable.
- Verification: focused28, corrective focused3 and exact module244 PASS with failures/errors/skips0;
  parent exact rerun and independent forced rerun PASS; API/Batch bootJar packaging and static
  scope/whitespace/conflict-marker gates PASS.
- Audited proof: `/tmp/account-756-regression-proof` at exact `a97d10ab` runs the byte-identical
  `JournalMakerCheckerAuditedRegressionTest` (SHA256 `f5611f…`) with1 expected failure and no errors/skips,
  proving the audited code allowed maker approval of its own DRAFT.
- Independent reviewer `/root/issue756_review` returned the original V16/event trust gaps to the writer,
  verified their correction, found no remaining P0–P3 and marked Q1–Q4 PASS.
- Rollback: reviewed scoped revert while retaining applied V16, or later forward migration. No financial
  amount, ledger balance or production data was changed.
- Deployment gate: out-of-scope direct HTTP adapters exist in Closing, Deposit, Expenditure Resolution,
  Loan, Payable, Receivable and Reconciliation. Coordinate trusted maker headers/roles for all seven and
  request-approval/distinct service checker updates for approval-capable callers before deployment;
  fail-closed behavior is intentional.
- Next owner: parent Integrator for commit/push/Draft PR and remote check inspection. Human reviewer owns
  consumer follow-up, Ready/merge/Issue close/deployment/cleanup gates.

## Draft publication handoff

- Reviewed implementation commit `a6ecb1d69be232d9937624e8b26244d0537fd8a9` is published on
  `origin/agent/756-maker-checker-approval`; [Draft PR #792](https://github.com/skyg547/account/pull/792)
  is OPEN/DRAFT/MERGEABLE against `main` with `Refs #756`.
- Issue #756 remains OPEN and is synchronized to `status:needs-review`. The PR body records module244,
  audited RED, Q1–Q4, rollback, seven-consumer deployment block and writer/reviewer/Integrator authority
  separation.
- The three GitHub Actions entry jobs did not start. Their annotations report failed recent account
  payments or a spending-limit prerequisite, so no remote CI PASS or code-test failure is claimed. The
  account owner must resolve Billing & plans and rerun checks.
- Next owner is a human reviewer for the Draft diff and consumer compatibility plan. Ready, merge, Issue
  close, deployment, branch/worktree deletion and audited-proof cleanup remain separate approvals.
