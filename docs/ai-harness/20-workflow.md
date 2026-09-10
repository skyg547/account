# AI Harness Workflow

## Standard Flow

```text
요구사항 수신
→ 요구사항/완료 조건 요약
→ Issue 선택 또는 생성
→ 영향 범위 분석
→ 작업 분해
→ agent branch/worktree 생성
→ 역할별 subagent 작업
→ 부모 Integrator 결과 수집
→ 테스트/검증
→ conflict 판단 및 기록
→ 부모 Integrator의 승인 범위 내 Draft MR/PR 생성
→ 동결 Draft 사전리뷰 및 Draft 단계 필수 검사 통과 (사람 리뷰 기본)
→ 부모 Ready 전환
→ 최신 head/base 기준 최종 독립 검토 및 Ready-only 포함 모든 필수 CI 통과
→ 별도 승인된 부모 merge
→ 종료 조건·승인 확인 후 Issue/로그 동기화
→ 별도 cleanup 승인 후 branch/worktree 정리
→ handoff 완료
```

## Skill Routing

- Issue, branch, worktree, PR 추적: `$account-issue-loop`
- API/core/batch/SQL 코드 변경: `$account-hexagonal-change`
- 다중 모듈 MSA 병렬 작업: `$account-module-parallel`
- diff 검수, 테스트, 로그, handoff: `$account-review-handoff`

간단한 설명이나 read-only 상태 확인에는 Issue와 작업 브랜치를 강제하지 않는다. 실행 가능한 코드/문서 변경, 병렬 작업, 장기 추적이 필요한 작업에는 Issue를 사용한다.

## Execution Steps

1. Intake
   - Restate goal, non-goals, constraints, and done conditions.
   - Read an existing Issue and comments when supplied.
   - Verify the Issue's diagnosis against the code before implementing it. See `25-issue-claim-verification.md`. Generated Issues describe an inferred symptom, not a confirmed one, and several have been wrong in ways that would have damaged the code if implemented as written.

2. Impact analysis
   - Read applicable `AGENTS.md`, harness rules, module README, and module docs.
   - Trace API, application, domain, persistence, batch, build, and test impact.
   - List expected files before editing.

3. Isolation
   - Require a non-base branch for changes.
   - Use an external worktree for dirty checkouts, parallel writers, or explicit isolation.
   - Record Issue, branch, worktree, and base branch.

4. Delegation
   - Use Planner/Explorer for read-only analysis.
   - Assign Controller/Service/Batch/SQL/Test/Documentation agents disjoint file allowlists.
   - Keep Reviewer and advisory Integrator read-only.
   - Continue unrelated parent work while subagents run; wait only when their output blocks the next step.

5. Implementation
   - Follow existing architecture and the approved allowlist.
   - Do not let subagents update shared harness logs.
   - Return changed files, verification evidence, and a PR body draft to the parent; implementers do not mutate Git/GitHub state.

6. Verification and review
   - Run targeted tests first and expand when shared contracts or wiring changed.
   - Review correctness, security, financial invariants, batch restartability, SQL impact, and missing tests.
   - Run diff and conflict-marker checks.

7. Integration
   - Let the parent Integrator make final conflict, Git, PR, and shared-log decisions.
   - Record semantic conflicts in `conflict-log.md`.
   - 부모 Integrator만 승인 범위 안에서 commit/push/Draft PR 생성·리뷰 결과 게시·Ready 전환을 수행한다. Reviewer와 advisory Integrator는 읽기 전용이다.
   - 부모가 명시 요청한 동결 Draft 사전리뷰는 가능하다. 작업 중 Draft를 전역 자동 인수하거나 병합하지 않는다.
   - 동결 diff 사전리뷰와 실행 가능한 Draft 단계 필수 검사 통과 후 승인된 부모 Ready 전환을 한다. Ready-only 검사는 미도래/pending으로 기록하며 PASS가 아니지만 Draft 사전리뷰를 막지 않는다.
   - Draft 단계 필수 검사의 오류·실패는 여전히 보류 또는 확인된 코드 결함 반려다. Ready 후 최종 독립 검토에서는 최신 head/base·수용 기준과 Ready-only 포함 모든 필수 CI 통과를 비작성자 독립 세션에서 확인한다. head/base 또는 검증 결과가 달라지면 재검토한다.
   - 사람 리뷰는 기본 게이트다. 기존 명시 승인 workflow 예외는 등록된 범위에서만 적용하며, 모델 티어 선택이나 Draft PR 요청 자체는 Ready/merge 승인이 아니다. 상세 단계와 보류 기준은 `88-pr-review-and-merge-runbook.md`를 따른다.

8. Completion
   - Merge through the separately approved parent PR gate (merge commit per `85-github-issue-agent-loop.md`).
   - Verify the merge before the separately approved Issue close gate; do not combine merge and cleanup.
   - Update worklog, status, integration log, and handoff.
   - Remove branches/worktrees only after merge verification and cleanup approval.

## Required Draft PR Content

Include Issue reference, branch/worktree identity, summary, changed files, architecture impact, tests, skipped checks, rollback, risks, reviewer checklist, and any conflict decisions.
