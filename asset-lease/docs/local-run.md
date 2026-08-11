# asset-lease local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

## IntelliJ에서 API 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. Project SDK와 Gradle JVM을 JDK 17로 맞춘다.
3. 상단 Run Configuration에서 `Asset Lease API bootRun`을 선택한다.
4. 실행 후 `http://localhost:8083/actuator/health` 또는 API 엔드포인트로 확인한다.

추가된 실행 구성:

- `.run/Asset Lease API bootRun.run.xml`
- `.run/Asset Lease Batch Context.run.xml`
- `.run/Asset Lease Tests.run.xml`

`Asset Lease API bootRun`은 `:asset-lease:api:bootRun`을 실행하고, `Asset Lease Batch Context`는 `local` 프로파일로 `:asset-lease:batch:bootRun`을 실행한다. Batch의 `application-local.yml`이 외부 Config Server, Discovery, Vault, Eureka, Kafka listener를 끄고 격리된 H2와 Batch metadata를 준비하며, 업무 Job 자동 실행도 끈다.

## PowerShell에서 실행하기

테스트:

```powershell
.\gradlew :asset-lease:core:test --console=plain --max-workers=1 --no-daemon
```

API 실행:

```powershell
.\gradlew :asset-lease:api:bootRun --args="--spring.profiles.active=local --server.port=8083 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

Batch 컨텍스트 실행:

```powershell
.\gradlew :asset-lease:batch:bootRun --args="--spring.profiles.active=local" --console=plain --max-workers=1
```

패키징된 Batch JAR도 같은 프로파일 하나만 지정하면 된다.

```powershell
.\gradlew :asset-lease:batch:bootJar --offline --console=plain
java -jar asset-lease/batch/build/libs/account-asset-lease-batch-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

이 실행은 in-memory H2 PostgreSQL compatibility mode와 `create-drop`을 사용하므로 프로세스가 끝나면 로컬 데이터가 사라진다. Job은 기본적으로 실행되지 않는다.

컴파일만 빠르게 확인:

```powershell
.\gradlew :asset-lease:core:compileJava :asset-lease:api:compileJava :asset-lease:batch:compileJava --console=plain --max-workers=1 --no-daemon
```

## Kafka와 지급결의 포트 주의사항

자산 등록, 월상각, 처분, 리스 최초 인식, 리스 월별 처리는 `AssetEventPort`로 Kafka 이벤트를 발행한다. 로컬에서 실제 API를 호출하려면 Kafka가 떠 있어야 이벤트 전송이 안정적이다.

리스료 지급결의 유즈케이스는 `LeasePaymentResolutionPort`가 필요하다. 현재 asset-lease 단독 실행에서는 포트 구현이 없으면 fallback 어댑터가 예외를 던진다. 따라서 리스료 지급결의까지 확인하려면 `expenditure-resolution`과의 통합 실행 또는 테스트 더블이 필요하다.

## 예시 요청

고정자산 등록은 실행자 헤더가 필요하다.

```http
POST /api/fixed-assets
X-User-ID: local-user
Content-Type: application/json
```

```json
{
  "assetCode": "FA-2026-001",
  "assetName": "개발 서버",
  "accountSubjectCode": "12300",
  "accumulatedAccountCode": "12399",
  "expenseAccountCode": "51500",
  "acquisitionDate": "2026-06-01",
  "acquisitionCost": 12000000,
  "usefulLife": 60,
  "depreciationMethod": "STRAIGHT_LINE",
  "residualValue": 0,
  "departmentCode": "D001"
}
```

## 자주 보는 실패

| 증상 | 확인할 지점 |
| --- | --- |
| Config Server 접속 오류 | `--spring.cloud.config.enabled=false` 인자가 들어갔는지 확인 |
| Eureka 등록 오류 | `--eureka.client.enabled=false` 인자가 들어갔는지 확인 |
| Kafka 연결 오류 | API 호출로 이벤트를 발행했다면 로컬 Kafka 또는 테스트 대체 어댑터가 필요 |
| 리스료 지급결의 예외 | `LeasePaymentResolutionPort` 구현이 없는 단독 실행인지 확인 |
| Batch targetDate 오류 | `assetDepreciationJob` 실행 시 `targetDate=YYYY-MM-DD` JobParameter를 넣었는지 확인 |

## Batch JobParameter 예시

감가상각 배치를 실제로 실행할 때는 처리 기준일을 명시한다.

```powershell
.\gradlew :asset-lease:batch:bootRun --args="--spring.profiles.active=local --spring.batch.job.enabled=true --spring.batch.job.name=assetDepreciationJob targetDate=2026-06-30" --console=plain --max-workers=1
```

초보자 관점에서는 `targetDate`가 "이번 배치가 어느 회계월의 상각을 처리하는지"를 고정하는 값이다. 재실행해도 같은 `targetDate`를 넣으면 같은 회계월 기준으로 처리된다. Batch 앱은 API 서버가 아니므로 `spring.main.web-application-type=none`으로 Tomcat을 띄우지 않고, Job 완료 상태를 Gradle 종료 코드로 확인한다. 대량 감가상각은 core pipeline이 결과 값만 계산하고 `AssetJdbcAdapter`가 JDBC bulk update로 한 번만 반영하므로, 단건 API처럼 JPA 엔티티를 저장하는 흐름과 구분해서 본다.
