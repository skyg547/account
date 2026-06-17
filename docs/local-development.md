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
.\gradlew :contracts:compileJava :shared-kernel:compileJava --console=plain
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

현재 루트 `app`는 실행 소스가 없으므로 IntelliJ에서 아래 실행 클래스를 기준으로 Run Configuration을 만듭니다.

| 모듈 | 실행 클래스 | 기본 포트 | 용도 |
| --- | --- | ---: | --- |
| `discovery` | `com.ho.account.discovery.DiscoveryApplication` | 8761 | Eureka |
| `config-server` | `com.ho.account.configserver.ConfigServerApplication` | 설정 파일 기준 | Config Server |
| `gateway` | `com.ho.account.gateway.GatewayApplication` | 설정 파일 기준 | API Gateway |
| `auth` | `com.ho.account.auth.AuthApplication` | 설정 파일 기준 | 인증 |
| `master-data` | `com.ho.account.MasterDataApplication` | 설정 파일 기준 | 기준정보 |
| `governance` | `com.ho.account.governance.GovernanceApplication` | 설정 파일 기준 | 감사/승인 |
| `journal-ledger:api` | `com.ho.account.journalledger.JournalLedgerApplication` | 설정 파일 기준 | 전표/원장 API |
| `deposit:api` | `com.ho.account.deposit.DepositApplication` | 8087 | 예금 API |
| `deposit:batch` | `com.ho.account.deposit.batch.DepositBatchApplication` | CLI 또는 설정 기준 | 예금 배치 컨텍스트 |
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
| `Deposit API bootRun` | `:deposit:api:bootRun` | 로컬 어댑터 기반 예금 API 실행 |
| `Deposit Batch Context` | `:deposit:batch:bootRun` | 로컬 어댑터 기반 예금 batch 컨텍스트 기동 |
| `Reporting API bootRun` | `:reporting:api:bootRun` | memory 모드 재무보고 API 실행 |
| `Reporting Batch Context` | `:reporting:batch:bootRun` | memory 모드 재무보고 batch 컨텍스트 기동 |
| `Config Server bootRun` | `:config-server:bootRun` | 중앙 설정 서버 로컬 실행 |
| `Discovery bootRun` | `:discovery:bootRun` | Eureka 서버 로컬 실행 |
| `Auth bootRun` | `:auth:bootRun` | Auth API 로컬 실행 |
| `Master Data bootRun` | `:master-data:bootRun` | 기준정보 API 로컬 실행 |
| `Governance bootRun` | `:governance:bootRun` | 감사/승인 API 로컬 실행 |
| `Gateway bootRun` | `:gateway:bootRun` | Gateway 로컬 실행 |
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
.\gradlew :account-mart:mart-api:bootRun --console=plain
.\gradlew :account-mart:mart-batch:bootRun --console=plain
.\gradlew :reporting:api:bootRun --args="--spring.profiles.active=local --server.port=8090 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
.\gradlew :deposit:api:bootRun --args="--spring.profiles.active=local --server.port=8087 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

## 7. 인프라 실행

IntelliJ에서 MSA 인프라를 순서대로 직접 띄울 때는 아래 순서를 따릅니다.

```text
Config Server bootRun -> Discovery bootRun -> Auth/Master Data/Governance/Journals -> Gateway bootRun
```

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
