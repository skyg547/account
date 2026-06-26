# AI Harness Workflow

## Standard Flow

```text
요구사항 수신
→ 요구사항 요약
→ 영향 범위 분석
→ 작업 분해
→ branch/worktree 생성
→ 역할별 Agent 작업
→ worklog 업데이트
→ 테스트/검증
→ Integrator Agent 통합
→ conflict-log 기록
→ Draft MR/PR 생성
→ 사람 리뷰
→ merge
→ branch/worktree 정리
→ handoff 업데이트
```

초보자 설명: 이 흐름은 "바로 고치기"가 아니라 "무엇을 고칠지 확인하고, 안전한 공간에서 나눠 작업하고, 검증 후 합치는" 절차다.

## Step Details

1. Requirement intake
   - Restate the user request.
   - Identify explicit constraints and non-goals.

2. Impact analysis
   - Check module README and docs.
   - Search controllers, services, repositories, batch jobs, migrations, and tests.
   - List expected files before editing.

3. Work decomposition
   - Assign Planner, Explorer, Coder, Test, Reviewer, Documentation, and Integrator responsibilities.
   - Keep code-writing agents scoped to approved files.

4. Branch or worktree setup
   - Use a non-base branch.
   - Use worktree when two or more agents edit in parallel.

5. Agent work
   - Each agent updates `agent-status.md`.
   - Code-changing agents update `worklog.md`.
   - Review-only agents write findings and do not edit production/test code.

6. Verification
   - Run targeted tests first.
   - Run broader build when shared contracts, build logic, or cross-module behavior changed.
   - Record skipped tests with reason and risk.

7. Integration
   - Integrator Agent merges or rebases agent branches.
   - Record conflicts in `conflict-log.md`.
   - Resolve semantic conflicts with human or high-reasoning review.

8. Draft MR/PR
   - Include summary, changed files, tests, rollback, risks, and reviewer checklist.
   - Keep it draft until tests and human review are complete.

9. Handoff
   - Update `handoff.md` with current branch, status, next commands, known risks, and owner.

