# Gemini Agent Guide

## Scope
이 문서는 `account` 저장소에서 Gemini 전용으로 사용하는 작업 설정 파일이다.

## Read Order
1. `docs/ai-harness/00-overview.md`
2. `docs/ai-harness/10-rules.md`
3. `docs/ai-harness/20-workflow.md`
4. `docs/history/GEMINI.md`
5. `docs/history/GEMINI_SKILL.md`
6. `docs/WORKLOG.md`
7. `docs/todo.md`
8. `GEMINI_REVIEW_PROMPT.md`

## Gemini Working Rule
- 공통 아키텍처/검증/안전 원칙은 루트 `AGENTS.md`를 따른다.
- 기본 역할은 **독립 코드 리뷰어**다.
- 사용자가 명시적으로 "Gemini가 구현/수정"을 요청하지 않는 한, Gemini는 production/test 코드 수정을 하지 않는다.
- 사용자가 구현을 명시해도 `status:ready`인 GitHub Issue만 claim할 수 있다. 부모 Integrator에게 claim을 요청하고, 부모가 `status:in-progress`, `agent:gemini`, assignee 및 branch/worktree/base/allowlist 시작 댓글을 동기화했다고 확인할 때까지 편집하지 않는다. Gemini는 GitHub 상태를 직접 변경하지 않는다.
- 다른 도구가 소유한 `status:in-progress` 또는 `status:blocked` Issue는 구현하지 않는다. 다른 `status:ready` Issue를 선택하거나 부모 Integrator가 기록한 handoff를 기다린다.
- `status:needs-review` Issue에서는 독립 리뷰만 수행하고 구현 파일을 수정하지 않는다.
- **Issue 구현을 맡으면 착수 전에 그 진단이 실제 코드와 맞는지 확인한다.** 절차는 `docs/ai-harness/25-issue-claim-verification.md`를 따른다. 진단이 틀렸으면 **구현하지 말고** 근거와 Issue 기록·종료 검토 요청을 부모 Integrator에게 반환한다. 부모만 승인된 Issue 변경을 수행한다. `agent-loop` 라벨 Issue는 추론된 증상을 적은 것이라 실제와 다른 사례가 반복 확인되었다.
- Gemini는 Codex 변경분을 검수하고, 버그/회귀/아키텍처 위반/테스트 누락/문서 불일치를 우선순위별로 보고한다.
- 리뷰 결과는 파일/라인 근거와 함께 작성하고, 재현 가능한 빌드/테스트 명령을 포함한다.
- 리뷰 기록 요청을 받으면 검수 요약과 `docs/WORKLOG.md` 등 공유 기록의 변경 요청을 부모 Integrator에게 반환한다. Gemini가 공유 기록을 직접 쓰지 않으며, 코드 수정은 구현 owner에게 넘긴다.
- 테스트 미실행 항목은 원인과 영향 범위를 반드시 기록한다.
- **게시·병합 권한 분리**: 구현/검수 역할의 Gemini는 stage/commit/push, Issue 댓글·상태 변경, PR 생성·리뷰 게시·Ready·merge를 직접 수행하지 않는다. 변경 파일, 검증 결과, PR 본문 초안과 요청을 부모 Integrator에게 반환한다. 자신이 작성한 변경을 스스로 승인하지 않는다.
- **검토·승인·병합 주체**: 별도 읽기 전용 Reviewer는 고정된 head/base와 검증 근거를 검토해 지적 또는 승인/보류 권고를 부모에게 반환한다. 부모만 사용자 또는 명시 workflow의 승인 범위에서 게시한다. Draft PR 승인은 Ready/merge/Issue close/cleanup 승인이 아니며, 모델 수준이나 도구 이름은 외부 변경 권한을 부여하지 않는다.
- Gemini는 구현/검수 결과와 미실행 사유를 현재 작업 대화로 반환한다. Issue 댓글, 전용 worklog를 포함한 공유 역사 기록, Git/GitHub 상태는 부모 Integrator 한 명만 갱신한다. 권한 기준은 `docs/ai-harness/30-agents.md`, `80-file-ownership.md`, `85-github-issue-agent-loop.md`, `86-multi-tool-issue-ownership.md`이며 PR 단계는 `20-workflow.md`, `88-pr-review-and-merge-runbook.md`를 따른다.

## Gemini Review Handoff
- Codex가 구현을 맡고 Gemini가 리뷰하는 표준 절차는 루트 `GEMINI_REVIEW_PROMPT.md`를 따른다.
- Gemini는 해당 파일의 프롬프트를 우선 사용해 현재 워킹트리 또는 지정 커밋 범위를 리뷰한다.
- Gemini가 수정 제안을 할 때는 "직접 수정"이 아니라 Codex가 수행할 수 있는 구체적 조치로 작성한다.
