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

### GH-779 Draft publication

- Published [Draft PR #789](https://github.com/skyg547/account/pull/789), OPEN/DRAFT, `Refs #779`, from `agent/779-fx-carrying-value` to `main`. Verified implementation commit: `b6d2a9ff8df0ebe8b79c9e51b946c03d1fc2cf8b`. Issue #779 remains OPEN / `status:needs-review`; worktree retained.
- Initial implementation-head GitHub Actions did not start because GitHub annotations report failed recent account payments or a spending-limit restriction. [Module Validation](https://github.com/skyg547/account/actions/runs/36027412303), [Harness Validation](https://github.com/skyg547/account/actions/runs/36027412183) and [Merge Guard](https://github.com/skyg547/account/actions/runs/36027412321) fail before job execution. These are not hosted code/test results. No billing configuration was accessed or changed.
- Next owner: repository account owner to resolve the hosted Actions gate, then human reviewer to check current-head CI and the documented PostgreSQL/operational limits. This publication-only append preserves verified production/test contents. Ready, merge, Issue close, deployment and branch/worktree deletion were not performed.


## GH-780 — intake contract

- Issue [#780](https://github.com/skyg547/account/issues/780); branch `agent/780-fx-valuation-eligibility`; worktree `/tmp/account-780-fx-valuation-eligibility`; base `4e20273c661edde3ab901c5e77f59e1ee5d6d8f2`.
- Delivery: verified Closing-only Draft PR with `Refs #780`. Explicit monetary/historical policy must be effective on the valuation date; unknown policy must not produce FX journals. No generic exception remeasurement or production data correction is included.
- Scope stays in `closing/**`, including these three module-local harness records. Shared harness and other modules are read-only.
- Authority separation: implementation writer changes assigned code/tests; independent reviewer reads and verifies without edits; parent Integrator alone records, commits, pushes and creates the authorized Draft. Read-only agent review is not human/GitHub approval. Ready, merge, Issue close and resource deletion remain separate gates.
- Verification and final ownership will be appended after implementation/review. No completion is claimed at intake.

### GH-780 verified behavior and deployment prerequisites

- Cash/revenue EUR100 at book110/rate1.2 now produces cash gain10 only. Expense/payable and historical fixed/nonfixed-asset/payable pairs produce liability loss10 only. Normal and abnormal balances retain actual debit-positive/credit-negative signs.
- Required full suite426/50suites PASS, failure/error/skip0; API/Batch packaging PASS. Full XML and frozen14 Java hashes: `/tmp/account-780-full-evidence/`. Audited-source RED evidence: `/tmp/issue-780-red/` (expected1 journal, received2 before remediation).
- Before execution, provide effective-dated `fx-valuation-policies` for every foreign candidate account, including `HISTORICAL_COST` exclusions. Preserve approved historical rules and dated Master data across restarts. [Configuration and expected result](../local-run.md#fx-평가-적격성-설정-gh-780); [policy, signs and restart boundaries](../process-flow.md#평가-대상-계정의-유효일자-정책-gh-780).
- Residual checks: actual PostgreSQL plans/load, deployed source/Journal interaction and faults, configuration governance and already-posted ineligible journal reconciliation. Unsupported nonmonetary exceptions require a separate explicit policy implementation.
- Rollback uses a reviewed scoped revert with affected FX jobs suspended; no schema or data reset is required. Parent retains the branch/worktree and all evidence; independent replay and Draft publication follow.

### GH-780 independent review complete

- Read-only `/root/closing_independent_review` verified full426 evidence, independently reran87 focused tests (all PASS, no failure/error/skip) and confirmed14 Java files unchanged. Q1–Q4 PASS; no remaining P0–P3.
- Parent alone performs commit/push/Draft publication. **권한 분리:** 구현자는 Closing 코드·테스트, 독립 리뷰어는 읽기 전용 검증, 부모 Integrator는 기록·Git/GitHub 게시를 담당합니다. AI 리뷰는 사람/GitHub 승인을 대체하지 않습니다. Ready·merge·Issue close·배포·삭제는 별도 승인입니다.
- Next owner: human reviewer of the Draft and its current-head CI, followed by authorized configuration/deployment owners for dated policies and the documented operational gates. PR URL/current-head CI will be appended after actual publication.

### GH-780 Draft publication

- Published [Draft PR #790](https://github.com/skyg547/account/pull/790), OPEN/DRAFT, `Refs #780`, branch `agent/780-fx-valuation-eligibility` to `main`. Verified implementation commit `3fd6568d2a6b73f982c3c70d428288cdac796364`. Issue #780 remains OPEN / `status:needs-review`; worktree retained at `/tmp/account-780-fx-valuation-eligibility`.
- Initial implementation-head hosted checks did not execute: GitHub annotations explicitly report failed recent account payments or a spending-limit restriction. [Module Validation](https://github.com/skyg547/account/actions/runs/36029393022), [Harness Validation](https://github.com/skyg547/account/actions/runs/36029393098), [Merge Guard](https://github.com/skyg547/account/actions/runs/36029392991). These failures are infrastructure gating, not executed code/test results. No billing settings were accessed or changed.
- This publication append changes only the three module records; all14 verified Java files remain frozen. The PR body contains426-test verification, independent87, Q1–Q4 and authority separation. Ready, merge, Issue close, deployment and resource deletion were not performed.
- Next owner: repository account owner to resolve hosted Actions availability, then human reviewer to verify current-head CI and approved effective-dated policies/operational prerequisites. Rollback and residual PostgreSQL/load/data-reconciliation gates remain as documented above.


## GH-781 — verified implementation handoff

- #781 / CL17; `agent/781-ecl-currency-units`; `/tmp/account-781-ecl-currency-units`; base `0709b1154a2333587e85d19335b548fe68028086`. All27 changed paths are Closing-owned (production12/test7/guides5/records3); full list and evidence are in [worklog](worklog.md#2026-09-25--gh-781-ecl-monetary-units).
- Required module command passes492 tests (267/127/98), failures/errors/skips0. API/Batch bootJar PASS, packaged Batch local H2/no-job startup PASS18.7s. Full XML and19 Java hashes: `/tmp/account-781-full-evidence/`; audited-equivalent RED2 evidence: `/tmp/account-781-red-evidence/`.
- Independent review requested/received three additional mixed run/model/entity source-contract cases; these pass. Independent replay and final quality confirmation are pending before publication.
- Run ECL only after existing foreign allowance has been valued and posted at the same closing-date rate. For USD80/KRW104,000 and rate1,400, first post eligible FX adjustment KRW8,000, then a USD100 target produces USD20/KRW28,000. [Workflow and rounding/retry policy](../process-flow.md#ecl-거래통화와-기능통화-대사-gh-781); [local verification](../local-run.md#ecl-충당-job-실행).
- JournalSummary omits exchangeRate; ECL persists pair/rate in its compared description. Existing old-format drafts need reconciliation. Source snapshot concurrency and partial remote success still require operator reconciliation; no distributed atomicity claim. Actual PostgreSQL plans/load, deployed Journal behavior and production data recovery were not tested.
- Rollback: suspend affected ECL jobs, reviewed scoped revert, preserve all posted journals/data/lineage. No schema migration/reset.
- 권한 분리: 구현자는 코드, 테스트 담당은 회귀 검증, 독립 리뷰어는 읽기 전용 검수, 부모 Integrator는 기록·Git/GitHub 게시를 담당합니다. AI 리뷰는 사람/GitHub 승인을 대체하지 않습니다.
- Merge authority: 독립 Reviewer 증거와 최신 head/base·CI 확인 후 별도 승인된 부모 Integrator만 병합합니다. 이번 승인 범위는 Draft 게시까지입니다. Ready·merge·Issue close·배포·삭제는 수행하지 않습니다.
- Next owner: human reviewer of the Draft/current head and authorized accounting/FX operator for operational prerequisites. PR and hosted CI facts will be appended after publication.

### GH-781 review complete

- Independent read-only `/root/closing_independent_review`: focused82 direct replay PASS (Core45/Batch37, fail/error/skip0), full492 archived results independently reconciled;19 Java hashes unchanged. No remaining P0–P3; Q1–Q4 PASS; evidence table is in the module worklog.
- Parent27-path scope/whitespace/conflict/unmerged checks PASS. Authorized commit/push/Draft publication follows; Ready/merge/Issue close/deployment/cleanup remain separate. No remote CI success is claimed before observing the published head.

### GH-781 Draft publication

- Published [Draft PR #791](https://github.com/skyg547/account/pull/791), OPEN/DRAFT, `Refs #781`, from `agent/781-ecl-currency-units` to `main`. Verified implementation commit `1add69bb1b083fc9c5074feba4809fbb77975965`; initial GitHub mergeability is MERGEABLE. Issue #781 remains OPEN / `status:needs-review`; isolated worktree is retained.
- Initial implementation-head hosted checks did not execute: GitHub check annotations report failed recent account payments or a spending-limit restriction. [Module Validation](https://github.com/skyg547/account/actions/runs/36031718246), [Harness Validation](https://github.com/skyg547/account/actions/runs/36031718247), [Merge Guard](https://github.com/skyg547/account/actions/runs/36031718278). These failures are infrastructure gating, not executed code/test results. No billing settings were accessed or changed.
- This publication append changes only the three module records; all19 verified Java files remain frozen. The PR body includes full492, independent82, package/startup evidence, Q1–Q4, rollback, remaining risks and authority separation. Ready, merge, Issue close, deployment and resource deletion were not performed.
- Next owner: repository account owner to restore hosted Actions availability, then human reviewer to verify current-head CI and FX/accounting operational prerequisites. Actual PostgreSQL/load/distributed behavior and existing-data reconciliation remain separate gates.

## GH-778 — final-close evidence handoff

- Issue/branch/worktree: [#778](https://github.com/skyg547/account/issues/778), `agent/778-final-close-evidence`, `/tmp/account-778-final-close-evidence`; Closing-only scope.
- Delivery: immutable typed evidence for six monthly controls plus YEAR annual transfer; freshness/identity/source/digest/account-currency reconciliation; append-only V53 storage; transition evidence binding and recovery fencing; trusted-submitters default deny-all plus dedicated role; no manual override.
- Verification: exact `./gradlew :closing:test` passed 513 tests /57 suites with no failure/error/skip. API and Batch bootJar passed. Independent reviewer found no remaining P0–P3 and marked Q1–Q4 PASS.
- Deployment prerequisites: configure only verified service actors, keep Closing private behind a Gateway that strips/rebuilds identity headers, integrate actual trusted producers, and run real PostgreSQL concurrency plus payload/load checks. Header allowlisting is not mTLS or signed identity. Keep the Issue open until those operational gates and human review are satisfied.
- Rollback after V53 must preserve evidence and transition lineage: drain writers, reconcile Master, and apply a forward corrective migration. Do not drop evidence tables or introduce a fail-open path.
- 권한 분리: 구현/테스트 작성자와 독립 읽기 전용 리뷰어를 분리했고 부모 Integrator만 records·commit·push·Draft PR·Issue 상태를 처리합니다. 이는 절차적 분리이며 같은 실행 환경의 보안 격리나 사람 승인을 뜻하지 않습니다. Ready·merge·Issue close·deployment·branch/worktree 삭제는 별도 승인 대상입니다.

### GH-778 Draft publication

- [PR #809](https://github.com/skyg547/account/pull/809): OPEN/DRAFT, initially MERGEABLE, `Refs #778`; implementation commit `ad30e2e9dc0349b4b46f0ee0b936c2d17ec85613` on latest base `2d7b964eee4ea8d36f2cdabfb5a56df6ab6ca75d`.
- Hosted Module/Harness/Merge Guard jobs were not started because GitHub reported an account payment or spending-limit restriction. Treat this as an external service gate, not a validation result.
- Next owner: repository account owner to restore Actions, then human review and authorized Gateway/producer/PostgreSQL operational verification. Issue remains open; Ready/merge/close/deploy/delete are not authorized.
