# Loan 로컬 실행 가이드

## 전제 조건

- JDK/Gradle JVM 17
- 저장소 루트를 Gradle 프로젝트로 import
- 전표 흐름에는 Master Data의 유효 거래처·통화·계정과 Journal Ledger 구성이 필요

## 테스트와 패키징

```powershell
.\gradlew :loan:core:test :loan:api:bootJar :loan:batch:bootJar --console=plain --max-workers=1 --no-daemon
```

Flyway 검증은 core 테스트의 `LoanFlywayMigrationTest`에 포함됩니다.

## API 컨텍스트

```powershell
.\gradlew :loan:api:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-api --spring.data.redis.repositories.enabled=false --server.port=8087 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain --max-workers=1 --no-daemon
```

권장 확인 순서:

1. `POST /api/loan/loans`: 계약 생성(`PENDING_DISBURSEMENT`)
2. `POST /api/loan/disbursals`: 원금 전액 1회 실행(`ACTIVE`)
3. `POST /api/loan/deferred-item-types`, `POST /api/loan/deferred-items`
4. `POST /api/loan/amortization-schedules/generate`
5. `POST /api/loan/events`: 중도상환/조건 변경 또는 부도/회복

## Batch 컨텍스트만 확인

```powershell
.\gradlew :loan:batch:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-batch --spring.data.redis.repositories.enabled=false --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain --max-workers=1 --no-daemon
```

## 이자 발생 Job

선행 조건:

- ACTIVE 대출
- `eir_amortization_schedules.schedule_date = accrualDate`인 행
- 해당 일자에 유효한 미수이자/이자수익 계정
- `accrualDate` 필수 Job parameter

```powershell
.\gradlew :loan:batch:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-batch --spring.data.redis.repositories.enabled=false --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.job.enabled=true --spring.batch.job.name=loanInterestAccrualJob accrualDate=2026-04-30 --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain --max-workers=1 --no-daemon
```

성공 로그는 건너뛰고 실패 로그는 재시도합니다. 한 건이라도 다시 실패하면 Job Step이 실패합니다. 현재 생성 스케줄은 월별이므로 다른 날짜는 `NOT_DUE`가 정상입니다.

## Docker

저장소 루트에서 전체 Compose를 실행하면 Loan은 8088을 사용합니다. `loan/docker-compose.yml`도 build context를 저장소 루트로 잡고 내부 `SERVER_PORT=8088`을 사용합니다.

```powershell
docker compose -f loan/docker-compose.yml build loan
```

Dockerfile은 Java 17 builder/runtime과 `:loan:api:bootJar`의 `api-0.0.1-SNAPSHOT.jar`를 사용합니다. 이미지 빌드는 패키지 다운로드가 필요할 수 있으므로 네트워크 가능한 CI에서 검증합니다.

## 주의

- `create-drop`과 Flyway 비활성화는 로컬 smoke 전용입니다.
- 별도 `application.yml`이 없으므로 실행 명령에 application name, DB, discovery, 계정 설정을 명시합니다.
- 로컬 API 8087은 단독 실행 예시입니다. 전체 Compose에서는 Governance가 8087을 사용하므로 Loan은 8088입니다.
- HTTP actor 필드는 신뢰 경계가 아닙니다. 운영에서는 인증 principal에서 서버가 주입해야 합니다.
