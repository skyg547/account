# receivable local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

`receivable`은 이제 `core/api/batch` 하위 Gradle 모듈로 분리되어 있다.

- `receivable:core`: 매출채권/수납/매칭 도메인, command 기반 UseCase, persistence adapter, local 외부 포트 adapter.
- `receivable:api`: Spring Boot HTTP 실행 진입점. Controller/DTO/Bean Validation을 소유하고, 업무 판단은 core command/usecase를 참조한다.
- `receivable:batch`: Spring Boot Batch 실행 진입점. 대량 매칭 Job/Step 제어와 JobRegistry 등록 순서만 담당하고 업무 계산은 core를 참조한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 `account > receivable > core > Tasks > verification > test`를 실행한다.
3. 또는 상단 Run Configuration에서 `Receivable Module Tests`를 선택한다.
4. 실행 후 `BUILD SUCCESSFUL`을 확인한다.
5. HTTP API를 직접 띄울 때는 Gradle task `:receivable:api:bootRun`을 실행한다.
6. Batch 컨텍스트만 확인할 때는 Gradle task `:receivable:batch:bootRun`을 실행한다.

추가된 실행 구성:

- `.run/Receivable Module Tests.run.xml`

## PowerShell에서 실행하기

```powershell
.\gradlew :receivable:core:test :receivable:api:test :receivable:batch:compileJava --console=plain --max-workers=1 --no-daemon
```

빠른 컴파일만 확인할 때:

```powershell
.\gradlew :receivable:core:compileJava :receivable:api:compileJava :receivable:batch:compileJava --console=plain --max-workers=1 --no-daemon
```

API 서버 실행:

```powershell
.\gradlew :receivable:api:bootRun --console=plain --max-workers=1
```

Batch 컨텍스트 실행:

```powershell
.\gradlew :receivable:batch:bootRun --console=plain --max-workers=1
```

실제 Batch Job 실행:

```powershell
.\gradlew :receivable:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=receivableAutoMatchingJob" --console=plain --max-workers=1
```

`receivableAutoMatchingJob`은 RECEIVED/UNMATCHED/PARTIAL_MATCHED 수납을 core 자동 매칭 유즈케이스로 넘긴다.

API 스모크만 확인하고 서버를 계속 띄우지 않을 때:

```powershell
.\gradlew :receivable:api:bootRun --args="--spring.main.web-application-type=none" --console=plain --max-workers=1
```


Batch 스모크만 확인하고 Job을 자동 실행하지 않을 때:

```powershell
.\gradlew :receivable:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

위 실행에서는 `ReceivableBatchJobRegistryConfiguration`이 Job 등록을 singleton 초기화 이후로 늦춰 `jobRegistryBeanPostProcessor` 조기 초기화 경고를 피한다.
## local profile 동작

호스트 앱에서 사용할 수 있는 대표 설정값:

```properties
account.receivable.account-mapping.accounts-receivable-account-code=11100
account.receivable.account-mapping.revenue-account-code=40100
account.receivable.account-mapping.output-vat-account-code=22100
account.receivable.account-mapping.cash-account-code=10100
account.receivable.account-mapping.ar-clearing-account-code=21100
```

## 로컬 검증 시 자주 보는 실패

| 증상 | 확인할 지점 |
| --- | --- |
| `Customer info missing` | master-data 테스트 더블 또는 테스트 데이터에 고객 코드가 있는지 확인 |
| `Account missing` | 계정 매핑 기본값에 해당하는 계정과목이 테스트 더블에 등록되어 있는지 확인 |
| `Invalid match amount` | 수동 매칭 금액이 수납 미배분액과 채권 잔액보다 크지 않은지 확인 |
| 자동 매칭이 되지 않음 | 참조번호 후보가 중복인지, 만기일 허용 범위 7일 안의 후보가 여러 건인지 확인 |
