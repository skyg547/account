# 📂 Master Data Service (기준 정보 관리)

`master-data` 모듈은 전체 시스템의 '단일 진실의 원천(Single Source of Truth)'입니다. 모든 모듈이 공유하는 계정과목, 거래처, 부서, 통화 등의 핵심 데이터를 관리하며, 이력 보존(SCD2)과 변경 승인 프로세스를 통해 데이터의 신뢰성을 보장합니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

회계 시스템에서 데이터가 엉키지 않으려면 모두가 똑같은 이름표를 써야 합니다.
1. **계정과목 (Account):** 돈의 용도 분류 (예: "식비", "비품비").
2. **거래처 (Partner):** 돈을 주고받는 대상 (예: "A사", "B은행").
3. **부서 (Department):** 돈을 쓰고 버는 주체 (예: "영업팀", "개발팀").
4. **SCD2 (이력 관리):** 데이터를 수정할 때 덮어쓰지 않고, "어제까지의 기록"과 "오늘부터의 새 기록"을 모두 남기는 방식입니다. 이를 통해 1년 전 전표를 볼 때 '당시의 부서명'을 정확히 알 수 있습니다.

---

## 2. 🔄 프로세스 흐름 (Process Flow)

### 📌 SCD2 기준정보 변경 및 승인 흐름
중요 기준 정보는 승인을 거쳐 반영되며, 기존 이력을 보존하면서 새로운 버전을 생성합니다.

```mermaid
sequenceDiagram
    participant Requester as 작성자
    participant Governance as 승인 모듈
    participant Master as MasterDataService
    participant DB as 데이터베이스

    Requester->>Master: 기준정보 변경 요청 + requestedVersion
    Master->>Master: 지원 전략과 현재 SCD2 버전 확인
    Master->>DB: 변경 요청 저장 (REQUESTED)
    Governance->>Master: 승인 처리
    Master->>Master: 버전 재확인
    Master->>DB: 승인 상태 저장 (APPROVED)

    rect rgb(240, 240, 240)
        Note over Master, DB: SCD2 반영 프로세스 (Apply)
        Master->>DB: 요청 행 잠금과 버전 재확인
        Master->>DB: 기존 활성 데이터 종료 (validTo 업데이트)
        Master->>DB: 신규 버전 데이터 삽입 (validFrom = approved effectiveDate)
        Master->>DB: 실제 반영 성공 후 APPLIED
    end

    Master-->>Requester: 최종 반영 완료
```

### 📌 타 모듈과의 연동 (ID 기반 참조)
다른 모듈은 `master-data`의 복잡한 엔티티를 직접 가져가지 않고, **코드(String ID)**만 저장한 뒤 필요할 때 API(Port)로 조회합니다.

```mermaid
flowchart LR
    A[Voucher / Loan / Tax] -- "String Code 저장" --> B{MasterDataQueryPort}
    B -- "SCD2 시점 조회" --> C[Master Data Service]
    C -- "Name, Attribute 반환" --> A
```

---

## 3. 📊 데이터 모델 (Schema)

SCD2 관리를 위해 모든 테이블은 유효 기간 필드를 포함합니다.

```mermaid
erDiagram
    ACCOUNT_SUBJECTS ||--o| ACCOUNT_SUBJECTS : "parent_code"
    BUSINESS_PARTNERS ||--o{ BUSINESS_PARTNER_ACCOUNTS : "has"
    CURRENCIES ||--o{ EXCHANGE_RATES : "from/to"

    ACCOUNT_SUBJECTS {
        Long id PK "기술 키"
        String code "업무 식별자"
        String name
        LocalDate valid_from "SCD2 시작"
        LocalDate valid_to "SCD2 종료"
    }
    
    BUSINESS_PARTNERS {
        Long id PK "대리키"
        String business_partner_code "업무 식별자"
        String name
        LocalDate valid_from
        LocalDate valid_to
    }
```

---

## 4. 🧭 실행 및 연동 방법

상세 문서는 [docs/README.md](./docs/README.md)에서 `beginner-guide`, `process-flow`, `schema`, `local-run` 순서로 확인합니다.

**IntelliJ 실행 순서:**
1. `Config Server bootRun`을 먼저 실행합니다.
2. Eureka 등록까지 확인하려면 `Discovery bootRun`을 실행합니다.
3. `Master Data bootRun`을 실행합니다.

**PowerShell 검증 명령:**
```powershell
.\gradlew :master-data:core:test :master-data:api:test :master-data:batch:test --console=plain --max-workers=1 --no-daemon
```

**API 로컬 실행 명령:**
```powershell
.\gradlew :master-data:api:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

**일일 유효성 Batch 실행 명령:**
```powershell
.\gradlew :master-data:batch:bootRun --args="--spring.profiles.active=local --spring.batch.job.enabled=true --spring.batch.job.name=masterDataValidityJob asOfDate=2026-07-29 --spring.cloud.config.enabled=false --spring.config.on-not-found=ignore --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1 --no-daemon
```

`spring.config.on-not-found=ignore`는 Config Server를 의도적으로 끈 이 학습용 H2
명령에서만 사용합니다. 기본 Batch 설정은 Config Server와 공통 datasource 계약을
받지 못하면 시작을 중단하므로, 운영 실행에 이 예외 플래그를 복사하면 안 됩니다.

**연동 주의사항:**
- 다른 모듈에서 마스터 데이터를 조회할 때는 반드시 `contracts`의 `MasterDataQueryPort`를 사용하세요.
- 데이터 수정 시 `terminate()` 메서드를 호출하여 SCD2 정책을 준수해야 합니다.
- 승인된 변경 요청은 targetType별 applier가 실제 SCD2 반영을 수행한 뒤에만 `APPLIED`가 됩니다. 현재 `ACCOUNT_SUBJECT`, `BUSINESS_PARTNER`, `DEPARTMENT`, `PRODUCT` typed applier가 구현되어 있습니다.
- `requestedVersion`은 CREATE=1, UPDATE=현재 저장 이력 수+1, DEACTIVATE=현재 저장 이력 수입니다. 요청·승인·반영 직전에 반복 검증하므로 대기 중 다른 버전이 먼저 반영되면 오래된 요청은 실패합니다.
- `DEACTIVATE`는 JSON payload 없이 실행되며 승인된 `effectiveDate`를 SCD2 종료일로 사용합니다. `CURRENCY`, `EXCHANGE_RATE`, `FISCAL_PERIOD`는 typed applier와 버전 어댑터가 생기기 전까지 접수 단계에서 fail-closed 됩니다.
- 승인/반려/반영은 변경 요청 행을 잠그고 `lockVersion`으로 동시 갱신도 감지합니다. 예약 반영은 `APPROVED` 상태와 `effectiveDate` 조건으로 최대 500건을 조회합니다.
- Governance 승인 ID는 `sourceReference`로 저장해 동일 명령 재시도는 기존 요청을 반환하고 다른 명령 재사용은 충돌로 차단합니다. 실제 반영 완료 시각은 `appliedAt`에 기록합니다.
- 직접 쓰기 CRUD API도 아직 공존하므로 운영에서는 관리자 보정 전용으로 제한하거나 일반 변경을 승인 API로 통합해야 합니다.
- 일일 유효성 보고서는 core pipeline이 기준일을 필수로 받고, JPA 통계 어댑터가 네 테이블의 활성 건수를 DB `COUNT`로 계산합니다.
- 계정과목/상품 활성 목록과 거래처 이름 검색도 전체 행을 Java에서 필터링하지 않고 기준일 조건을 DB query에 전달합니다. 거래처 검색은 빈 검색어를 거부하고 현재 활성 버전만 반환합니다.
- 환율 조회는 요청일 이하의 데이터 중 가장 최근 `effectiveDate` 1건을 DB에서 선택합니다. 환율은 양수이고 통화 코드는 3자리 ISO 형식이어야 합니다.
- Closing이 회계기간 상태를 바꿀 때는 `FiscalPeriodPersistencePort` 뒤에서 대상 행을 잠그고, 영구 마감 불변식과 감사 사용자를 `FiscalPeriod` 도메인 메서드가 검증합니다.
- 운영에서는 `config-repo/master-data.yml`의 `ddl-auto: update`를 그대로 쓰지 말고 Flyway 기준으로 검증해야 합니다.
- 현재 Loan core의 거래처/통화 엔티티 연관과 Closing Batch의 환율 Repository 직접 참조는 위 contracts 원칙의 예외입니다. 다음 순차 리팩터링에서 코드/ID 저장과 소비 모듈 소유 포트 + contracts 조회로 분리해야 합니다.
