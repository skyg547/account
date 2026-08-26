# Internal Audit (내부 감사) 문서 인덱스

internal-audit 모듈은 내부회계관리제도 및 위험 통제 활동(RCM, Risk Control Matrix), 설계/운영 평가(Design/Operating Evaluation), 결함 관리(Deficiency) 및 감사 이력을 관리하는 헥사고날 아키텍처 기반 모듈입니다.

---

## 1. 🐣 모듈 개요 및 핵심 개념

1. **RCM (Risk Control Matrix, 리스크 통제 행렬):**
   - 프로세스(RcmProcess)별 통제 목적, 리스크(RcmRisk), 이에 대응하는 통제 활동(ControlActivity)을 정의하고 관리합니다.
2. **평가 (Evaluation):**
   - 통제가 적절히 설계되었는지 검증하는 설계평가(DesignEvaluation)와 효과적으로 작동하는지 검증하는 운영평가(OperatingEvaluation)를 수행하고, 미비점 발생 시 결함(Deficiency)을 기록합니다.
3. **독립 로컬 런타임 (Self-Contained Local Runtime):**
   - local 프로파일을 통해 외부 PostgreSQL, Config Server, Eureka 없이 H2 인메모리 DB(MODE=PostgreSQL)만으로 오프라인 단독 구동 및 테스트를 지원합니다.

---

## 2. 🏛️ 코드 지도 (Code Map)

| 계층 / 관심사 | 위치 (Package) | 주요 클래스 및 인터페이스 | 역할 및 책임 |
| :--- | :--- | :--- | :--- |
| **REST / Inbound Web Adapter** | com.ho.account.internalaudit.api.adapter.in.web | RcmController, EvaluationController, InternalAuditApiExceptionHandler | HTTP 엔드포인트 노출, Request/Response DTO 변환 및 예외 처리 |
| **Composition Root** | com.ho.account.internalaudit.api | InternalAuditApiApplication | Spring Boot 애플리케이션 진입점 및 컴포넌트 스캔 |
| **Inbound Ports** | com.ho.account.internalaudit.core.application.port.in | RcmUseCase, EvaluationUseCase | 유즈케이스 인터페이스 계약 |
| **Application Services** | com.ho.account.internalaudit.core.application.service | RcmService, EvaluationService | 비즈니스 유즈케이스 오케스트레이션 및 트랜잭션 경계 |
| **Domain Models (RCM)** | com.ho.account.internalaudit.core.domain.rcm<br>com.ho.account.internalaudit.core.domain | RiskControlMatrix, RcmProcess, RcmRisk, ControlActivity, DesignAssessment | RCM 도메인 엔티티 및 평가 상태 |
| **Domain Models (Evaluation)** | com.ho.account.internalaudit.core.domain.evaluation | DesignEvaluation, OperatingEvaluation, Deficiency | 설계/운영 평가 및 결함 도메인 엔티티 |
| **Outbound Ports** | com.ho.account.internalaudit.core.application.port.out | RcmPersistencePort, EvaluationPersistencePort | 영속성 저장을 위한 출력 포트 계약 |
| **Persistence Adapters** | com.ho.account.internalaudit.core.infrastructure.persistence.adapter | RcmPersistenceAdapter, EvaluationPersistenceAdapter | Spring Data JPA 리포지토리를 호출하여 엔티티 매핑 및 저장 |
| **JPA Entities & Repositories** | com.ho.account.internalaudit.core.infrastructure.persistence.entity<br>com.ho.account.internalaudit.core.infrastructure.persistence.repository | RcmProcessJpaEntity, ControlActivityJpaEntity, DesignEvaluationJpaEntity, RcmProcessRepository 등 | 데이터베이스 테이블 매핑 및 JPA 데이터 액세스 |

---

## 3. 🧭 빠른 검증 및 실행 명령

`powershell
# 단위 및 통합 테스트 실행
.\gradlew.bat :internal-audit:core:test :internal-audit:api:test :internal-audit:api:bootJar --console=plain

# 로컬 단독 실행 (H2 In-Memory)
.\gradlew.bat :internal-audit:api:bootRun --args= --spring.profiles.active=local --console=plain
`
