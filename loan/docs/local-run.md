# Loan 로컬 실행 가이드

## 전제 조건

- JDK/Gradle JVM 17
- 저장소 루트를 Gradle 프로젝트로 import
- 전표 흐름에는 Master Data의 유효 거래처·통화·계정과 Journal Ledger 구성이 필요
- 로컬 전표 모드(기본값)는 작성자와 다른 `account.loan.accounting.journal-approver-actor`가 필요합니다. 아래 예시의 `service:loan-checker`는 기계 승인자 식별자이며 자격 증명이 아닙니다. [신원 형식과 실패·재시도 흐름](process-flow.md#로컬-전표-승인과-재시도)을 확인하세요.

## 테스트와 패키징

```powershell
.\gradlew :loan:core:test :loan:api:bootJar :loan:batch:bootJar --console=plain --max-workers=1 --no-daemon
```

Flyway 검증은 core 테스트의 `LoanFlywayMigrationTest`에 포함됩니다.

## 개발 HTTP 전표 검증

`dev`에서 `account.loan.remote.enabled=true`인 경우 `HttpLoanJournalAdapter`는 다음 순서로 호출합니다.

1. 전표 초안을 생성하고 ID·전표번호·`DRAFT` 상태를 확인합니다.
2. 생성 응답에는 상세 라인이 없으므로 전표번호로 조회하여 `DRAFT`, ID, 회계일자, 통화, lineage를 확인합니다. 응답 `lines`의 양수 금액을 `side`별로 합산해 요청의 차변·대변 합계와 각각 비교합니다.
3. 검증이 통과하면 요청 actor로 승인·전기하고, 다시 조회하여 `POSTED` 상태와 같은 헤더·금액 조건을 확인합니다.

예를 들어 요청이 차변 200/대변 200인데 제공자 전표가 100/100이면 승인·전기는 호출하지 않고 `IllegalStateException`을 발생시킵니다. 누락된 라인·알 수 없는 차대변·0/음수도 실패합니다. 합산과 비교는 반올림 없이 `BigDecimal`로 수행하므로 `200`과 `200.00`은 같습니다. 생성 본문의 actor·lineage와 승인·전기 헤더의 actor는 유지됩니다.

HTTP 3xx는 `Location`을 따라가지 않고 실패합니다. 정상 처리에는 HTTP 호출 5회가 필요하며 자동 재시도는 없습니다. 예외는 호출자의 트랜잭션 실패로 전달되지만, 이미 생성한 원격 초안이나 승인·전기는 자동 롤백되지 않으므로 재시도 전에 원격 상태를 대사해야 합니다. 검증 조회와 후속 쓰기는 원격 단일 트랜잭션이 아닙니다.

```bash
bash gradlew :loan:core:test --tests '*HttpLoanJournalAdapterTest' --console=plain --max-workers=1 --no-daemon
```

이 테스트는 요청/응답 계약과 실제 loopback HTTP 리다이렉트를 확인하며 외부 Journal 서버·DB는 사용하지 않습니다.

## API 컨텍스트

```powershell
.\gradlew :loan:api:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-api --spring.data.redis.repositories.enabled=false --server.port=8087 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100 --account.loan.accounting.journal-approver-actor=service:loan-checker" --console=plain --max-workers=1 --no-daemon
```

권장 확인 순서:

1. `POST /api/loan/loans`: 계약 생성(`PENDING_DISBURSEMENT`)
2. `POST /api/loan/disbursals`: 원금 전액 1회 실행(`ACTIVE`)
3. `POST /api/loan/deferred-item-types`, `POST /api/loan/deferred-items`
4. `POST /api/loan/amortization-schedules/generate`
5. `POST /api/loan/events`: 중도상환/조건 변경 또는 부도/회복

## Batch 컨텍스트만 확인

```powershell
.\gradlew :loan:batch:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-batch --spring.data.redis.repositories.enabled=false --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100 --account.loan.accounting.journal-approver-actor=service:loan-checker" --console=plain --max-workers=1 --no-daemon
```

## 이자 발생 Job

선행 조건:

- ACTIVE 대출
- `eir_amortization_schedules.schedule_date = accrualDate`인 행
- 해당 일자에 유효한 미수이자/이자수익 계정
- `accrualDate` 필수 Job parameter

```powershell
.\gradlew :loan:batch:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-batch --spring.data.redis.repositories.enabled=false --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.job.enabled=true --spring.batch.job.name=loanInterestAccrualJob accrualDate=2026-04-30 --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100 --account.loan.accounting.journal-approver-actor=service:loan-checker" --console=plain --max-workers=1 --no-daemon
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
- 별도 `application.yml`이 없으므로 실행 명령에 application name, DB, discovery, 계정 및 로컬 전표 승인자 설정을 명시합니다. 승인자를 설정하지 않으면 API·Batch가 기동하더라도 첫 로컬 전표 호출은 실패합니다.
- 로컬 API 8087은 단독 실행 예시입니다. 전체 Compose에서는 Governance가 8087을 사용하므로 Loan은 8088입니다.
- HTTP actor 필드는 신뢰 경계가 아닙니다. 운영에서는 인증 principal에서 서버가 주입해야 합니다.
