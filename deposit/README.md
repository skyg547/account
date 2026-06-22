# Deposit Service (수신/예금)

`deposit` 모듈은 예금 계좌 개설, 초기 입금 인식, 예금 부채 계정 매핑 흐름을 담당합니다. 현재 코드는 `deposit:core`, `deposit:api`, `deposit:batch` 하위 프로젝트로 나뉘며, API와 Batch는 Spring Boot 실행 앱으로 기동할 수 있습니다.

## 모듈 구조

- `deposit:core`: 예금 계좌 도메인, 계좌 개설 유즈케이스, 초기입금 전표 생성 포트, JPA 영속성 어댑터.
- `deposit:api`: HTTP API 실행 앱. `DepositApplication`을 main class로 사용합니다.
- `deposit:batch`: Batch 컨텍스트 실행 앱. `depositAccountIntegrityJob`으로 활성 예금 계좌의 잔액/이자율/유효기간 무결성을 점검합니다.

## 로컬 실행

초보자 기준으로 먼저 H2와 로컬 어댑터를 사용해 단독 실행을 확인합니다. 로컬 어댑터는 `master-data`와 `journal-ledger`가 없어도 초기입금 흐름을 확인하기 위한 학습용 어댑터입니다.

```powershell
.\gradlew :deposit:api:bootRun --args="--spring.profiles.active=local --server.port=8087 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain

.\gradlew :deposit:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

IntelliJ에서는 `.run/Deposit API bootRun.run.xml`, `.run/Deposit Batch Context.run.xml` 실행 구성을 사용할 수 있습니다.

실제 Spring Batch Job 실행:

```powershell
.\gradlew :deposit:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=depositAccountIntegrityJob asOfDate=2026-06-19" --console=plain --max-workers=1
```

## 업무/데이터 흐름

1. 사용자가 예금 계좌 개설 요청을 보냅니다.
2. `DepositService`가 계좌번호를 만들고 `DepositAccount` 도메인 객체를 활성 상태로 생성합니다.
3. 초기입금액이 있으면 도메인 메서드로 잔액을 증가시킵니다.
4. `DepositAccountPersistencePort`를 통해 계좌를 저장합니다.
5. `DepositAccountMappingPort`에서 현금 계정과 예금부채 계정을 해석합니다.
6. `MasterDataQueryPort`로 계정과목 존재를 검증합니다.
7. `JournalPostingPort`로 초기입금 전표 초안을 생성합니다.

운영에서는 로컬 어댑터 대신 master-data와 journal-ledger의 실제 어댑터를 연결해야 합니다.

## 검증 명령

```powershell
.\gradlew :deposit:core:test :deposit:api:bootJar :deposit:batch:bootJar --console=plain --max-workers=1 --no-daemon
```
