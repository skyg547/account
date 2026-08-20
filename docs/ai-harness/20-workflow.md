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
→ Draft MR/PR 생성
→ 사람 리뷰
→ merge
→ Issue/로그 동기화
→ branch/worktree 정리
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
   - Return changed files and verification evidence to the parent.

6. Verification and review
   - Run targeted tests first and expand when shared contracts or wiring changed.
   - Review correctness, security, financial invariants, batch restartability, SQL impact, and missing tests.
   - Run diff and conflict-marker checks.

7. Integration
   - Let the parent Integrator make final conflict, Git, PR, and shared-log decisions.
   - Record semantic conflicts in `conflict-log.md`.
   - Keep the PR draft until checks and human review are complete.

8. Completion
   - Merge through the PR gate.
   - Verify PR and Issue state.
   - Update worklog, status, integration log, and handoff.
   - Remove branches/worktrees only after merge verification and cleanup approval.

## Required Draft PR Content

Include Issue reference, branch/worktree identity, summary, changed files, architecture impact, tests, skipped checks, rollback, risks, reviewer checklist, and any conflict decisions.
