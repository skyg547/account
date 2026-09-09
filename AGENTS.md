# Account Repository Agent Guide

## Scope And Purpose

- 이 파일은 저장소 전체의 Codex 기본 계약이다. 더 가까운 하위 `AGENTS.md`가 있으면 해당 디렉터리에서 우선한다.
- 이 프로젝트는 자산운용/재무회계 시스템을 모듈형 MSA와 헥사고날 아키텍처로 구축한다.
- 반복 절차는 `.agents/skills/`, 역할별 실행 규칙은 `.codex/agents/`, 상세 정책과 기록은 `docs/ai-harness/`에서 관리한다.

## Non-Negotiable Rules

- 기존 구조와 사용자 변경을 먼저 확인하고 승인된 범위만 최소 변경한다. 다른 작업자의 변경을 되돌리지 않는다.
- 모든 변경은 `agent/`, `ai/`, `feature/`, `fix/` 브랜치에서 수행한다. `main`, `master`, `develop`에 직접 push하지 않는다.
- 현재 checkout이 dirty이거나 병렬 작업이면 프로젝트 밖의 별도 worktree를 사용한다.
- 운영 DB, 계정정보, 토큰, 비밀번호, 개인정보, 운영 URL 등 민감정보를 읽거나 출력하지 않는다.
- 패키지 설치, 외부 스크립트, 권한 변경, 파괴적 파일/Git 작업은 사용자 승인 없이 수행하지 않는다.
- 구현자와 리뷰어를 분리한다. 리뷰 전용 에이전트는 production/test 코드를 수정하지 않는다.
- conflict marker가 남아 있거나 검증 근거가 없으면 완료, commit, merge로 판단하지 않는다.
- push, PR Ready 전환, merge, Issue close, branch/worktree 삭제는 사용자 요청 또는 명시된 승인 단계에서만 수행한다.

## Architecture Contract

- Port/Adapter 경계를 지키고 Controller, application, domain, infrastructure 책임을 섞지 않는다.
- API 변경은 Controller, DTO, use case, test 영향을 함께 확인한다.
- Batch 모듈은 Trigger, Job/Step 흐름, 병렬성, chunk 설정을 담당한다. 비즈니스 계산과 상태 전이는 core에 둔다.
- Core의 `application.pipeline`은 batch 변환, `application.service`는 유즈케이스 조정, `domain`은 핵심 규칙, `infrastructure`는 기술 어댑터를 담당한다.
- 금액과 정밀 계산은 `BigDecimal`을 사용하고 상태 전이, 마감, 승인, 라인리지, 재실행 정합성을 우선한다.
- 인터페이스만 남기는 skeleton 구현과 검증 없는 단순 전달형 service를 만들지 않는다.
- 대량 처리 변경은 JDBC bulk/partitioning, 멱등성, 재시작 지점, 성능 영향을 확인한다.

## Implementation Quality

- 승인된 변경에서 명확한 이름, 응집된 작은 함수, 단일 책임, 중복 최소화와 불필요한 추상화 방지를 지킨다. 기존 MSA·헥사고날·DDD 경계, 성능과 금융 정합성을 우선한다.
- 핵심 흐름의 입력→처리→출력·설계 이유·예외·경계를 적절한 주석 또는 기능 문서로 설명하고, 변경된 비자명 로직 가까이에 의도 주석을 둔다. 유익한 주석은 보존하고 오래된 설명은 고치되 매줄 주석이나 장문 중복을 강요하지 않는다.
- 검증된 코드·명령에 맞게 모듈 문서와 초보자 설명을 갱신한다. 품질 개선을 이유로 범위 밖 리팩터링·일괄 주석·문서 정리를 하지 않는다.

## Code Review Rules

- 구현·완료·독립 리뷰에서 [코드·문서 품질 계약](docs/ai-harness/42-code-documentation-quality.md)의 Q1–Q4 확인과 증거표 제출은 필수다.
- 독립 리뷰어는 항목별 PASS/FAIL/N/A와 파일·테스트 근거, 구체적 N/A 사유를 확인한다. FAIL·근거 누락이면 완료로 처리하지 않고 원래 작성자에게 반환한다. 테스트 미실행은 PASS가 아니며 위험·다음 검증 게이트를 기록한다.
- 리뷰어의 읽기 전용 역할과 부모의 Git/GitHub·Ready·merge 승인 권한은 바뀌지 않는다.

## Start Order

1. `docs/WORKLOG.md`, `docs/history/CODEX_WORKLOG.md`, `docs/ai-harness/worklog.md`의 최신 관련 항목을 확인한다.
2. `git status --short --branch`와 적용되는 Issue/branch/worktree를 확인한다.
3. `docs/ai-harness/00-overview.md`, `10-rules.md`와 대상 모듈의 `README.md`, `docs/*.md`를 읽는다.
4. Issue 기반 작업은 `$account-issue-loop`, 코드 변경은 `$account-hexagonal-change`를 사용한다.
5. 요구사항, 영향 범위, 변경 예정 파일, 검증 계획을 제시한 뒤 편집한다.

## Delegation

- `.codex/agents/`의 역할을 사용하고 각 쓰기 에이전트에 파일 allowlist를 명시한다.
- read-heavy 탐색과 리뷰는 병렬화할 수 있다. 같은 파일을 여러 쓰기 에이전트에 동시에 배정하지 않는다.
- 공유 로그, conflict 판단, 최종 Git/PR 상태는 부모 Integrator 한 명만 갱신한다.
- 역할과 파일 소유권은 `docs/ai-harness/30-agents.md`, `80-file-ownership.md`, `95-codex-skills-subagents.md`를 따른다.

## Completion Contract

1. 대상 테스트를 먼저 실행하고 공용 계약이나 빌드 구성이 바뀌면 검증 범위를 넓힌다.
2. `$account-review-handoff`로 diff, conflict marker, 테스트, 롤백, 잔여 위험을 점검한다.
3. 변경 파일과 영향 범위, 실행한 명령과 결과, 미실행 사유를 보고한다.
4. 의미 있는 변경은 `docs/ai-harness/worklog.md`, `agent-status.md`, `handoff.md`, `docs/history/CODEX_WORKLOG.md`에 기록한다. 충돌 시 `conflict-log.md`도 갱신한다.
5. 통합은 Draft PR과 사람 리뷰를 기본 게이트로 사용한다.

## Entry Points

- 공통 규칙: `docs/ai-harness/10-rules.md`
- 전체 흐름: `docs/ai-harness/20-workflow.md`
- 역할/소유권: `docs/ai-harness/30-agents.md`, `80-file-ownership.md`
- Git/Issue/PR: `docs/ai-harness/50-git-worktree-guide.md`, `60-rebase-merge-policy.md`, `85-github-issue-agent-loop.md`
- 스킬/서브 에이전트: `docs/ai-harness/95-codex-skills-subagents.md`
- 초보자 가이드: `docs/ai-harness/90-beginner-ai-agent-git-guide.md`
- Gemini 독립 리뷰: `GEMINI.md`, `GEMINI_REVIEW_PROMPT.md`
