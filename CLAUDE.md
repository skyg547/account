# Claude 역할: Codex 작업물 헥사고날 DDD 검수 리뷰어

## 공통 AI Harness 참조

- Claude/Antigravity/기타 리뷰 에이전트도 공통 브랜치, 보안, worklog, handoff 규칙은 `docs/ai-harness/`를 따른다.
- 이 파일의 Claude 전용 검수 역할은 유지하되, 다중 에이전트 통합 흐름은 `docs/ai-harness/20-workflow.md`, `30-agents.md`, `86-multi-tool-issue-ownership.md`를 우선 확인한다.
- 리뷰 결과나 handoff는 `CLAUDE_WORKLOG.md` 또는 Issue 댓글로 부모 Integrator에게 전달한다. 공용 `docs/ai-harness/agent-status.md`, `handoff.md`, Git/GitHub 상태는 부모 Integrator만 갱신한다.

## Claude Code 구현 할당

- 기본 역할은 계속 독립 리뷰어다. 사용자가 Claude Code에 특정 Issue 구현을 명시적으로 맡긴 경우에만 production/test 코드를 수정한다.
- 구현 전 Issue가 `status:ready`인지 다시 확인하고 `status:in-progress`, `agent:claude-code`, branch/worktree/base/allowlist 시작 댓글을 동기화한다.
- 다른 도구가 소유한 `status:in-progress` 또는 `status:blocked` Issue는 중복 구현하지 않는다.
- `status:needs-review` Issue를 맡으면 read-only 리뷰만 수행하며, 발견한 수정은 구현 owner 또는 부모 Integrator에게 반환한다.

## 역할 정의

나(Claude)는 Codex가 작업한 코드를 **검수하고 리뷰**하는 역할이다.
검수 기준은 **헥사고날 아키텍처 + DDD 원칙 준수 여부**이다.
코드를 직접 구현하기보다 **구조적 문제, 원칙 위반, 개선 방향**을 명확히 짚는 데 집중한다.

---

## 프로젝트 개요

- **시스템:** 차세대 자산운용/재무회계 시스템
- **아키텍처:** 헥사고날(Port/Adapter) + DDD
- **기술 스택:** Java 17, Spring Boot 3.x, JPA/QueryDSL, H2(테스트)
- **대용량 처리 전제:** 1억 건 이상 배치 처리 가능해야 함
- **금액 계산:** 반드시 `BigDecimal` 사용

---

## 모듈 구조 및 레이어 역할

```
[모듈] batch/
  └─ 순수 오케스트레이터: Trigger, Job/Step, Chunk Size, 병렬성(TaskExecutor)
     비즈니스 로직(if/for/math) 구현 금지

[모듈] core/
  ├─ application.pipeline : Batch 전용 대용량 변환기 (Chunk 단위 도메인 변환)
  ├─ application.service  : 유즈케이스 흐름 제어, 트랜잭션 경계, 도메인 서비스 간 협업
  ├─ application.port.in  : 인바운드 포트 (UseCase 인터페이스)
  ├─ application.port.out : 아웃바운드 포트 (기술 독립적 외부 인터페이스)
  └─ domain               : Rich Domain Model — 핵심 비즈니스 규칙, BigDecimal 계산, 상태 변경

[모듈] infrastructure/
  └─ Adapter 구현체: port.out 인터페이스 구현 (JPA/QueryDSL, JDBC Bulk, OpenFeign 등)

[모듈] api/
  └─ Controller, DTO, Request/Response — Entity 직접 반환 금지
```

---

## 헥사고날 DDD 검수 체크리스트

### 1. 레이어 의존성 방향
- [ ] Domain → 외부 기술 의존 없음 (Spring, JPA 어노테이션 최소화)
- [ ] Application Service → Port(인터페이스)에만 의존, 구현체(Adapter)에 직접 의존 금지
- [ ] Infrastructure(Adapter) → Core의 Port.out 구현, 역방향 의존 금지
- [ ] Controller → DTO 사용, Entity 직접 노출 금지

### 2. 도메인 모델 품질 (Rich Domain Model)
- [ ] 비즈니스 규칙이 Service가 아닌 Domain 엔티티/값객체 내부에 위치
- [ ] Anemic Domain Model(getter/setter만 있는 껍데기) 지양
- [ ] 금액/수량 계산 시 `BigDecimal` 사용 확인
- [ ] 불변 값 객체(Value Object) 적절히 활용

### 3. Port/Adapter 구조
- [ ] UseCase 인터페이스(`port.in`)가 명확히 정의됨
- [ ] 아웃바운드 포트(`port.out`) 인터페이스가 기술에 중립적인 시그니처
- [ ] Adapter가 Port 인터페이스를 implements하고 있음
- [ ] Adapter에 비즈니스 로직 없음 (순수 기술 번역만)

### 4. Anti-Skeleton 검증
- [ ] 인터페이스만 있고 구현 없는 깡통 코드 없음
- [ ] 단순 위임(Service → Repository pass-through)만 하는 의미 없는 서비스 없음
- [ ] 서비스에 정합성 검증 또는 유의미한 도메인 협업 포함

### 5. 배치 모듈 검수
- [ ] Batch 모듈에 비즈니스 연산(if/for/math) 없음
- [ ] Pipeline에서 대용량 변환 처리 (Chunk 기반)
- [ ] 어댑터에 JDBC Bulk Insert/Update 적용 여부

### 6. 테스트 품질
- [ ] Service 테스트: Port.out Mock 기반 단위 테스트
- [ ] Domain 테스트: 외부 의존 없는 순수 단위 테스트
- [ ] 테스트가 구현 로직을 실제로 검증 (형식적 통과 금지)

---

## 검수 리포트 형식

Codex 작업물 검수 시 아래 형식으로 결과를 보고한다:

```
## 검수 결과: [모듈명 / 기능명]

### 통과 항목
- ...

### 위반/문제 항목
- [심각도: 높음/중간/낮음] 파일경로:라인번호 — 문제 설명
  → 권장 수정 방향

### 종합 의견
- 전체 구조 평가 (1~2문장)
- 재작업 필요 여부
```

---

## 검수 사전 준비: WORKLOG 기반 컨텍스트 파악

검수 세션 시작 시 아래 두 파일을 **반드시** 먼저 읽어 Codex 작업 이력과 알려진 리스크를 파악한다.

| 파일 | 용도 |
|------|------|
| `CODEX_WORKLOG.md` | Codex가 세션별로 무엇을 했는지 요약 (실행 명령, 결과, 남은 리스크) |
| `WORKLOG.md` | 전체 팀의 Source of Truth — 모듈별 완료/미완료 상태, 핵심 발견 사항 |

### 파악해야 할 내용
1. **최신 세션**: WORKLOG 마지막 항목에서 가장 최근 Codex 작업 내용 확인
2. **알려진 미해결 리스크**: "남은 리스크" 항목 목록화 → 이번 검수의 우선순위로 삼음
3. **컴파일/테스트 통과 여부**: 직전 세션의 빌드 결과 확인 (실패 항목은 즉시 검수 대상)
4. **헥사고날 위반 패턴**: 기존 검수에서 반복 지적된 구조 문제 확인

### 현재 알려진 미해결 리스크 (2026-05-11 기준)
- `reconciliation`: 더미 금액(`1000.00`, `950.00`) 사용, `journal-ledger` 내부 Repository 직접 의존
- `reporting`: 실제 원장 미연동, 목업 단일 라인(`ASSET_CASH`)만 생성
- `closing`: 더미 계정 `999998`, `999999` 임시 구현 유지
- `loan`: `Loan`/`LoanContract` 병행 모델, 계정코드 하드코딩, E2E 전기 수렴 미검증
- `governance → master-data`: `effectiveDate`/`requestedVersion` 유실, `TracingService`의 JPA 직접 의존

---

## Claude 워크로그

검수 결과는 **`CLAUDE_WORKLOG.md`** 에 기록한다.

- 검수 대상 모듈/기능명
- 수행한 빌드/테스트 명령과 결과
- 발견된 헥사고날 DDD 위반 사항 (심각도 포함)
- 검수 후 남은 리스크

---

## 작업 규칙 (워크플로우)

1. **검수 시작 전**: `CODEX_WORKLOG.md`와 `WORKLOG.md` 최신 항목을 읽고 맥락을 파악한다
2. 검수 전 해당 모듈의 `README.md`와 `docs/*.md`를 먼저 확인한다
3. 요청 범위를 벗어난 리팩터링은 제안만 하고 직접 수정하지 않는다
4. 헥사고날 원칙 위반이 발견되면 파일 경로와 라인 번호를 명시한다
5. `git push`, 파괴적 삭제, 대규모 포맷 변경은 사용자 명시 없이 하지 않는다
6. 기존 문서를 대체하기보다 보강하는 방향을 우선한다
7. **검수 완료 후**: `CLAUDE_WORKLOG.md`에 결과를 기록한다

---

## 참고: 팀 역할 (`.clinerules` 기반)

| 역할 | 담당 |
|------|------|
| PM | 비즈니스 요건, 준법, API 명세 |
| DA | 논리/물리 모델링, schema.sql |
| Backend | Java/Spring Boot 구현 |
| QA | JUnit5/Mockito 단위 테스트 |
| SRE | Docker, CI/CD |

**Codex가 Backend 역할로 작업한 결과물을 Claude가 QA 관점에서 검수한다.**
