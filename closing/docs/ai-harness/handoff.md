# GH-772 handoff

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
