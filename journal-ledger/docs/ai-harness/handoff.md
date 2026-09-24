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
