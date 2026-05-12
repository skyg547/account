# master-data 초보자 가이드 (Beginner Guide)

`master-data` 모듈은 회계 플랫폼의 기준 정보(Reference Data)를 관리하는 핵심 모듈입니다. 다른 모듈들은 계정과목, 거래처, 부서, 통화, 회계 기간, 상품 등의 일관된 코드와 명칭을 사용하기 위해 이 모듈에 의존합니다. 이 모듈은 헥사고날 아키텍처(Hexagonal Architecture)를 채택하고 있으며, 다른 모듈에서는 엔티티를 직접 참조하지 않고 항상 ID 기반 참조를 사용합니다.

## 🌟 초보자를 위한 개념 설명

* **계정과목 (Account Subjects):** 가계부를 쓸 때 '식비', '교통비'라고 나누는 기준과 같습니다. 회사의 모든 돈의 흐름을 분류하는 기준표(Chart of Accounts)입니다.
* **거래처 (Business Partners):** 우리 회사와 돈을 주고받는 모든 대상입니다. 고객, 협력사, 은행 등이 모두 포함됩니다.
* **부서 (Departments):** 돈을 쓰거나 버는 조직 단위입니다. 어느 부서에서 비용이 발생했는지 추적하기 위해 필요합니다.
* **SCD2 (Slowly Changing Dimensions):** 기준 정보가 변경될 때 기존 데이터를 덮어쓰지 않고 변경 이력을 남기는 방식입니다. 예를 들어, 거래처의 주소가 바뀌면 과거 날짜의 주소 기록과 새로운 날짜의 주소 기록을 모두 보관하여 과거 시점의 데이터를 정확히 조회할 수 있게 합니다.

## 주요 개념

- **계정과목:** 계정표와 보고서 라인을 정의합니다.
- **거래처:** 고객, 공급업체, 은행 및 기타 상대방을 나타냅니다.
- **부서:** 조직, 비용 중심점(Cost Center), 이익 중심점(Profit Center)을 나타냅니다.
- **통화 및 환율:** 다중 통화 회계 처리를 지원합니다.
- **회계 기간:** 회계 기간의 상태(열림/마감)를 제어합니다.
- **상품:** 상품 또는 서비스의 기준 정보를 제공합니다.
- **변경 요청 (Change Requests):** 민감한 기준 정보가 적용되기 전에 승인 및 감사 통제를 추가합니다.

## 현재 API 그룹 (Ports)

*Inbound 어댑터(REST Controller)를 통해 노출되는 주요 포트입니다.*

- `POST /api/basic/account-subjects`
- `GET /api/basic/account-subjects`
- `GET /api/basic/businesspartners`
- `GET /api/basic/departments`
- `POST /api/master-data/change-requests`
*(이외 다양한 조회/수정/삭제/승인 API가 존재합니다)*

## 코드 분석 순서 (헥사고날 아키텍처 기준)

전형적인 계정과목 요청을 처리할 때 다음 순서로 코드를 읽는 것을 추천합니다:

1. **Inbound Adapter:** `masterdata.api.web.AccountSubjectController` (REST API 진입점)
2. **DTO:** `masterdata.api.dto.AccountSubjectRequestDto`
3. **Command:** `masterdata.core.application.command.AccountSubjectCommand`
4. **Inbound Port:** `masterdata.core.application.port.in.AccountSubjectUseCase`
5. **Application Service:** `masterdata.core.application.service.AccountSubjectService` (유스케이스 조율)
6. **Outbound Port:** `masterdata.core.application.port.out.AccountSubjectPersistencePort`
7. **Outbound Adapter:** `masterdata.core.infrastructure.persistence.JpaAccountSubjectPersistenceAdapter`
8. **Repository:** `masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository`
9. **Domain Model:** `masterdata.core.domain.model.AccountSubject` (순수 비즈니스 로직)

거래처, 부서, 상품 등에도 동일한 패턴이 적용됩니다.

## 코드 수정 시 주의사항

- **아키텍처 경계 준수:** Controller와 DTO는 API 경계(Inbound Adapter)에만 위치해야 합니다.
- **서비스의 역할:** Application Service는 유스케이스 조율만 담당하며 비즈니스 로직은 Domain Model에 위임합니다.
- **인프라 격리:** JPA 엔티티와 Repository 세부 구현은 Infrastructure 계층(Outbound Adapter)에만 유지합니다. 다른 모듈은 JPA 엔티티를 직접 참조하지 않고 ID(예: `businessPartnerCode`)만 보관합니다.
- **이력 관리 (SCD2):** 감사 가능성(Auditability)이 필요한 도메인은 물리적 삭제(Hard Delete) 대신 유효 기간(valid_from, valid_to) 업데이트 방식(SCD2)을 선호합니다.
- **테스트:** 변경 후에는 반드시 멀티 스테이지 Docker 환경을 고려한 통합 테스트 및 `./gradlew :master-data:test`를 실행하여 검증합니다.