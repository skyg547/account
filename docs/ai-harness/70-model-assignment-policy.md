# Model Assignment & Task Difficulty Policy

Assign models by capability tier, task risk, and task difficulty, not by vendor name. Project custom agent files specify reasoning effort only and inherit the available parent model.

초보자 설명: 쉬운 일(단순 오타, 정형화된 설정)은 빠른 경량 모델에 낮은 추론으로 맡기고, 복잡한 회계 계산이나 동시성 잠금 같은 위험한 일은 최상위 모델에 최고 추론(xhigh)을 주어 정밀하게 풀게 한다.

---

## 1. 작업 난이도 4단계 (Task Difficulty Tiers)

저장소의 모든 실행 가능한 GitHub Issue는 아래 4단계 난이도 중 **정확히 1개의 라벨**(`difficulty:*`)을 부여받아야 합니다.

| 난이도 | 라벨 | 대상 작업 성격 | 권장 모델 티어 | Codex `model_reasoning_effort` | Claude / 타 도구 권장 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **하 (Low)** | `difficulty:low` | 단순 기계적 치환, 문서/오탈자 수정, 주석 보강, 정형화된 YAML 설정 미러링, 단일 상수 변경 | **Fast / Low-Cost** | `low` | **Gemini 3.8 Flash (`thinking: low`)**, Claude Haiku, GPT-4o-mini |
| **중 (Medium)** | `difficulty:medium` | 단일 모듈 내 경계가 명확한 비즈니스 로직, Remote REST Outbound Port Adapter 연동, 단위/통합 테스트 보강, DTO 매핑 | **Balanced** | `medium` (또는 기본 `high`) | **Gemini 3.8 Flash (`thinking: middle`)**, Claude Sonnet, GPT-5 / GPT-6 Astra |
| **상 (High)** | `difficulty:high` | 멀티 모듈 공통 계약(`contracts`, `shared-kernel`), 헥사고날 아키텍처 리팩토링, DB 마이그레이션(Flyway) 및 권한 분리, 보안 정책(JWT, HttpOnly BFF, Vault 시크릿 관리), 멀티 컨테이너 Compose 오버레이 | **Frontier / Pro** | `high` | **Gemini 3.8 Flash (`thinking: high`)**, Claude 3.5 Sonnet, GPT-6 Astra |
| **최상 (Very-High)** | `difficulty:very-high` | 금융 정밀도(`BigDecimal` 반올림/스케일, 외환 환율, 복합 이자/상각 계산), 분산 락 및 원장 마감(Closing) 동시성 제어, 다중 트랜잭션 멱등성 가드(Idempotency Guard), 감사 로그 불변성(Append-only), IFRS9 대손충당금(ECL) 스테이지 전이, 원장 정합성/대사 엔진 | **Frontier SOTA / Deep Reasoning** | **`xhigh`** | GPT-6 Astra (`xhigh`), Claude Opus, Gemini Deep Reasoning (xhigh) |

---

## 2. 난이도별 상세 기준 및 판단 가이드

### 2-1. 하 (Low — `difficulty:low`)
- **특징**: 판단(decision)이 거의 필요 없거나 이미 100% 결정된 작업.
- **적용 대상**:
  - `docs/**/*.md` 문서 작성, 오탈자 및 주석 보강.
  - 동일한 패턴의 기계적 치환 (예: deprecation 메서드 교체, 단순 패키지 경로 정리).
  - 기존 설정 파일을 그대로 복제·미러링하는 정형화된 YAML 생성 (`config-repo/*.yml`).
  - 단일 상수, 타임아웃, 단순 파라미터 값 조정.
- **실패 영향**: 매우 낮음 (revert 커밋 1개로 즉시 롤백 가능).

### 2-2. 중 (Medium — `difficulty:medium`)
- **특징**: 도메인 전체에 파급력이 없고, 단일 마이크로서비스 내부에서 해결 가능한 표준 개발 작업.
- **적용 대상**:
  - 단일 모듈의 Inbound Controller / Outbound Port Adapter 구현 (예: RestClient 기반 HTTP 연동).
  - 모듈별 단위 테스트(`*Test.java`) 및 슬라이스 테스트 보강.
  - 기존 JPA Entity 필드 추가 및 단순 DTO 변환 매퍼 작성.
  - 프론트엔드 컴포넌트 이벤트 바인딩 및 기본 화면 전환 피드백 개선.
- **실패 영향**: 모듈 단위 테스트 및 격리 검증으로 즉시 발견 가능.

### 2-3. 상 (High — `difficulty:high`)
- **특징**: 여러 모듈 간의 통신/공유 계약에 영향을 주거나, 인프라·보안·데이터 영속성 레이어의 상태가 변경되는 작업.
- **적용 대상**:
  - `contracts` 및 `shared-kernel` 모듈의 공통 DTO, Enum, 이벤트 스키마 변경.
  - PostgreSQL Flyway 스키마 마이그레이션 (`V*__*.sql`) 작성 및 `_owner`/`_app` DB 런타임 권한 분리.
  - Spring Cloud Gateway 라우트 필터, JWT 검증 로직, BFF rate-limit 신뢰 경계 분리, Spring Cloud Vault 하이브리드 구성.
  - 마이크로서비스 간 통합을 위한 Docker Compose / Podman 네트워크 및 멀티 프로파일 오버레이 구성.
- **실패 영향**: 다중 서비스 기동 실패 또는 런타임 보안/통신 장애 유발 가능.

### 2-4. 최상 (Very-High — `difficulty:very-high`)
- **특징**: 단 한 줄의 오차나 레이스 컨디션도 용납되지 않는 금융 시스템의 핵심 도메인 규칙 및 원장 무결성 작업.
- **적용 대상**:
  - **금융 정밀도**: `BigDecimal` 소수점 스케일 및 반올림(RoundingMode) 계약, 통화 간 환율 적용, 일할 복리 이자 계산, 리스 상각 스케줄.
  - **동시성 및 락**: 일계표/월마감(Ledger Closing) 시 비관적/낙관적 락, 분산 락, 계좌 잔액 갱신 경합 처리.
  - **멱등성 보장 (Idempotency Guard)**: 반제(UnsettledItem) 중복 반영 방지, 결제/출금 재시도 시 원장 중복 분개 방지, Outbox 릴레이 중복 발행 방지.
  - **감사 및 규제 준수**: Internal Audit 로그의 Append-only 불변성 보장, IFRS9 대손충당금(ECL) 스테이지 전이 및 신용위험 평가.
- **실패 영향**: 회계 장부 불일치, 이중 출금/지급, 데이터 영구 왜곡 등 치명적 금융 사고로 직결.

---

## 3. 추론 강도(Reasoning Effort) 관리 및 할당 정책

### 3-1. 기본값 유지 원칙 (API Quota Protection)
- **Codex 기본 설정 권고**: `model_reasoning_effort = "high"` (또는 `"medium"`)
- `xhigh` 추론은 턴당 수만 토큰의 내부 추론(Thinking)을 소비하므로, 일상적인 설정/중간 난이도 작업에 상시 적용하면 급격한 Rate Limit 및 비용 고갈을 유발합니다.
- 따라서 **상시 기본값은 `high`로 유지**하며, `difficulty:very-high` 라벨이 명시된 태스크를 수행할 때에만 해당 세션 또는 프로젝트 설정에서 선별적으로 `xhigh`를 활성화합니다.

### 3-2. 에스컬레이션 (Escalation) 규칙
아래 조건 중 하나라도 감지되면 원래 난이도보다 즉시 상위 등급으로 승격해야 합니다:
1. `difficulty:medium` 작업 중 `BigDecimal`, 통화 환산, 금액 반올림 규칙이 포함된 경우 ➔ `difficulty:very-high`로 승격.
2. 단일 모듈 작업 중 `contracts`나 Gateway 공용 라우트 변경이 불가피해진 경우 ➔ `difficulty:high`로 승격.
3. 배송/지급/상각 로직에서 재시도 및 중복 방지 처리가 필요한 경우 ➔ `difficulty:very-high`로 승격.

### 3-3. 디에스컬레이션 (De-escalation) 규칙
- 원래 `difficulty:high` 성격의 문제라도, **Spec Author(상위 모델)가 `spec-driven`으로 근본 원인 분석, 파일 단위 작업 지시, allowlist, 검증 명령을 완벽히 명시한 경우**, Implementer는 `difficulty:medium` (Balanced tier) 수준에서 안전하게 지시를 기계적으로 실행할 수 있습니다.
- 단, `difficulty:very-high`의 금융 정밀도와 동시성 로직은 스펙이 명시되어 있더라도 검증 및 구현 시 반드시 최고 수준의 추론 모델과 리뷰어 게이트를 거쳐야 합니다.

### 3-4. 연속성 보장형 추론 강도 배정 및 분담 원칙
- **최상 (Very-High) 난이도**: **Codex (`reasoning: xhigh`)** 전담.
  - 금융 정밀도(`BigDecimal`), 원장 마감 동시성 락, IFRS9 대손충당금 등 극한의 수학적·회계적 무결성이 요구되는 핵심 도메인에 `xhigh` 최고 추론을 집중 투입합니다.
- **상 (High), 중 (Medium), 하 (Low) 난이도**: **Codex (`reasoning: high`)** 및 **Gemini 3.8 Flash** 유연 지원.
  - 작업 간 컨텍스트 연속성(Continuity)이 끊기는 것을 방지하고 빠른 파이프라인 흐름을 유지하기 위해, Codex 자동 러너 실행 시에는 `very-high`를 제외한 모든 작업(High/Medium/Low)을 `high` 추론 강도로 연속 수행합니다.
  - 상대적으로 쉬운 작업은 내부 추론 토큰 소진량이 적으므로 `high` 강도에서도 쿼터에 큰 부담 없이 정확하고 신속하게 기계적 완료가 가능합니다.
  - Gemini 3.8 Flash는 빠른 설정 반영, 독립 코드 리뷰, 병렬 탐색 및 프론트엔드/인프라 오버레이 작업에 상시 투입됩니다.

### 3-5. 동적 모델 티어링 및 서킷 브레이커 (Dynamic Model Tiering & Circuit Breaker)
- **Codex 러너 모델 티어링 (`scripts/codex-runner.py`)**:
  - **기본 모델 (Default)**: `gpt-5.6-sol` (`model_reasoning_effort = "high"`)
    - 일반 기능 구현, 단위/통합 테스트 작성, 문서 갱신 등 대다수 표준 작업(`difficulty:low`, `difficulty:medium`, `difficulty:high`)에 기본 적용하여 불필요한 토큰 번(Burn)을 방지합니다.
  - **고난도 승격 (Escalation)**: `gpt-6-astra` (`model_reasoning_effort = "xhigh"`)
    - `difficulty:very-high` 또는 `model:astra` 라벨이 부여된 금융 정밀도, 원장 마감 동시성, 멱등성 가드, 보안 정책 이슈에 한해 선별적으로 승격 실행합니다.
- **서킷 브레이커 (Circuit Breaker) & 토큰 가드**:
  1. **사용량 한도(Usage Limit) 즉시 탈출**: 러너 실행 중 OpenAI `usage limit` 또는 `rate limit` 감지 시 무한 루프 재시도를 즉시 중단하고 프로세스를 안전하게 종료(`sys.exit(1)`)합니다.
  2. **연속 실패 차단 (Max Consecutive Failures)**: 동일 이슈에서 2회 연속 에러(`returncode != 0`) 발생 시 해당 이슈를 즉시 `status:blocked`로 전환하고 원인을 댓글에 기록하여 무한 루프로 인한 쿼터 소진을 원천 차단합니다.
