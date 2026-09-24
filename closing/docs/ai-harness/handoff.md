# Closing handoff

## GH-772 historical handoff

This is a **Closing-side implementation, not a completed system-wide journal admission fix**. The Issue must remain open and the PR must remain Draft until the outstanding integration is explicitly scoped, implemented and verified.

- Issue: [#772](https://github.com/skyg547/account/issues/772); dependency: [GL07/#762](https://github.com/skyg547/account/issues/762).
- Branch/worktree: `agent/772-closing-journal-admission`, `/tmp/account-772-closing-journal-admission`.
- Base: `8e476f1a7caf2612860652df5069302abd6599ec`.
- Scope: `closing/**` only. Shared harness documents are deliberately unchanged under the user contract; records live here.
- Runtime policy: [process flow](../process-flow.md#일반-전표-허용-판정-gh-772).
- API and verification command: [local run](../local-run.md#일반-전표-허용-여부-조회).

## Acceptance and remaining gates

| Issue acceptance | Closing-side evidence | Remaining gate |
| --- | --- | --- |
| Active locks and closing progress deny ordinary posting in local/remote modes | Composite query, local status-port adapter and HTTP provider | Standalone Journal still selects Master-only status adapter; select/integrate Closing consumer in both deployment modes |
| Missing/unknown state fails closed; governed adjustments if policy requires | Explicit OPEN allowlist, failed lookups reject, no text-based adjustment bypass | Journal's existing unknown-state logic remains outside scope; define trusted adjustment permit separately |
| Lock/unlock and start/close observable through actual Journal port | Real compiled Journal filter/posting consumer explicitly assembled with Closing provider; persisted HTTP lifecycle checks | Actual deployed consumer selection, interservice failure and restart evidence |
| Regression fails on audited code and preserves controls | Pre-change 4/4 RED against byte-identical audited production files; full module suite 314/314 passed | GL07 barrier-controlled close-vs-post commit proof remains absent |

The endpoint is a query snapshot. No admission reservation, draining of in-flight transactions, durable epoch, commit fence or distributed atomicity is supplied. A second read, cache setting or process-local mutex is not a substitute. EOD and overlapping quarter/year propagation are outside this monthly lookup.

## Rollback and authority

Rollback is a reviewed revert of this Issue's Closing-only code/test/build/docs changes; no data repair or migration reversal is required. Preserve historical records and the branch/worktree for follow-up.

**권한 분리:** 구현 에이전트는 지정된 파일만 변경하고, 독립 리뷰어는 읽기 전용으로 검증합니다. 부모 Integrator만 승인된 commit·push·Draft PR 생성과 기록 갱신을 수행합니다. PR Ready 전환, merge, Issue close, 배포 및 branch/worktree 삭제는 별도 사용자 승인과 후속 검증이 필요합니다.

Verification and review evidence is recorded below; the Draft PR link is recorded after publication.

## Verified evidence

- Requested `./gradlew :closing:test`: **314/314 PASS**, Core199/API95/Batch20; 41 suites, failures/errors/skips0, exit0.
- Narrow checks: core94 and API21 PASS, included in the full total. Pre-change audited-identical service/adapter: expected RED4/4.
- Real local execution: random-port HTTP server, H2, transaction-proxied Closing commands and fresh requests after commits. This is not production PostgreSQL or a deployed Journal connection.
- Source/test ownership is frozen. Parent's remaining writes are module documentation/records and Git/GitHub publication.
- Local evidence: `/tmp/account-772-full-test.log`, `/tmp/account-772-full-evidence/`, `/tmp/account-772-api-focused.log`.

- Independent `/root/review_772`: no new scoped P0–P3 findings; Q1–Q4 PASS after independent source/XML/log comparison; full Issue acceptance HOLD.
- API/Batch boot JARs: PASS, 14s/exit0; both omit Journal runtime JARs. Artifact hashes and detailed Q1–Q4 table are in [worklog](worklog.md#independent-review-and-packaging).
- Next reviewer should review this Closing supplier as a partial Draft only. The next integration owner needs explicit Journal/shared-contract scope and must retain the Issue's original acceptance criteria.

## Published Draft

- [PR #787](https://github.com/skyg547/account/pull/787): OPEN/DRAFT, `Refs #772`.
- Verified implementation commit: `46800ead5d643d81b6beeaea75b8e25b16e6ad23`; follow-up commits update publication records only.
- Initial-head hosted CI jobs were not started: GitHub reported an account payment/spending-limit restriction ([Module Validation](https://github.com/skyg547/account/actions/runs/36022330652), [Merge Guard](https://github.com/skyg547/account/actions/runs/36022330328)). Account configuration is outside scope; no remote test pass is claimed.
- Full #772 acceptance remains HOLD. Keep the Issue open and PR Draft while the next reviewer evaluates the Closing-side supplier and the cross-module scope is resolved.

## GH-774 — verified implementation

- Issue: [#774](https://github.com/skyg547/account/issues/774).
- Branch/worktree: `agent/774-closing-decision-consistency`, `/tmp/account-774-closing-decision-consistency`.
- Base: `1d3e6264c6703bace265186f3319f44407dc1458`.
- Scope: `closing/**`; these module-local records satisfy the requested harness update without changing shared harness documents.
- Required full verification passed: 348 tests, failures/errors/skips0. Independent review Q1–Q4 PASS, no unresolved findings; API/Batch packaging PASS. Draft publication is recorded below.
- Parent Integrator owns commit, dedicated-branch push, Draft PR and records. The independent reviewer is read-only. Ready, merge, Issue close, deployment and cleanup are separate authorization gates.

### GH-774 acceptance evidence

| Acceptance criterion | Evidence and boundary |
| --- | --- |
| One approve/reject decision; loser conflict | Both winning schedules use controlled Master lookup/PUT barriers and independent Closing transactions. The losing HTTP call returns409; durable approval/calendar/Master/audits agree. |
| Final calendar/Master agreement after recovery | Independent Master H2 commit plus lost response, local finalization rollback and wrong response ID; PREPARED resume; one terminal audit; no DISPATCHED resend; stale operation rejected. Unresolved source-state DISPATCHED remains fenced and requires supervised Master reconciliation. |
| Concurrent monthly aggregate policy | Six start/task-create/task-update/gate-create/gate-pass/close contenders wait on a database root lock and validate its new state; actual close-versus-close makes one Master write. Managed stale calendar/task/gate/list cases are refreshed. This is not an exhaustive pairwise or PostgreSQL load test. |
| Regressions and existing financial controls | Pre-fix RED1/failure1; final focused28; full348/45 suites, no failures/errors/skips. Maker/checker, task/gate evidence, admission and adjustment checks retained. |

`./gradlew :closing:test` passed in1m14s, exit0, Core201/API127/Batch20. The parent task is expected to be NO-SOURCE; all three child test tasks executed. Exact evidence: `/tmp/account-774-full-test.log`, `/tmp/account-774-full-evidence/summary.json` and archived XML. Focused counts are subsets of348.

Recovery and rollout instructions are in [process flow](../process-flow.md#월말-동시-결정과-복구-gh-774), [schema](../schema.md#월말-전이-기록-gh-774) and [local run](../local-run.md#월말-전이-조회와-복구-gh-774). Apply V52 and drain old monthly writers before new traffic. Preserve unresolved intent/audit evidence during rollback; supervise termination and Master reconciliation before reverting the writer. Real PostgreSQL/deployed Master faults/process kills/load and production-data repair were not performed. The original-request termination flag is an operator attestation, not machine-verifiable remote completion.

Independent `/root/closing_774_review` confirmed Q1–Q4 PASS and no unresolved findings on the verified source. Both boot JARs passed; exact V52 contents and absence of a Journal runtime dependency were checked. Artifact hashes, quality table and limits are in the GH-774 section of [worklog](worklog.md).

**GH-774 권한 분리:** 구현/테스트 작성자와 독립 읽기 전용 리뷰어가 분리되었습니다. 부모만 commit·전용 브랜치 push·Draft PR·모듈 하네스 기록을 처리합니다. 이는 절차상의 분리이며 같은 실행 환경의 보안 격리를 뜻하지 않습니다. Ready·merge·Issue close·배포·자원 삭제는 별도 승인 대상입니다.

### GH-774 Draft publication

[PR #788](https://github.com/skyg547/account/pull/788) is OPEN/DRAFT, `Refs #774`, branch `agent/774-closing-decision-consistency` to `main`. Verified implementation commit: `9a4348071ca5de39ecee266c08a0009cae16a722`. Issue #774 is OPEN / `status:needs-review`. Worktree is retained.

Initial implementation-head hosted CI did not execute its jobs: GitHub annotations report failed recent account payments or a spending-limit restriction. [Module Validation](https://github.com/skyg547/account/actions/runs/36024856421), [Harness Validation](https://github.com/skyg547/account/actions/runs/36024856552) and [Merge Guard](https://github.com/skyg547/account/actions/runs/36024856595) are failed before execution; no hosted code/test pass is claimed. The repository account owner must resolve that external gate before current-head CI can run. No billing settings were inspected or changed.

Next owner: repository account owner for the CI service gate, then human reviewer for current-head checks and rollout prerequisites. Ready, merge, Issue close, deployment and branch/worktree deletion have not been performed.

## GH-779 — implementation handoff

Issue [#779](https://github.com/skyg547/account/issues/779) uses `agent/779-fx-carrying-value` at `/tmp/account-779-fx-carrying-value`, base `363cdff48069eb5930ae4a45b0f5e56045d66122`. Scope is `closing/**` including these module-local records. Implementation and verification are in progress; the final evidence and Draft PR link will be appended after review.

The source already has durable attribution: `FX_VALUATION` / `batch|account|sourceCurrency`. Reporting-currency FX account legs must change only carrying value; effective `REVERSAL` / `JOURNAL_ENTRY` ancestry must retain this attribution. Parent owns Git/GitHub and documentation; SQL and test writers have disjoint allowlists; independent review remains read-only.

### GH-779 verified behavior and remaining gates

| Acceptance criterion | Evidence |
| --- | --- |
| Same-rate later month produces zero | `FxValuationPostedHistoryTest.unchangedRateAfterPostedValuationCreatesNoAdditionalJournal`: USD100/base1000, posted200, next rate12 no new command; book1200/principal100. |
| Rising/falling, liability and abnormal signs, multiple currencies | Four explicit normal/actual sign combinations over rates12→13→11; USD/EUR on one account with separate lineage and200/100 incremental adjustments. |
| Posted-journal carrying value and reversal lineage | Actual Reader/service feedback through synthetic persisted commands; root + posted reversal + reversal-of-reversal, own date/status cutoff, P&L/domestic exclusion, foreign principal unchanged. |
| Failing regression and preserved controls | Untouched-production RED1/failure1; corrected focused32 PASS; whole378 PASS. Residual-balance fail guard, malformed lineage/header/leg controls, posting default and deterministic retry/cursor restart retained. |

Required `./gradlew :closing:test`:378tests/46suites, failure/error/skip0, exit0,1m23s. API/Batch bootJar both PASS. See [worklog](worklog.md) for exact commands, RED fixture distinction, archived XML and risks. Independent review final confirmation and Draft publication follow this checkpoint.

No migration/backfill or shared contract change. Real PostgreSQL execution/load, deployed Journal writes and distributed faults were not exercised; orphan lineage and concurrent-posting inputs require operational reconciliation/frozen-input controls. A scoped code revert needs review and suspension of affected FX valuation because the old reader repeats adjustments. Existing posted data must remain intact.

**GH-779 권한 분리:** SQL 구현자·테스트 작성자와 독립 읽기 전용 리뷰어를 분리했습니다. 부모 Integrator만 commit·전용 브랜치 push·Draft PR 생성·모듈 하네스 갱신을 수행합니다. 이는 절차상의 권한 분리이며 같은 실행 환경의 보안 격리 또는 GitHub 사람 승인을 의미하지 않습니다. Ready·merge·Issue close·배포·branch/worktree 삭제는 후속 승인 게이트입니다.

### GH-779 independent acceptance

`/root/fx_independent_review` directly reran32/32 focused tests successfully, confirmed full378-test evidence and final source/test hashes, and found no unresolved P0–P3. Q1–Q4 all PASS; detailed evidence is in [worklog](worklog.md#independent-review-and-delivery-gate). Parent owns the authorized commit/push/Draft gate; the next owner after publication is a human reviewer for current-head CI and operational limits.
