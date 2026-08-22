# Internal Audit (내부 감사) 문서 인덱스

internal-audit 모듈은 내부회계관리제도 및 위험 통제 활동(RCM, Risk Control Matrix), 설계/운영 평가(Evaluation), 결함 관리(Deficiency) 및 감사 이력을 관리하는 헥사고날 아키텍처 기반 모듈입니다.

---

## 1. 🐣 모듈 개요 및 핵심 개념

1. **RCM (Risk Control Matrix, 리스크 통제 행렬):** 
   - 회계 및 운영 프로세스별 위험 요소(Risk)와 이를 완화·통제하기 위한 통제 활동(Control)을 정의하고 관리합니다.
2. **평가 (Evaluation):**
   - RCM에 정의된 통제가 실제 프로세스에 적절히 설계되었는지(설계평가), 주기적으로 효과적으로 작동하는지(운영평가)를 평가하고 결함(Deficiency) 여부를 기록합니다.
3. **독립 로컬 런타임 (Self-Contained Local Runtime):**
   - local 프로파일을 통해 외부 PostgreSQL, Config Server, Eureka 없이 H2 인메모리 DB만으로 오프라인 단독 구동 및 테스트를 지원합니다.

---

## 2. 🏛️ 코드 지도 (Code Map)

| 계층 / 관심사 | 위치 | 역할 및 책임 |
| :--- | :--- | :--- |
| **REST / HTTP Adapter** | internal-audit:api (com.ho.account.internalaudit.api.adapter.in.web) | RcmController, EvaluationController, DTO 및 예외 처리 |
| **Composition Root** | internal-audit:api (com.ho.account.internalaudit.api) | InternalAuditApiApplication, Spring Boot 실행 진입점 |
| **Inbound Port** | internal-audit:core (com.ho.account.internalaudit.application.port.in) | RcmUseCase, EvaluationUseCase 유즈케이스 계약 |
| **Application Service** | internal-audit:core (com.ho.account.internalaudit.application.service) | RcmService, EvaluationService 업무 비즈니스 로직 |
| **Domain Entities** | internal-audit:core (com.ho.account.internalaudit.domain) | RcmControl, EvaluationResult, Deficiency 핵심 엔티티 |
| **Outbound Port** | internal-audit:core (com.ho.account.internalaudit.application.port.out) | RcmPersistencePort, EvaluationPersistencePort |
| **Persistence Adapter** | internal-audit:core (com.ho.account.internalaudit.infrastructure.adapter.out.persistence) | Spring Data JPA 리포지토리 및 엔티티 매퍼 |

---

## 3. 🧭 빠른 검증 및 실행 명령

```powershell
# 단위 및 통합 테스트 실행
.\gradlew.bat :internal-audit:core:test :internal-audit:api:test :internal-audit:api:bootJar --console=plain

# 로컬 단독 실행 (H2 In-Memory)
.\gradlew.bat :internal-audit:api:bootRun --args="--spring.profiles.active=local" --console=plain
```
