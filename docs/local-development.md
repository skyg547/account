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

## 4. Spring Boot 실행 모듈

현재 루트 `app`는 실행 소스가 없으므로 IntelliJ에서 아래 실행 클래스를 기준으로 Run Configuration을 만듭니다.

| 모듈 | 실행 클래스 | 기본 포트 | 용도 |
| --- | --- | ---: | --- |
| `discovery` | `com.ho.account.discovery.DiscoveryApplication` | 8761 | Eureka |
| `config-server` | `com.ho.account.configserver.ConfigServerApplication` | 설정 파일 기준 | Config Server |
| `gateway` | `com.ho.account.gateway.GatewayApplication` | 설정 파일 기준 | API Gateway |
| `auth` | `com.ho.account.auth.AuthApplication` | 설정 파일 기준 | 인증 |
| `master-data` | `com.ho.account.MasterDataApplication` | 설정 파일 기준 | 기준정보 |
| `journal-ledger:api` | `com.ho.account.journalledger.JournalLedgerApplication` | 설정 파일 기준 | 전표/원장 API |
| `account-mart:mart-api` | `com.ho.account.mart.api.AllowanceMartApiApplication` | 8085 | ECL 입력 마트 API |
| `account-mart:mart-batch` | `com.ho.account.mart.batch.AllowanceMartBatchApplication` | 8086 또는 CLI | ECL 입력 마트 배치 |
| `ecl:ecl-api` | `com.ho.account.ecl.api.AllowanceEclApiApplication` | 설정 파일 기준 | ECL API |
| `ecl:ecl-batch` | `com.ho.account.ecl.batch.AllowanceEclBatchApplication` | CLI 또는 설정 기준 | ECL 배치 |

## 5. IntelliJ Run Configuration 만들기

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
```

## 6. 인프라 실행

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

## 7. 문서 작업 검증

문서만 고쳤더라도 아래를 확인합니다.

```powershell
rg -n "\]\((?!https?://|#)" docs README.md account-mart -S
.\gradlew :account-mart:mart-core:test :account-mart:mart-batch:test --console=plain --max-workers=1 --no-daemon
git diff --check
```

링크 검사는 정규식만으로 완전하지 않으므로, 변경한 문서의 상대 링크는 직접 한 번 더 확인합니다.
