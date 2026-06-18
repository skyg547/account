# deposit local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

## IntelliJ에서 실행하기

공유 실행 설정:

- `.run/Deposit API bootRun.run.xml`
- `.run/Deposit Batch Context.run.xml`

실행 순서:

1. IntelliJ에서 루트 프로젝트를 연다.
2. Gradle sync를 완료한다.
3. 상단 Run Configuration에서 `Deposit API bootRun` 또는 `Deposit Batch Context`를 선택한다.
4. 처음에는 로컬 어댑터 설정이 들어간 공유 실행 설정을 사용한다.

## PowerShell에서 실행하기

API 앱:

```powershell
.\gradlew :deposit:api:bootRun --args="--spring.profiles.active=local --server.port=8087 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

Batch 컨텍스트:

```powershell
.\gradlew :deposit:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

Batch 앱은 Spring Batch의 `JobRegistry`에 Job을 등록할 때 `JobRegistrySmartInitializingSingleton`을 사용한다. 초보자 관점에서는 "애플리케이션이 모든 Bean을 만든 뒤 Batch Job 목록을 등록한다"는 뜻이며, 로컬 기동 중 `jobRegistryBeanPostProcessor`가 다른 Bean을 너무 일찍 깨우면서 내는 경고를 줄이기 위한 설정이다.

빌드 검증:

```powershell
.\gradlew :deposit:core:test :deposit:api:bootJar :deposit:batch:bootJar --console=plain --max-workers=1 --no-daemon
```

## 로컬 어댑터 의미

`account.deposit.local-adapters.enabled=true`는 단독 실행을 위한 학습용 설정입니다.

- `LocalDepositMasterDataAdapter`: 초기입금 전표 계정과목 검증을 통과시킵니다.
- `LocalDepositJournalPostingAdapter`: journal-ledger 없이 전표 생성 결과 식별자를 반환합니다.

운영에서는 이 설정을 끄고 master-data, journal-ledger의 실제 어댑터를 연결해야 합니다.
