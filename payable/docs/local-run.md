# payable local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

`payable`은 현재 독립 실행 애플리케이션이 아니므로 `bootRun`으로 바로 띄우는 모듈이 아니다. 모듈 단위에서는 테스트와 컴파일로 업무 흐름을 검증한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 `account > payable > Tasks > verification > test`를 실행한다.
3. 또는 상단 Run Configuration에서 `Payable Module Tests`를 선택한다.
4. 실행 후 `BUILD SUCCESSFUL`을 확인한다.

추가된 실행 구성:

- `.run/Payable Module Tests.run.xml`

## PowerShell에서 실행하기

```powershell
.\gradlew :payable:test --console=plain --max-workers=1 --no-daemon
```

빠른 컴파일만 확인할 때:

```powershell
.\gradlew :payable:compileJava --console=plain --max-workers=1 --no-daemon
```

## HTTP API를 로컬에서 호출하려면

현재 payable 모듈에는 `SpringBootApplication`이 없다. 따라서 아래 중 하나가 필요하다.

1. 기존 호스트 애플리케이션이 payable 패키지를 컴포넌트 스캔하도록 구성한다.
2. 테스트 전용 또는 통합 실행용 Boot 앱 모듈을 별도로 만든다.
3. 컨트롤러 테스트를 추가해 `@WebMvcTest` 또는 `@SpringBootTest`로 웹 계약을 검증한다.

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
