# Service Discovery Model

이 문서는 `account` 멀티모듈 모놀리스에서 서비스 분리 전 단계의 서비스 디스커버리 기준을 정의합니다.

## 1. 목적

- 모듈별 기능 제공자를 Spring Bean 목록 주입에만 의존하지 않고 공통 모델로 식별합니다.
- `contracts` 포트, `shared-kernel` 메타데이터, `app` 런타임 레지스트리를 같은 규칙으로 묶습니다.
- 이후 REST/gRPC/Event 기반 분리 시에도 같은 서비스명, 컨텍스트, capability를 재사용합니다.
- `gateway-service` 같은 인프라 진입점도 서비스 ID 기준으로 같은 런타임 토폴로지 안에서 관리합니다.

## 2. 핵심 모델

### `BoundedContext`

- `MASTER_DATA`
- `GOVERNANCE`
- `JOURNAL_LEDGER`
- `RECEIVABLE`
- `PAYABLE`
- `ASSET_LEASE`
- `LOAN`
- `CLOSING`
- `RECONCILIATION`
- `REPORTING`
- `TAX`
- `EXPENDITURE_RESOLUTION`

### `ServiceCapability`

- `SOURCE_DOCUMENT_LOOKUP`
- `MASTER_DATA_QUERY`
- `JOURNAL_POSTING`
- `ACCOUNTING_PERIOD_STATUS`
- `BUDGET_CONTROL`
- `ASSET_REGISTRATION`
- `LEASE_PAYMENT_RESOLUTION`
- `TAX_INVOICE_QUERY`

### `ServiceDescriptor`

- `serviceName`: 런타임과 문서에서 공통으로 쓰는 안정 식별자
- `context`: 소속 bounded context
- `capabilities`: 서비스가 제공하는 계약 기능 집합
- `description`: 사람이 읽는 설명

## 3. 등록 규칙

1. 디스커버리 대상 서비스는 `DiscoverableService`를 구현합니다.
2. 계약 포트가 이미 있으면 포트가 `DiscoverableService`를 상속합니다.
3. 런타임에서는 `app`의 `SpringServiceDiscoveryRegistry`가 모든 discoverable bean을 수집합니다.
4. 소비자는 구현체 리스트를 직접 순회하지 않고 `ServiceDiscoveryRegistry`를 통해 조회합니다.

## 4. 첫 적용 범위

현재 1차 적용 대상은 라인리지 원천문서 조회입니다.

| serviceName | context | capability | supported lineage type |
| --- | --- | --- | --- |
| `receivable-source-document-provider` | `RECEIVABLE` | `SOURCE_DOCUMENT_LOOKUP` | `O2C_AR`, `SALES`, `SALES_INVOICE` |
| `payable-source-document-provider` | `PAYABLE` | `SOURCE_DOCUMENT_LOOKUP` | `P2P_AP` |
| `asset-lease-source-document-provider` | `ASSET_LEASE` | `SOURCE_DOCUMENT_LOOKUP` | `FIXED_ASSET`, `IFRS16_LEASE` |
| `loan-source-document-provider` | `LOAN` | `SOURCE_DOCUMENT_LOOKUP` | `LOAN` |

## 5. 전환 원칙

- 신규 포트는 가능하면 capability를 하나 이상 갖는 discoverable contract로 정의합니다.
- 모듈 문서에는 구현 클래스명보다 `serviceName`과 capability를 우선 적습니다.
- 서비스 분리 이후에도 `serviceName`은 registry key, 라우팅 key, 운영 문서 key로 유지합니다.

## 6. 다음 단계

- `MasterDataQueryPort`, `JournalPostingPort`, `AccountingPeriodStatusPort`도 capability 기반으로 등록합니다.
- 운영 문서에 서비스 카탈로그와 장애 영향도 표를 추가합니다.
- 필요 시 registry를 정적 Spring Bean 검색에서 Config Server 또는 서비스 메타 저장소 기반으로 확장합니다.
- API 진입점은 `gateway-service`가 담당하고, 도메인 서비스는 Eureka 서비스 ID를 통해 게이트웨이 라우트에 연결합니다.
