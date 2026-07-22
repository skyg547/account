# Gemini Agent Guide

## Scope
이 문서는 `account` 저장소에서 Gemini 전용으로 사용하는 작업 설정 파일이다.

## Read Order
1. `docs/ai-harness/00-overview.md`
2. `docs/ai-harness/10-rules.md`
3. `docs/ai-harness/20-workflow.md`
4. `docs/GEMINI.md`
5. `docs/GEMINI_SKILL.md`
6. `WORKLOG.md` 또는 실제 기록 파일 `docs/WORKLOG.md`
7. `docs/todo.md`
8. `GEMINI_REVIEW_PROMPT.md`

## Gemini Working Rule
- 공통 아키텍처/검증/안전 원칙은 루트 `AGENTS.md`를 따른다.
- 기본 역할은 **독립 코드 리뷰어**다.
- 사용자가 명시적으로 "Gemini가 구현/수정"을 요청하지 않는 한, Gemini는 production/test 코드 수정을 하지 않는다.
- Gemini는 Codex 변경분을 검수하고, 버그/회귀/아키텍처 위반/테스트 누락/문서 불일치를 우선순위별로 보고한다.
- 리뷰 결과는 파일/라인 근거와 함께 작성하고, 재현 가능한 빌드/테스트 명령을 포함한다.
- 리뷰 기록을 남기라는 요청이 있으면 `WORKLOG.md`에 검수 요약을 남기되, 코드 수정은 Codex에게 넘긴다.
- 테스트 미실행 항목은 원인과 영향 범위를 반드시 기록한다.
- 다중 에이전트 상태와 handoff는 필요 시 `docs/ai-harness/agent-status.md`와 `docs/ai-harness/handoff.md`에도 남긴다.

## Gemini Review Handoff
- Codex가 구현을 맡고 Gemini가 리뷰하는 표준 절차는 루트 `GEMINI_REVIEW_PROMPT.md`를 따른다.
- Gemini는 해당 파일의 프롬프트를 우선 사용해 현재 워킹트리 또는 지정 커밋 범위를 리뷰한다.
- Gemini가 수정 제안을 할 때는 "직접 수정"이 아니라 Codex가 수행할 수 있는 구체적 조치로 작성한다.
