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

Legacy duplicates/NULL references require separately approved financial reconciliation before V13. Constraint creation needs a deployment window appropriate for table size. Production load, process crash injection, concurrent closing and different journals updating shared balances were not verified. No deployment or financial data repair was performed.

Main worktree: `/tmp/account-760-single-posting`; regression-proof worktree: `/tmp/account-760-regression-proof`. Evidence logs and saved PostgreSQL XML remain under `/tmp/account-760-*`. Task-only PostgreSQL container `account-760-postgres` is stopped and retained. Branches/worktrees are retained for human review; shared harness history remains unchanged under the user's allowlist.
