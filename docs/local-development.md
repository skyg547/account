# 로컬 개발 실행 가이드

이 문서는 IntelliJ IDEA와 Gradle로 이 저장소를 로컬에서 실행하는 방법을 정리합니다.

## 1. 필수 도구

- JDK 17
- IntelliJ IDEA
- Git
- PowerShell
- Docker Desktop은 선택 사항입니다. Kafka, Redis, Zipkin 같은 인프라를 같이 띄울 때 필요합니다.

`gradle.properties`에는 JDK 후보 경로가 아래처럼 들어 있습니다.

```properties
org.gradle.java.installations.paths=C:\\Java\\jdk17,C:\\Java\\jdk21,C:\\Java\\jdk8
```

로컬 JDK가 다른 위치에 있으면 IntelliJ의 Gradle JVM을 직접 JDK 17로 지정하면 됩니다.

## 2. IntelliJ 프로젝트 열기

1. `File > Open`에서 저장소 루트 `C:\Users\skyg547\IdeaProjects\account`를 선택합니다.
2. Gradle 프로젝트로 import합니다.
3. `File > Project Structure > Project SDK`를 JDK 17로 설정합니다.
4. `Settings > Build, Execution, Deployment > Build Tools > Gradle`에서 Gradle JVM을 JDK 17로 설정합니다.
5. `Build and run using`, `Run tests using`은 Gradle로 두는 편이 멀티모듈 의존성 확인에 안전합니다.

## 3. 루트에서 먼저 확인할 명령

```powershell
.\gradlew projects --console=plain
.\gradlew :contracts:test :shared-kernel:test --console=plain --max-workers=1 --no-daemon
```

전체 테스트는 오래 걸립니다. 모듈을 수정할 때는 해당 모듈과 직접 연관 모듈부터 확인합니다.

## 4. Spring Cloud LoadBalancer 캐시

Eureka, Gateway, OpenFeign을 사용하는 실행 모듈은 Spring Cloud LoadBalancer를 함께 사용합니다. 이 저장소는 해당 모듈의 Gradle 의존성에 `com.github.ben-manes.caffeine:caffeine`을 명시해 기본 캐시 대신 Caffeine 기반 캐시를 사용하도록 맞췄습니다.

초보자 관점에서는 아래처럼 이해하면 됩니다.

- Eureka는 서비스 이름으로 실제 인스턴스를 찾는 주소록입니다.
- LoadBalancer는 주소록에서 받은 여러 인스턴스 중 어디로 요청을 보낼지 고릅니다.
- Caffeine은 그 인스턴스 목록을 짧게 캐시해 매번 새로 계산하지 않도록 돕는 로컬 캐시입니다.
- `local` profile에서 Eureka를 꺼도 클래스패스에는 LoadBalancer가 남아 있을 수 있으므로, Caffeine 의존성은 로컬 기동 경고를 줄이는 데도 도움이 됩니다.
- H2와 로컬 어댑터만으로 단독 실행할 때는 `--spring.cloud.discovery.enabled=false`와 함께 `--spring.cloud.loadbalancer.enabled=false`를 넣어 LoadBalancer 자동 구성 자체를 끕니다. 이 경우 서비스 디스커버리와 외부 서비스 라우팅을 쓰지 않겠다는 뜻입니다.

## 5. Spring Boot 실행 모듈

통합 `app` Gradle 프로젝트는 제거되었으므로 IntelliJ에서 아래 서비스별 실행 클래스를 기준으로 Run Configuration을 만듭니다. 로컬의 `app/build`는 과거 빌드 산출물일 수 있으며 실행 대상이 아닙니다.

| 모듈 | 실행 클래스 | 기본 포트 | 용도 |
| --- | --- | ---: | --- |
| `discovery` | `com.ho.account.discovery.DiscoveryApplication` | 8761 | Eureka |
| `config-server` | `com.ho.account.configserver.ConfigServerApplication` | 8888 | native Config Server, strict repository readiness |
| `gateway` | `com.ho.account.gateway.GatewayApplication` | 8000 | API Gateway |
| `auth` | `com.ho.account.auth.AuthApplication` | 설정 파일 기준 | 인증 |
| `master-data:api` | `com.ho.account.masterdata.MasterDataApplication` | 8082 | 기준정보 HTTP API |
| `master-data:batch` | `com.ho.account.masterdata.batch.MasterDataBatchApplication` | CLI | 기준정보 유효성 Batch |
| `governance` | `com.ho.account.governance.GovernanceApplication` | 설정 파일 기준 | 감사/승인 |
| `journal-ledger:api` | `com.ho.account.journalledger.JournalLedgerApplication` | 설정 파일 기준 | 전표/원장 API |
| `deposit:api` | `com.ho.account.deposit.DepositApplication` | 8087 | 예금 API |
| `deposit:batch` | `com.ho.account.deposit.batch.DepositBatchApplication` | CLI 또는 설정 기준 | 예금 배치 컨텍스트 |
| `asset-lease:api` | `com.ho.account.asset.api.AssetLeaseApiApplication` | 8083 | 고정자산/리스 API |
| `asset-lease:batch` | `com.ho.account.asset.batch.AssetLeaseBatchApplication` | CLI 또는 설정 기준 | 고정자산 감가상각 배치 컨텍스트 |
| `payable:api` | `com.ho.account.expenditure.payable.api.PayableApiApplication` | 8091 | 매입채무/지급 API |
| `payable:batch` | `com.ho.account.expenditure.payable.batch.PayableBatchApplication` | CLI 또는 설정 기준 | 매입채무/지급 배치 컨텍스트 |
| `receivable:api` | `com.ho.account.receivable.api.ReceivableApiApplication` | 8092 | 매출채권/수납 API |
| `receivable:batch` | `com.ho.account.receivable.batch.ReceivableBatchApplication` | CLI 또는 설정 기준 | 매출채권/수납 배치 컨텍스트 |
| `reconciliation:api` | `com.ho.account.reconciliation.api.ReconciliationApiApplication` | 8093 | 대사 API |
| `reconciliation:batch` | `com.ho.account.reconciliation.batch.ReconciliationBatchApplication` | CLI 또는 설정 기준 | 대사 배치 컨텍스트 |
| `tax:api` | `com.ho.account.tax.api.TaxApiApplication` | 8094 | 세금계산서 API |
| `tax:batch` | `com.ho.account.tax.batch.TaxBatchApplication` | CLI 또는 설정 기준 | 세금계산서 배치 컨텍스트 |
| `expenditure-resolution:api` | `com.ho.account.expenditure.resolution.api.ExpenditureResolutionApiApplication` | 8095 | 지출결의 API |
| `expenditure-resolution:batch` | `com.ho.account.expenditure.resolution.batch.ExpenditureResolutionBatchApplication` | CLI 또는 설정 기준 | 지출결의 배치 컨텍스트 |
| `account-mart:mart-api` | `com.ho.account.mart.api.AllowanceMartApiApplication` | 8085 | ECL 입력 마트 API |
| `account-mart:mart-batch` | `com.ho.account.mart.batch.AllowanceMartBatchApplication` | 8086 또는 CLI | ECL 입력 마트 배치 |
| `ecl:ecl-api` | `com.ho.account.ecl.api.AllowanceEclApiApplication` | 설정 파일 기준 | ECL API |
| `ecl:ecl-batch` | `com.ho.account.ecl.batch.AllowanceEclBatchApplication` | CLI 또는 설정 기준 | ECL 배치 |
| `reporting:api` | `com.ho.account.reporting.ReportingApiApplication` | 8090 | 재무보고 API |
| `reporting:batch` | `com.ho.account.reporting.batch.ReportingBatchApplication` | CLI 또는 설정 기준 | 재무보고 배치 컨텍스트 |

## 6. IntelliJ Run Configuration 만들기

이 저장소에는 일부 공통 실행 설정을 `.run/`에 공유합니다. IntelliJ가 자동으로 읽지 않으면 `Run > Edit Configurations`에서 Gradle 설정을 직접 만들면 됩니다.

현재 제공되는 공유 실행 설정:

| 이름 | Gradle task | 설명 |
| --- | --- | --- |
| `Account Mart API bootRun` | `:account-mart:mart-api:bootRun` | 마트 API 로컬 실행 |
| `Account Mart Batch Demo` | `:account-mart:mart-batch:bootRun` | demo profile + `integratedPositionEtlJob` 실행 |
| `ECL API bootRun` | `:ecl:ecl-api:bootRun` | ECL API 로컬 실행 |
| `ECL Batch Context` | `:ecl:ecl-batch:bootRun` | demo profile로 ECL batch 컨텍스트 기동, Job 자동 실행 없음 |
| `Journal Ledger API bootRun` | `:journal-ledger:api:bootRun` | 전표/원장 API 로컬 실행 |
| `Journal Ledger API JDBC Bulk` | `:journal-ledger:api:bootRun` | JDBC bulk 원장 저장 어댑터 모드로 실행 |
| `Journal Ledger Batch Reaggregation` | `:journal-ledger:batch:bootRun` | GL/SL 잔액 재집계 Job 실행 |
| `Deposit API bootRun` | `:deposit:api:bootRun` | 로컬 어댑터 기반 예금 API 실행 |
| `Deposit Batch Context` | `:deposit:batch:bootRun` | 로컬 어댑터 기반 예금 batch 컨텍스트 기동 |
| `Asset Lease API bootRun` | `:asset-lease:api:bootRun` | 자산/리스 API 로컬 실행 |
| `Asset Lease Batch Context` | `:asset-lease:batch:bootRun` | 자산 감가상각 batch 컨텍스트 기동 |
| `Reporting API bootRun` | `:reporting:api:bootRun` | memory 모드 재무보고 API 실행 |
| `Reporting Batch Context` | `:reporting:batch:bootRun` | memory 모드 재무보고 batch 컨텍스트 기동 |
| `Config Server bootRun` | `:config-server:bootRun` | native `config-repo` + 8888 + readiness 로컬 실행 |
| `Discovery bootRun` | `:discovery:bootRun` | Config Server 연동 Eureka 실행 |
| `Discovery standalone bootRun` | `:discovery:bootRun` | 8761/readiness/registry 단독 smoke |
| `Auth bootRun` | `:auth:bootRun` | Auth API 로컬 실행 |
| `Master Data bootRun` | `:master-data:api:bootRun` | 기준정보 API 로컬 실행 |
| `Master Data Batch Validity` | `:master-data:batch:bootRun` | 필수 `asOfDate` 기준 유효성 Job 실행 |
| `Governance bootRun` | `:governance:bootRun` | 감사/승인 API 로컬 실행 |
| `Gateway bootRun` | `:gateway:bootRun` | Config/Discovery/Auth 연동 Gateway 실행 |
| `Gateway standalone bootRun` | `:gateway:bootRun` | 외부 의존성을 끈 포트/컨텍스트 smoke |
| `Foundation Library Compile` | `:contracts:compileJava`, `:shared-kernel:compileJava` | 공통 library 모듈 컴파일 |
| `Foundation Infra Tests` | foundation/infra 테스트·assemble 묶음 | 마지막 foundation/infra 문서 배치 검증 |

Spring Boot 앱:
1. `Run > Edit Configurations > + > Spring Boot`
2. `Main class`에 위 실행 클래스를 선택합니다.
3. `Use classpath of module`에 해당 모듈을 선택합니다.
4. 필요하면 `Active profiles`에 `demo`, `postgres`, `docker` 등을 입력합니다.
5. 실행 전 Gradle build가 필요하면 `Before launch`에 `Gradle task`를 추가합니다.

Gradle task로 실행:
```powershell
.\gradlew :master-data:api:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain
.\gradlew :master-data:batch:bootRun --args="--spring.profiles.active=local --spring.batch.job.enabled=true --spring.batch.job.name=masterDataValidityJob asOfDate=2026-07-29 --spring.cloud.config.enabled=false --spring.config.on-not-found=ignore --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain
.\gradlew :account-mart:mart-api:bootRun --console=plain
.\gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.batch.job.enabled=true --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30 --mart.batch.cdm-event.enabled=false" --console=plain
.\gradlew :ecl:ecl-batch:bootRun --args="--spring.profiles.active=demo --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.batch.job.enabled=false job.name=standaloneDqJob baseDate=2026-04-30 runId=DQ-20260430 --spring.flyway.enabled=false" --console=plain
.\gradlew :journal-ledger:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.batch.job.enabled=true --spring.batch.job.name=dailyBalanceReaggregationJob baseDate=2026-04-30" --console=plain
.\gradlew :reporting:api:bootRun --args="--spring.profiles.active=local --server.port=8090 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
.\gradlew :deposit:api:bootRun --args="--spring.profiles.active=local --server.port=8087 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
.\gradlew :asset-lease:api:bootRun --args="--spring.profiles.active=local --server.port=8083 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
.\gradlew :asset-lease:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
.\gradlew :payable:api:bootRun --console=plain --max-workers=1
.\gradlew :receivable:api:bootRun --console=plain --max-workers=1
.\gradlew :reconciliation:api:bootRun --console=plain --max-workers=1
.\gradlew :tax:api:bootRun --console=plain --max-workers=1
.\gradlew :expenditure-resolution:api:bootRun --console=plain --max-workers=1
```

Deposit/Reporting Batch 컨텍스트는 Spring Batch Job 목록을 모든 Bean 생성 뒤에 등록하도록 `JobRegistrySmartInitializingSingleton`을 사용한다. 이 설정은 로컬 기동 중 `jobRegistryBeanPostProcessor` 관련 조기 초기화 경고를 줄이기 위한 Batch 인프라 설정이며, 업무 계산 로직을 Batch 앱에 넣는 변경은 아니다.

Payable/Receivable/Reconciliation/Tax/Expenditure Resolution도 같은 원칙을 따른다. API/BATCH 모듈은 Spring Boot 진입점과 로컬 H2 설정을 제공하고, 업무 상태 전이와 금액 계산은 각 `core` 모듈의 domain/application 계층을 참조한다.

ECL Batch는 `spring.batch.job.enabled=false`로 Boot 기본 자동 실행을 막고, `job.name`을 받은 `JobRunner`가 명시 Job을 한 번 실행한다. Job 상태가 `COMPLETED`가 아니면 프로세스가 실패하므로, CLI 검증에서 실패 Job을 성공으로 오인하지 않는다.


## 6-1. 전체 모듈 순차 실행 매트릭스

이 표는 2026-07-02 기준 로컬 H2/메모리 어댑터로 검증한 실행 단위입니다. 메모리 부족을 피하려면 아래 순서로 하나씩 실행하고, 다음 모듈을 띄우기 전에 이전 Java 프로세스를 종료합니다.

| 구분 | 실행 대상 | Gradle 기준 | 로컬 검증 방식 |
| --- | --- | --- | --- |
| Foundation | `contracts`, `shared-kernel` | `compileJava`, `test` | Spring Boot 앱이 아닌 공통 계약/커널 모듈 |
| 독립/인프라 API | `config-server`, `discovery`, `gateway`, `auth`, `master-data:api`, `governance` | 모듈별 `bootRun` | 필요 시 순차 기동, local smoke는 Config/Eureka 의존 비활성화 |
| 업무 API | `asset-lease`, `journal-ledger`, `closing`, `loan`, `deposit`, `payable`, `receivable`, `reconciliation`, `tax`, `expenditure-resolution`, `reporting` | `:<module>:api:bootRun` | 내장 WAS + H2 또는 memory adapter |
| Mart/ECL API | `account-mart`, `ecl` | `:account-mart:mart-api:bootRun`, `:ecl:ecl-api:bootRun` | 내장 WAS + demo/local datasource |
| 업무 Batch | `asset-lease`, `closing`, `deposit`, `journal-ledger`, `loan`, `payable`, `receivable`, `reconciliation`, `tax`, `expenditure-resolution`, `reporting` | `:<module>:batch:bootRun` | `spring.main.web-application-type=none`, Spring Batch context 또는 Job 실행 |
| 기준정보 Batch | `master-data:batch` | `:master-data:batch:bootRun` | 필수 `asOfDate` + `masterDataValidityJob` |
| Mart/ECL Batch | `account-mart`, `ecl` | `:account-mart:mart-batch:bootRun`, `:ecl:ecl-batch:bootRun` | demo profile + 명시 Job 실행 |

공통 H2 API smoke 옵션:

```powershell
--spring.profiles.active=local --server.port=0 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always
```

공통 H2 Batch context 옵션:

```powershell
--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always
```

PostgreSQL로 실행할 때는 H2용 `ddl-auto=create-drop`을 운영처럼 쓰지 않습니다. 로컬 PostgreSQL을 직접 띄운 뒤 아래 datasource 값을 모듈 DB명에 맞춰 바꿉니다.

```powershell
--spring.datasource.url=jdbc:postgresql://localhost:5432/account_local --spring.datasource.username=account --spring.datasource.password=account --spring.datasource.driver-class-name=org.postgresql.Driver --spring.jpa.hibernate.ddl-auto=none --spring.flyway.enabled=true
```

운영 image 대상 API/Batch의 PostgreSQL driver와 API readiness dependency가 실제 `runtimeClasspath`와 `bootJar`에 들어가는지는 다음 공용 gate로 확인합니다. 이 명령은 DB에 접속하지 않습니다.

```powershell
.\gradlew.bat verifyProductionRuntimeDependencies --offline
```

현재 gate는 11개 bounded context의 API/Batch 22개에서 PostgreSQL JDBC JAR을, API 11개에서 Actuator JAR을 확인합니다. 이번 변경은 기존 Local H2 dependency scope를 수정하지 않습니다. Closing API의 local runtime H2 누락처럼 기존 독립 실행 결함은 #77 등 모듈 Issue에서 별도로 해결합니다.

대표 Spring Batch Job smoke 명령은 아래 기준으로 검증했습니다. 빈 H2 데이터 기준이므로 Job 성공은 "엔트리포인트와 메타 테이블, 파라미터, core 위임 경로가 정상"이라는 뜻이고, 운영 금액 결과 검증은 별도 seed data가 필요합니다.

| 모듈 | 대표 Job | 필수 파라미터 예시 |
| --- | --- | --- |
| `master-data:batch` | `masterDataValidityJob` | `asOfDate=2026-07-29` |
| `asset-lease:batch` | `assetDepreciationJob` | `targetDate=2026-06-30` |
| `closing:batch` | `fxValuationJob` | `valuationDate=2026-04-30 valuationBatchId=20260430` |
| `closing:batch` | `eclProvisionJob` | `closingDate=2026-04-30 provisionBatchId=20260430` |
| `loan:batch` | `loanInterestAccrualJob` | `accrualDate=2026-04-30` + 계정 매핑 옵션 |
| `deposit:batch` | `depositAccountIntegrityJob` | `asOfDate=2026-06-19` |
| `payable:batch` | `payablePaymentRunJob` | `runDate=2026-06-19 createdBy=SMOKE description=Smoke` |
| `receivable:batch` | `receivableAutoMatchingJob` | 없음 |
| `reconciliation:batch` | `reconciliationDailyJob` | `reconciliationDate=2026-06-19 runBy=SMOKE deepMode=false` |
| `tax:batch` | `taxInvoiceValidationJob` | `startDate=2026-06-19 endDate=2026-06-19` |
| `expenditure-resolution:batch` | `expenditureResolutionApprovalJob` | `startDate=2026-06-19 endDate=2026-06-19 paymentDueDate=2026-06-19` |
| `reporting:batch` | `reportingStatementGenerationJob` | `baseDate=2026-03-31 requester=local-batch` |
| `account-mart:mart-batch` | `integratedPositionEtlJob` | `baseDate=2026-04-30 --mart.batch.cdm-event.enabled=false` |
| `ecl:ecl-batch` | `standaloneDqJob` | `job.name=standaloneDqJob baseDate=2026-04-30 runId=DQ-20260430` |
| `journal-ledger:batch` | `dailyBalanceReaggregationJob` | `startDate=2026-04-01 endDate=2026-04-30` 또는 `baseDate=2026-04-30` |
## 7. 인프라 실행

IntelliJ에서 MSA 인프라를 순서대로 직접 띄울 때는 아래 순서를 따릅니다.

```text
Config Server bootRun -> Discovery bootRun (readiness UP) -> Auth/Master Data/Governance/Journals -> Gateway bootRun (8000)
```

Config Server는 DB가 필요하지 않습니다. 다음 상태가 모두 확인된 뒤 Discovery를 실행합니다.

```powershell
(Invoke-WebRequest http://localhost:8888/actuator/health/readiness).StatusCode
(Invoke-WebRequest http://localhost:8888/master-data/default).StatusCode
```

두 요청은 HTTP 200이어야 하며 Config 응답 본문은 민감 설정을 포함할 수 있으므로 공유 로그에 붙이지 않습니다.

전체 인프라를 한 번에 띄울 때:

```powershell
.\start-infra.bat
```

또는 필요한 컨테이너만 Docker Compose로 실행합니다.

```powershell
docker compose up -d
docker compose ps
```

로컬 단위 테스트와 H2 기반 모듈 실행만 확인할 때는 인프라를 모두 띄우지 않아도 됩니다.

## 8. 문서 작업 검증

문서만 고쳤더라도 아래를 확인합니다.

```powershell
rg -n "\]\((?!https?://|#)" docs README.md account-mart -S
.\gradlew :account-mart:mart-core:test :account-mart:mart-batch:test --console=plain --max-workers=1 --no-daemon
git diff --check
```

링크 검사는 정규식만으로 완전하지 않으므로, 변경한 문서의 상대 링크는 직접 한 번 더 확인합니다.
