# 📂 Master Data Service (기준 정보 관리)

`master-data` 모듈은 전체 시스템의 '단일 진실의 원천(Single Source of Truth)'입니다. 모든 모듈이 공유하는 계정과목, 거래처, 부서, 통화 등의 핵심 데이터를 관리하며, 이력 보존(SCD2)과 변경 승인 프로세스를 통해 데이터의 신뢰성을 보장합니다.

### 거래처 도메인과 저장 모델의 경계

- `BusinessPartner`와 `BusinessPartnerAccount`는 JPA를 모르는 순수 도메인 객체입니다. 생성 팩터리가 필수 코드·이름, 유효기간, 계좌의 대표 여부 같은 업무 불변식을 먼저 검사합니다.
- `BusinessPartnerJpaEntity`와 `BusinessPartnerAccountJpaEntity`만 테이블·컬럼·연관관계를 알고, `JpaBusinessPartnerPersistenceAdapter`가 두 모델을 명시적으로 변환합니다. 따라서 도메인 규칙을 DB 프록시 생명주기와 분리해 단위 테스트할 수 있습니다.
- SCD2 수정의 `closeVersion()`과 업무 종료의 `terminate()`는 모두 종료일만 닫고, 당시의 `useYn`은 과거 조회를 위해 보존합니다. 종료일 다음 날부터는 기간 조건으로 조회되지 않습니다. 현재 활성 조회는 기간과 `useYn=true`를 확인하지만 KYC 상태·위험 등급에 따른 심사 적격성까지 판단하지는 않습니다.
- 새 SCD2 버전은 기존 계좌 값을 새 자식 행으로 복제합니다. 자식 ID는 재사용하지 않아 과거 버전의 FK와 현재 지급 계좌를 모두 보존합니다.
- HTTP 응답은 `BusinessPartnerDto`가 조립하며 사업자등록번호 마스킹도 API 경계에서 수행합니다. 영속성 엔티티나 민감한 원문을 Controller가 직접 반환하지 않습니다.
- 기존 `business_partners`, `business_partner_accounts` 테이블·컬럼과 API 경로는 유지되므로 이번 분리만을 위한 DB migration은 없습니다.

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
        Master->>DB: 업무 키 잠금 → 요청 행 잠금 → 버전 재확인
        Master->>DB: 기존 활성 데이터 종료 (validTo 업데이트)
        Master->>DB: 신규 버전 데이터 삽입 (validFrom = approved effectiveDate)
        Master->>DB: 실제 반영 성공 후 APPLIED
    end

    Master-->>Requester: 최종 반영 완료
```

### 📌 타 모듈과의 연동 (코드·ID 참조)
소비 모듈은 필요한 코드 또는 ID를 저장하고 자신의 조회 포트를 통해 기준일의 속성을 확인합니다. 예를 들어 Loan은 거래처 ID와 통화 코드를 저장합니다.

```mermaid
flowchart LR
    A[업무 모듈] -- "코드 또는 ID 보관" --> B{소비 모듈 조회 포트}
    B -- "지원되는 계약으로 기준일 조회" --> C[Master Data]
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

## 거래처 screening과 사용 가능 여부 (Issue #923)

`CUSTOMER`와 `VENDOR`의 거래처 screening은 [입력·결정·eligibility 흐름](docs/process-flow.md#거래처-screening과-eligibility-issue-923)을 따릅니다. 증빙 제출에는 인증된 기존 기준정보 관리자 역할(`ROLE_PARTNER_MANAGER` 등)이 필요하고, 결정은 제출자와 다른 인증된 `SCREENING_REVIEWER`가 합니다. 관리자 역할이 없는 `ROLE_USER`의 재제출은 403으로 거부되어 기존 `APPROVED` 상태를 `PENDING`으로 바꿀 수 없습니다. `GET /api/basic/businesspartners/{businessPartnerCode}/eligibility?asOfDate=yyyy-MM-dd`는 해당 날짜까지의 마지막 screening 감사 상태와 그날 유효한 거래처 버전으로 판단하고, 식별번호나 증빙을 노출하지 않고 `eligible`만 반환합니다. 일반 거래처 DTO의 기존 KYC/risk 표시는 이 결과가 아닙니다.

현재 정책은 KR 사업자등록번호 10자리의 **형식**과 거래처 등록번호 일치만 검사합니다. 발급 사실을 증명하지 않으며, 다른 관할권·식별자 유형의 승인된 정책은 제공되지 않았습니다. DB 경계와 H2 차이는 [schema](docs/schema.md#거래처-screening-저장-issue-923), 요청 예시와 검증 명령은 [local-run](docs/local-run.md#거래처-screening-확인-issue-923)을 참조하세요. 공용 계약 및 실제 payable/receivable 소비자 연동은 이번 범위 밖입니다.

## 4. 🧭 실행 및 연동 방법

상세 문서는 [docs/README.md](./docs/README.md)에서 `beginner-guide`, `process-flow`, `schema`, `local-run` 순서로 확인합니다.

**IntelliJ 실행 순서:**
1. 로컬 단독 실행은 `Master Data bootRun`을 바로 실행합니다.
2. 통합 모드에서만 Config Server와 Discovery를 먼저 실행합니다.

**PowerShell 검증 명령:**
```powershell
.\gradlew :master-data:core:test :master-data:api:test :master-data:batch:test --console=plain --max-workers=1 --no-daemon
```

**API 로컬 실행 명령:**
```powershell
.\gradlew :master-data:api:bootRun --console=plain --max-workers=1
```

**일일 유효성 Batch 실행 명령:**
```powershell
.\gradlew :master-data:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=masterDataValidityJob asOfDate=2026-07-29" --console=plain --max-workers=1 --no-daemon
```

프로파일을 생략하면 `local`이 선택되어 H2 PostgreSQL mode와 Flyway V1-V8 (H2 target=8, 반려 사유 반복 migration 및 H2 전용 실패 상태 migration 포함), JPA validate를
사용합니다. `dev`/`prod`는 주입된 PostgreSQL 접속정보를 사용하고 애플리케이션 Flyway와
SQL/Batch schema 자동 생성을 끕니다. 배포 migration은 별도 `migration-runner`가 수행합니다.
prod PostgreSQL은 `sslmode=verify-full`을 startup에서 검증합니다. 외부 서비스의 SCD2 조회는
`/api/basic/references/**?effectiveDate=yyyy-MM-dd`와 read-only Fiscal Period API를 사용하며,
회계기간 상태 변경은 Closing 승인/gate를 우회하는 raw endpoint로 제공하지 않습니다.

**연동 주의사항:**
- 계정과목 이름만 수정할 때는 관리자 권한의 `PUT /api/basic/account-subjects/{code}`에 `{"name":"새 이름"}`을 보냅니다. 생략한 업무 필드(비영업 `accountType`, `regulatoryMappingCode`, category, 정산·고정자산 플래그 등)는 현재 버전에서 새 SCD2 버전으로 이어집니다. `unsettled`/`fixedAsset`를 명시적으로 `false`로 보내면 그 값으로 변경합니다.
- `accountType` 또는 `regulatoryMappingCode`의 의도적인 변경과 매핑 해제는 승인 UPDATE 요청의 `payloadJson`에 각각 값을 넣거나 `"clearRegulatoryMappingCode":true`를 넣어 처리합니다. 승인 요청에는 이름과 시행일을 포함하고 요청자·승인자·사유·원문 payload·반영 시각이 변경 요청에 남습니다. 직접 PUT에서 이 세 필드를 보내면 HTTP 400으로 거부합니다. 분류 유형과 category가 맞지 않거나 매핑 값과 해제 플래그를 동시에 지정하면 접수·승인 전에 거부합니다. 명시적 `null`은 생략으로 취급하지 않고 거부하며, CREATE에서 매핑 해제 플래그도 거부합니다.
- 다른 모듈에서 마스터 데이터를 조회할 때는 반드시 `contracts`의 `MasterDataQueryPort`를 사용하세요.
- SCD2 수정은 이전 버전에 `closeVersion()`을 호출하고 새 버전을 저장합니다. 업무상 비활성화에만 `terminate()`를 사용합니다.
- 계정과목·부서·상품·거래처의 `CREATE`는 신규 업무 키에만 사용합니다. 오늘 활성 행이 없어도 미래 예약 또는 종료된 이력이 하나라도 있으면 같은 키의 직접 생성과 승인 반영을 HTTP 409 충돌로 거부합니다. 날짜가 서로 닿거나 완전히 떨어져 있어도 키 재생성은 허용하지 않습니다. 현재 활성 버전이 있는 키의 변경은 SCD2 `UPDATE`를 사용하며, 종료 이력만 있거나 미래 버전만 있는 키의 재활성화는 현재 지원하지 않습니다.
- 승인된 변경 요청은 targetType별 applier가 실제 SCD2 반영을 수행한 뒤에만 `APPLIED`가 됩니다. 현재 `ACCOUNT_SUBJECT`, `BUSINESS_PARTNER`, `DEPARTMENT`, `PRODUCT` typed applier가 구현되어 있습니다.
- `requestedVersion`은 CREATE=1, UPDATE=현재 저장 이력 수+1, DEACTIVATE=현재 저장 이력 수입니다. 요청·승인·반영 직전에 반복 검증하므로 대기 중 다른 버전이 먼저 반영되면 오래된 요청은 실패합니다.
- `DEACTIVATE`는 JSON payload 없이 실행되며 승인된 `effectiveDate`를 SCD2 종료일로 사용합니다. `CURRENCY`, `EXCHANGE_RATE`, `FISCAL_PERIOD`는 typed applier와 버전 어댑터가 생기기 전까지 접수 단계에서 fail-closed 됩니다.
- 요청/승인/반영과 네 유형의 직접 쓰기는 `(targetType, targetKey)` 잠금을 트랜잭션 종료까지 공유합니다. 승인/반영은 그 뒤 요청 행을 잠그고 최신 버전을 다시 확인하며 `lockVersion`도 유지합니다. 예약 반영은 `APPROVED` 상태와 `effectiveDate` 조건으로 최대 500건을 조회하고 요청별 독립 트랜잭션으로 처리합니다. 실패 건은 안전한 분류·시각과 함께 `FAILED_*`로 격리해 다음 실행에서 다시 선택하지 않습니다.
- `core` 변경 요청 유즈케이스에 전달된 Governance 승인 ID는 `sourceReference`로 저장합니다. PostgreSQL에서는 동시 최초 접수도 DB의 원자적 생성·기존 행 조회를 거쳐 동일한 요청 ID를 반환하고, 다른 명령의 식별자 재사용은 충돌 예외로 차단합니다. 현재 HTTP 생성 DTO는 `sourceReference`를 받지 않으므로 이 접수 멱등성은 HTTP POST에는 적용되지 않습니다. 검증 실패 시 새 요청 행도 롤백됩니다. 이미 `APPLIED`인 요청 ID의 반영 재시도는 SCD2 이력과 `appliedAt`을 바꾸지 않고 기존 결과를 반환합니다.
- 계정과목/부서/거래처/상품의 직접 쓰기(POST/PUT/DELETE) API는 서비스의 인증 필터에서 설정한 `AUTH_JWT_AUDIENCE`(기본 `account-api`) audience의 Auth 서명 JWT를 재검증한 뒤 `MasterDataDirectWritePolicy`를 통해 승인된 관리자 역할(`ROLE_ADMIN`, `ROLE_SYSTEM_ADMIN`, `ROLE_MASTER_MANAGER`, `ROLE_ACCOUNTING_ADMIN`, `ROLE_PARTNER_MANAGER`)로 제한됩니다. 자격증명이 없거나 위조된 요청은 HTTP 401, 검증된 비관리자는 HTTP 403입니다. Gateway의 `X-Auth-*` 전달 값은 JWT claim과 일치해야 합니다.
- 변경 요청의 조회/승인/반려/반영도 같은 수신 서비스 검증을 거칩니다. 내부 회계기간 변경은 별도 서명키와 `master-data-internal` audience의 Closing assertion을 요구하며, `audit_user`에는 검증된 `closing:<delegated actor>`를 기록합니다. 상세 입력과 통합 제약은 [docs/process-flow.md](docs/process-flow.md)에 있습니다.
- 일일 유효성 보고서는 core pipeline이 기준일을 필수로 받고, JPA 통계 어댑터가 네 테이블의 활성 건수를 DB `COUNT`로 계산합니다.
- 계정과목/상품 활성 목록과 거래처 이름 검색도 전체 행을 Java에서 필터링하지 않고 기준일 조건을 DB query에 전달합니다. 거래처 검색은 빈 검색어를 거부하고 현재 활성 버전만 반환합니다.
- 거래처 `GET /api/basic/businesspartners`, `/active`, `/search?name=...`와 변경 요청 `GET /api/master-data/change-requests/pending`은 0부터 시작하는 `page`와 `size`를 받습니다. 기본 크기는 20, 최대 크기는 100이며 잘못된 범위는 HTTP 400입니다. JSON 본문은 기존 배열 형식을 유지하고 응답 헤더 `X-Page`, `X-Page-Size`, `X-Has-Next`가 다음 페이지 여부를 알려줍니다. 전체 결과가 필요한 소비자는 `X-Has-Next=true`인 동안 `page`를 1씩 올려 조회해야 합니다. 거래처 목록은 계좌를 포함하지 않는 scalar 요약만 DB에서 조회합니다.
- 환율 조회는 요청일 이하의 데이터 중 가장 최근 `effectiveDate` 1건을 DB에서 선택합니다. 환율은 양수이고 통화 코드는 3자리 ISO 형식이어야 합니다.
- Closing이 회계기간 상태를 바꿀 때는 `FiscalPeriodPersistencePort` 뒤에서 대상 행을 잠그고, 영구 마감 불변식과 감사 사용자를 `FiscalPeriod` 도메인 메서드가 검증합니다.
- 운영에서는 애플리케이션 `ddl-auto`를 `validate`로 유지하고 migration-runner가 검증한 Flyway 기준만 사용합니다.
- Loan은 거래처 ID·통화 코드를 저장하고 `LoanReferenceDataPort`로 참조를 확인합니다. 모놀리스 어댑터는 Master Data 영속성 포트를 사용하며, dev 원격 어댑터의 숫자 거래처 ID·기준일 통화 조회는 제공 API가 없어 현재 실패합니다. Closing 환율 조회는 `FxExchangeRateLookupPort` 뒤의 contracts 어댑터(모놀리스) 또는 JDBC 어댑터(dev)를 사용합니다.

### 업무 키별 동시 쓰기와 DB 기간 제약 (GH-753)

아직 이력이 없는 키도 잠금 테이블의 정확한 `(유형, 코드)` 행으로 직렬화합니다. 다른 키는
독립적으로 처리됩니다. 버전 확인, SCD2 저장, `APPLIED` 저장은 같은 트랜잭션이며 실패하면
모두 롤백됩니다. PostgreSQL V9는 네 지원 유형의 **모든 이력**에 날짜 범위 exclusion을
적용합니다. 종료일도 유효일이므로 같은 날짜를 공유하는 두 행은 겹칩니다.

`./gradlew :master-data:test`는 core/API/Batch 테스트를 함께 실행합니다. 실제 PostgreSQL
경쟁 검증은 [local-run](docs/local-run.md#postgresql-scd2-회귀-검증)의 별도 환경이 필요합니다.
H2는 V8까지만 적용하므로 PostgreSQL exclusion의 증거가 아닙니다.
기간 충돌409, 기존 데이터 보정 절차와 롤백 제약은 [schema](docs/schema.md#업무-키-잠금과-기간-제약-gh-753)를 참조하세요.

### 상품 가격 저장 규칙 (GH-912)

상품 가격은 직접 API, 승인 변경 요청, core 유즈케이스에서 동일하게 검사합니다. 필수 가격은
0 이상이고 `NUMERIC(19,4)`에 반올림 없이 저장할 수 있어야 합니다. 최대값은
`999999999999999.9999`이며 `1.23456`과 이 범위를 넘는 값은 거부합니다. `1.230000`처럼
추가 자리가 0뿐인 값은 허용하고 소수 4자리로 정규화합니다. 승인 UPDATE에서 가격을
생략하면 현재 버전 가격을 유지하며, 유효하지 않은 새 가격은 이전 버전 종료 전에 거부합니다.
PostgreSQL V10은 음수 가격을 DB에서도 차단합니다. 실제 저장·재조회와 migration 검증 방법은
[local-run](docs/local-run.md#postgresql-상품-가격-회귀-검증)을 참조하세요.
