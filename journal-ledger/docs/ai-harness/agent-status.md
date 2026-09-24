# GH-767 agent status

- Issue: [#767](https://github.com/skyg547/account/issues/767); remote handoff target `status:needs-review`, owner `agent:codex`.
- Status: implementation, audited RED proof, requested local verification and independent review complete; [Draft PR #785](https://github.com/skyg547/account/pull/785) is open with `Refs #767`.
- Base: `origin/main@795c494a950980864c0e9bc63464dcb505a8d46b`.
- Branch/worktree: `agent/767-reaggregation-consistency`, `/tmp/account-767-reaggregation-consistency`.
- Model assignment: `gpt-5.6-sol`, high. Parent owns Git/GitHub and these module-local records.
- Ownership: `/root/implement_767` wrote `journal-ledger/**` production/tests/functional docs; parent writes only these module-local records and owns Git/GitHub; `/root/explore_767` and independent `/root/review_767` remained read-only.
- Allowlist: `journal-ledger/**`; other modules, shared contracts and repository-level shared harness records are unchanged.
- Result: V15 persists a singleton `OPEN/REBUILDING` barrier with JobInstance owner, frozen range and epoch. The four-step Job blocks ordinary posting/read/overlap, resumes committed checkpoints for the same instance, and releases only after exact GL/SL reconciliation.
- Verification: two audited regressions fail as expected; API local schema regression found during review also fails before its fix. Final writer and independent forced module runs pass core173/API40/batch11 = 224 tests, failures/errors/skips0. API local boot returns HTTP200 `[]`; Batch local boots with V15/validate. Static and quality gates pass.
- Independent review: initial P2 missing API-local V15 table was reproduced with HTTP500 and returned to the writer. The final Flyway/validate fix was independently rerun; no remaining P0-P3 and Q1-Q4 PASS.
- Remote CI: Module Validation, Harness Validation and Agent Merge Guard jobs did not start. Their annotations report failed recent account payments or a spending limit requiring increase; no remote CI test result is claimed.
- Residual/later gates: no live PostgreSQL, distributed process kill, production data, runtime-role permission or load test. Direct SQL and old binaries bypass the barrier. Human review owns Ready/merge/Issue close/deployment; branch/worktree remain retained.

See [worklog.md](worklog.md) for commands, RED/GREEN evidence, rollback and limits. These records are module-local because the user forbids repository shared-harness edits.

---

The following is retained historical status and is not a current GH-767 report.

# GH-764 agent status

- Issue: [#764](https://github.com/skyg547/account/issues/764); remote `status:needs-review`, owner `agent:codex`.
- Status: implementation, audited RED proof, requested local verification and independent review complete; [Draft PR #784](https://github.com/skyg547/account/pull/784) is OPEN/MERGEABLE with `Refs #764`.
- Base: `origin/main@bd2707af704c4a106328c34477f7e3259d827d99`.
- Branch/worktree: `agent/764-preserve-event-decimals`, `/tmp/account-issue-764`; audited proof `agent/764-regression-proof`, `/tmp/account-764-regression-proof` at `a97d10ab`.
- Model assignment: `gpt-5.6-sol`, high. Parent owns Git/GitHub and these module-local records.
- Writers: `/root/controller_764` (inbound production config), `/root/test_764` (new regressions), parent (functional docs/records). Independent reviewer: `/root/review_764`, read-only.
- Allowlist: `journal-ledger/**`; other modules, shared contracts and repository-level shared harness records are unchanged.
- Result: HTTP and Kafka untyped decimal JSON use the same Boot-managed BigDecimal-preserving mapper before real rule interpolation. Existing core `AccountingPrecision` rejects excess scale without rounding.
- Verification: audited MVC regressions2/2 expected failures; focused fixed5/5 PASS; exact requested module208/208 PASS (core164/API39/batch5), failures/errors/skips0; API bootJar PASS. Static gates clean.
- Independent review: initial two P3 gaps corrected by original owners; final no P0–P3, Q1–Q4 PASS.
- Remote CI: Module Validation, Harness Validation and Agent Merge Guard jobs did not start. Their annotations report failed recent account payments or a spending limit requiring increase; no remote CI test result is claimed.
- Residual/later gates: no live broker/deployed-server/production-data/load test. Repository/account owner resolves the billing prerequisite and reruns checks; human reviewer owns Ready/merge/Issue close/deployment. Branch and both worktrees remain retained.

See [worklog.md](worklog.md) for exact commands, RED/GREEN evidence, rollback and limits. These records are module-local because the user forbids repository shared-harness edits.

---

The following is the retained historical GH-761 checkpoint; its remote state is not a current status report.

# GH-761 agent status

- Issue: [#761](https://github.com/skyg547/account/issues/761); remote `status:needs-review`, owner `agent:codex`.
- Status: implementation, local verification and independent review complete; [Draft PR #783](https://github.com/skyg547/account/pull/783) open with `Refs #761`.
- Base: `origin/main@0c0fcd3a7338161e7987c644f9b2c1f31ce29470`.
- Branch/worktree: `agent/761-concurrent-balances`, `/tmp/account-761-concurrent-balances`.
- Model assignment: gpt-6-astra, xhigh. Parent owns Git/GitHub and module-local records.
- Writers: `/root/balance_implementation` (production/docs), `/root/balance_tests` (new integration fixture), parent (existing fixtures and migration regression). Independent reviewer: `/root/balance_review`, read-only.
- Allowlist: `journal-ledger/**`; shared contracts, other modules and shared harness records frozen by user instruction.
- Verification: exact requested module suite203/203; new PostgreSQL16.13 concurrency26/26; initial PostgreSQL existing posting9 and V14 upgrade1 pass. Successful suites have failures/errors/skips0. Audited baseline16/16 expected failures establish the regression. Counts overlap and are not additive.
- Independent review: `/root/balance_review`, no remaining P0–P3, Q1–Q4 PASS; Loan consumer1 PASS and API/Batch bootJARs contain exact V14 source bytes.
- Later gates: human Ready/merge/Issue-close review, coordinated deployment and production load; branch/worktree deletion remains unrequested.

Remote CI did not start because GitHub reports failed account payments or a spending limit needing increase. The repository/account owner must resolve this prerequisite and rerun checks before later readiness gates. No remote test PASS is claimed.

See [worklog.md](worklog.md) for exact commands, artifacts, rollback and limits. These task records are module-local because the user forbids editing shared harness documents.

---

The following is the retained historical GH-760 checkpoint; its remote state is not a current status report.

# GH-760 agent status

- Issue: [#760](https://github.com/skyg547/account/issues/760)
- Status: implementation and local verification complete; remote Issue `status:needs-review`; [Draft PR #782](https://github.com/skyg547/account/pull/782) open with `Refs #760`.
- Base: `origin/main@a97d10ab6efc2570a88f83d630242cd748f8be57`
- Branch: `agent/760-single-posting`
- Worktree: `/tmp/account-760-single-posting`
- Model assignment: gpt-6-astra, xhigh.
- Parent Integrator: Git/GitHub, `journal-ledger/build.gradle`, and these module-local records.
- Module writer: `/root/journal_implementation`, `journal-ledger/**` except parent-owned files.
- Independent reviewer: `/root/posting_review`; read-only, no implementation or Git/GitHub writes; no open P0–P3, Q1–Q4 PASS.
- Scope: journal-ledger only; shared contracts and shared harness records are frozen by user instruction.
- Authorized endpoint: verified change, branch push, Draft PR with `Refs #760`.
- Later gates: Ready, merge, Issue close, deployment, and branch/worktree deletion.

Verification: exact module command170/170; PostgreSQL16.13 regressions14/14; independent Loan consumer1/1; API/Batch bootJARs include the exact V13 migration. All successful suites have failures/errors/skips0. Audited PostgreSQL baseline fails both new concurrent cases with4 GL rows instead of2. See [worklog.md](worklog.md) for commands and limitations.

Remote CI limitation: GitHub Actions did not start the jobs because account billing/spending-limit prerequisites are unmet. No CI test failure or CI pass is claimed; the account owner must resolve the external prerequisite and rerun checks before later readiness gates.

These module-local records satisfy task traceability within the explicit module allowlist. They do not replace or edit the repository's shared harness history.
