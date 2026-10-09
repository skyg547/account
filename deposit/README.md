# Deposit Service (수신/예금)

`deposit` 모듈은 예금 계좌 개설, 입출금, 예금 부채 계정 매핑 흐름을 담당합니다. 현재 코드는 `deposit:core`, `deposit:api`, `deposit:batch` 하위 프로젝트로 나뉘며, API와 Batch는 Spring Boot 실행 앱으로 기동할 수 있습니다.

## 모듈 구조

- `deposit:core`: 예금 계좌 도메인, 계좌 상태 전이 머신(`DepositAccountStateMachine`), 일할 이자 계산기(`DepositInterestAccrualCalculator`), 중도/만기해지 정산 엔진(`DepositTerminationSettlementCalculator`), 계좌 개설 유즈케이스 및 JPA 영속성 어댑터를 포함합니다.
- `deposit:api`: HTTP API 실행 앱. `DepositApplication`을 main class로 사용합니다.
- `deposit:batch`: Batch 컨텍스트 실행 앱. `depositAccountIntegrityJob`으로 활성 예금 계좌의 잔액/이자율/유효기간 무결성을 점검합니다.

## 주요 코어 도메인 계산 엔진

1. **DepositDayCountConvention**: 원화 예금 365일 기준(`ACTUAL_365`) 및 외화 예금 360일 기준(`ACTUAL_360`) 일수 계산 기준.
2. **DepositInterestAccrualCalculator**: 약정이율 기반 매일 미지급 이자 일할 계산 (\( \text{Balance} \times \text{Rate} \times \frac{\text{Days}}{\text{BaseDays}} \)) 정밀 계산기.
3. **DepositTerminationSettlementCalculator**: 만기해지 및 중도해지 패널티 비율 적용, 이자소득세 14% + 지방소득세 1.4% (총 15.4%) 원천징수 세금 및 최종 실지급액 산출 엔진.
4. **DepositAccountStateMachine**: 계좌 상태 전이(`ACTIVE` -> `SUSPENDED` / `DORMANT` -> `CLOSED`) 규칙 검증기.

## 로컬 실행

초보자 기준으로 먼저 H2와 로컬 어댑터를 사용해 단독 실행을 확인합니다. 로컬 어댑터는 `master-data`와 `journal-ledger`가 없어도 초기입금 흐름을 확인하기 위한 학습용 어댑터입니다.

```powershell
.\gradlew :deposit:api:bootRun --args="--spring.profiles.active=local --server.port=8087 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain

.\gradlew :deposit:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.deposit.local-adapters.enabled=true --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

IntelliJ에서는 `.run/Deposit API bootRun.run.xml`, `.run/Deposit Batch Context.run.xml` 실행 구성을 사용할 수 있습니다.

실제 Spring Batch Job 실행:

`asOfDate`는 재실행과 감사 기준일이므로 필수입니다. 누락하거나 `yyyy-MM-dd` 형식이 아니면 배치를 시작하지 않습니다.

```powershell
.\gradlew :deposit:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=depositAccountIntegrityJob asOfDate=2026-06-19" --console=plain --max-workers=1
```

## 업무/데이터 흐름

1. 사용자가 예금 계좌 개설 요청을 보냅니다. API는 요청 DTO를 core `OpenAccountCommand`로 변환합니다.
2. `OpenAccountCommand`가 필수 코드, 통화 코드 대문자 정규화, 초기입금/금리 음수 금지를 검증합니다.
3. `DepositService`가 계좌번호를 만들고 `DepositAccount` 도메인 객체를 활성 상태로 생성합니다.
4. 초기입금액이 있으면 도메인 메서드로 잔액을 증가시킵니다.
5. `DepositAccountPersistencePort`를 통해 계좌를 저장합니다.
6. `DepositAccountMappingPort`에서 현금 계정과 예금부채 계정을 해석합니다.
7. `MasterDataQueryPort`로 계정과목 존재를 검증합니다.
8. `JournalPostingPort`로 초기입금 전표 초안을 생성합니다.

### 입출금과 전표 outbox

`POST /api/deposits/accounts/{accountNumber}/deposit` 또는 `/withdraw`에 양수 `amount`를 보내면, core가 매 시도마다 계좌를 다시 읽고 잔액 규칙을 검사합니다. 입금은 **현금 차변 / 예금부채 대변**, 출금은 **예금부채 차변 / 현금 대변**으로 같은 금액의 두 줄을 기록합니다. 거래마다 새 계보 ID와 outbox 멱등 키를 만들므로 한 계좌의 여러 입출금을 구별할 수 있습니다. 계좌 잔액과 `deposit_outbox`의 `PENDING` 이벤트는 같은 로컬 DB 트랜잭션으로 저장됩니다. 저장 실패 시 둘 다 롤백하며, 낙관 잠금 충돌 시 실패한 트랜잭션이 끝난 뒤 최신 잔액으로 다시 검증합니다. 잔액 부족 출금은 재시도 중에도 실패할 수 있습니다. Journal의 금액 계약에 맞춰 **정확히 소수 둘째 자리까지 표현되는 금액**만 허용합니다(`1.0000`은 `1.00`으로 처리, `0.0001`은 거부). 현재 입출금은 환율·기준통화 금액 공급 경로가 없으므로 **KRW 계좌만 처리**하며, 외화 계좌는 잔액 변경 전에 거부합니다.

HTTP 성공은 **잔액과 전표 요청의 로컬 커밋**을 뜻합니다. 외부 Journal 초안 생성은 커밋 후 기존 outbox 릴레이가 수행하므로, 릴레이가 완료되기 전에는 원장 대사가 일시적으로 차이 날 수 있습니다. 예를 들어 로컬 H2 프로필에서 계좌를 개설한 뒤 `{"amount": "100.00"}`으로 입금하면 잔액이 100.00 증가하고 해당 거래의 outbox 이벤트가 생성됩니다. 릴레이가 활성화되어 있으면 이후 이벤트 상태가 `PUBLISHED`로 바뀝니다.

회계일자가 마감되어 Journal이 전표를 거부하면 **이미 커밋된 잔액은 자동으로 되돌리지 않습니다.** 릴레이는 오류와 재시도 횟수를 outbox에 남기며, 반복 실패로 `FAILED`가 된 이벤트는 원인을 확인하고 마감 정책에 맞게 기간을 재개방하거나 승인된 조정 전표 절차를 결정한 뒤 재전송해야 합니다. 임의로 원본 outbox를 지우거나 회계일자를 바꾸면 계보가 끊깁니다. 현재 입출금 HTTP 요청에는 멱등 키가 없으므로 동일 요청을 다시 보내면 새 거래로 처리됩니다. `deposit_transactions` 영속 이력, 운영 PostgreSQL 트랜잭션, 실제 Journal 마감·대사는 이 변경에서 검증하지 않았습니다.

운영에서는 로컬 어댑터 대신 master-data와 journal-ledger의 실제 어댑터를 연결해야 합니다.

## 검증 명령

```powershell
.\gradlew :deposit:core:test :deposit:api:bootJar :deposit:batch:bootJar --console=plain --max-workers=1 --no-daemon
```

입출금 회귀 검증은 저장소 루트에서 다음 명령을 사용합니다.

```powershell
.\gradlew :deposit:core:test :deposit:api:test --offline --no-daemon --console=plain
```
