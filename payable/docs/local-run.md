# payable local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

`payable`은 이제 `core/api/batch` 하위 Gradle 모듈로 분리되어 있다.

- `payable:core`: 매입채무/지급 도메인, command, UseCase, persistence adapter, local 외부 포트 adapter. HTTP/Web/Bean Validation은 포함하지 않는다.
- `payable:api`: Spring Boot HTTP 실행 진입점. Controller, 요청 DTO, Bean Validation, 응답 DTO를 소유하고 업무 판단은 core를 참조한다.
- `payable:batch`: Spring Boot Batch 실행 진입점. Job/Step 오케스트레이션만 담당하고 업무 계산은 core를 참조한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 `account > payable > core > Tasks > verification > test`를 실행한다.
3. 또는 상단 Run Configuration에서 `Payable Module Tests`를 선택한다.
4. 실행 후 `BUILD SUCCESSFUL`을 확인한다.
5. HTTP API를 직접 띄울 때는 Gradle task `:payable:api:bootRun`을 실행한다.
6. Batch 컨텍스트만 확인할 때는 Gradle task `:payable:batch:bootRun`을 실행한다.

추가된 실행 구성:

- `.run/Payable Module Tests.run.xml`

## PowerShell에서 실행하기

```powershell
.\gradlew :payable:core:test --console=plain --max-workers=1 --no-daemon
```

빠른 컴파일만 확인할 때:

```powershell
.\gradlew :payable:core:test :payable:api:compileJava :payable:batch:compileJava --console=plain --max-workers=1 --no-daemon
```

API 서버 실행:

```powershell
.\gradlew :payable:api:bootRun --console=plain --max-workers=1
```

Batch 컨텍스트 실행:

```powershell
.\gradlew :payable:batch:bootRun --console=plain --max-workers=1
```

실제 Batch Job 실행:

```powershell
.\gradlew :payable:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=payablePaymentRunJob runDate=2026-06-19 createdBy=LOCAL description=LocalPaymentRun" --console=plain --max-workers=1
```

`payablePaymentRunJob`은 `runDate`까지 지급기일이 도래한 미지급 채무를 core `PaymentUseCase`의 지급런 생성 흐름으로 넘긴다.

API 스모크만 확인하고 서버를 계속 띄우지 않을 때:

```powershell
.\gradlew :payable:api:bootRun --args="--spring.main.web-application-type=none" --console=plain --max-workers=1
```

## local profile 동작

호스트 앱에서 사용할 수 있는 대표 설정값:

```properties
account.payable.account-mapping.purchase-expense-account-code=50100
account.payable.account-mapping.input-vat-account-code=13500
account.payable.account-mapping.accounts-payable-account-code=21100
account.payable.account-mapping.cash-account-code=10100
account.payable.account-mapping.advance-account-code=13100
```

## 로컬 검증 시 자주 보는 실패

| 증상 | 확인할 지점 |
| --- | --- |
| `Vendor info missing` | master-data 테스트 더블 또는 테스트 데이터에 공급업체 코드가 있는지 확인 |
| `Account missing` | 계정 매핑 기본값에 해당하는 계정과목이 테스트 더블에 등록되어 있는지 확인 |
| `Bank account is required` | `/api/payments/execute` 또는 서비스 테스트에서 지급 계좌 값이 비어 있지 않은지 확인 |
| `지급 금액이 채무 잔액보다 클 수 없습니다.` | `Payment.amount`와 `Payable.outstandingAmount`가 맞는지 확인 |
