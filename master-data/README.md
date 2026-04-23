# Master Data Service

`master-data` owns the reference data used by accounting and finance modules: account subjects, business partners, departments, currencies, exchange rates, fiscal periods, and products.

The module follows DDD and hexagonal architecture. API DTOs, use cases, domain rules, output ports, and persistence adapters are separated so the business core does not depend directly on Spring Data repositories or database details.

## Architecture

Request flow:

```text
Controller -> UseCase -> Application Service -> Output Port -> JPA Adapter -> Repository -> DB
```

Package responsibilities:

- `com.ho.account.masterdata.api.web`: REST controllers and inbound HTTP adapters.
- `com.ho.account.masterdata.api.dto`: request and response DTOs for the API boundary.
- `com.ho.account.masterdata.core.application.command`: use-case input commands converted from DTOs.
- `com.ho.account.masterdata.core.application.port.in`: input ports called by controllers.
- `com.ho.account.masterdata.core.application.pipeline`: batch-specific transformation/report pipelines.
- `com.ho.account.masterdata.core.application.service`: transaction boundary and use-case orchestration.
- `com.ho.account.masterdata.core.domain.model`: master-data domain entities and state rules.
- `com.ho.account.masterdata.core.domain.changerequest`: change request aggregate and approval state rules.
- `com.ho.account.masterdata.core.domain.policy`: shared domain policies such as validity periods.
- `com.ho.account.masterdata.core.application.port.out`: technology-independent output ports.
- `com.ho.account.masterdata.core.infrastructure.persistence`: JPA output adapters.
- `com.ho.account.masterdata.core.infrastructure.persistence.repository`: Spring Data JPA repositories.
- `com.ho.account.masterdata.core.infrastructure.adapter`: adapters that expose master-data lookups to other modules.
- `com.ho.account.masterdata.batch.application`: batch orchestration only.

## Managed Domains

- `AccountSubject`: chart of accounts and reporting classification.
- `BusinessPartner`: customers, vendors, banks, and other counterparties.
- `Department`: cost centers, profit centers, and organization hierarchy.
- `Currency` and `ExchangeRate`: currency master and effective exchange rates.
- `FiscalPeriod`: accounting period status.
- `Product`: product or service reference data.
- `MasterDataChangeRequest`: controlled change request, approval, rejection, and application history.

## Change Request Control

Operational changes should be registered as `MasterDataChangeRequest` records when approval and auditability are required.

1. A requester creates a change request with target type, target key, change type, effective date, version, reason, and JSON payload.
2. A different user approves or rejects the request. The domain enforces separation of duties.
3. Approved requests can be applied one by one or in bulk through due-date processing.
4. Successfully applied requests move to `APPLIED`.

Supported automatic apply targets are currently `ACCOUNT_SUBJECT`, `BUSINESS_PARTNER`, `DEPARTMENT`, and `PRODUCT`. `CURRENCY`, `EXCHANGE_RATE`, and `FISCAL_PERIOD` are modeled but do not yet have automatic apply use cases.

## Run

```bash
./gradlew :master-data:bootRun
```

## Test

```bash
./gradlew :master-data:test
```

## Docker

```bash
docker build -t account/master-data master-data
docker run -p 8082:8082 account/master-data
```
