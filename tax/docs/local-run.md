# tax local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

`tax`는 현재 독립 실행 애플리케이션이 아니므로 `bootRun`으로 바로 띄우는 모듈이 아니다. 모듈 단위에서는 테스트와 컴파일로 업무 흐름을 검증한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 `account > tax > Tasks > verification > test`를 실행한다.
3. 또는 상단 Run Configuration에서 `Tax Module Tests`를 선택한다.
4. 실행 후 `BUILD SUCCESSFUL`을 확인한다.

추가된 실행 구성:

- `.run/Tax Module Tests.run.xml`

## PowerShell에서 실행하기

```powershell
.\gradlew :tax:test --console=plain --max-workers=1 --no-daemon
```

빠른 컴파일만 확인할 때:

```powershell
.\gradlew :tax:compileJava --console=plain --max-workers=1 --no-daemon
```

## HTTP API를 로컬에서 호출하려면

현재 tax 모듈에는 `SpringBootApplication`이 없다. 따라서 아래 중 하나가 필요하다.

1. 기존 호스트 애플리케이션이 tax 패키지를 컴포넌트 스캔하도록 구성한다.
2. 테스트 전용 또는 통합 실행용 Boot 앱 모듈을 별도로 만든다.
3. 컨트롤러 테스트를 추가해 `@WebMvcTest` 또는 `@SpringBootTest`로 웹 계약을 검증한다.

## 예시 요청

```http
POST /api/ap/invoices
Content-Type: application/json
```

```json
{
  "issueId": "TAX-2026-0001",
  "type": "PURCHASE",
  "issueDate": "2026-06-01",
  "businessPartnerCode": "BP001",
  "supplyAmount": 10000,
  "taxAmount": 1000,
  "totalAmount": 11000
}
```

취소 요청:

```http
DELETE /api/ap/invoices/1?reason=wrong-issue
X-User-ID: local-user
```

## 자주 보는 실패

| 증상 | 확인할 지점 |
| --- | --- |
| `AP Invoice는 PURCHASE 타입만 생성할 수 있습니다.` | 요청 `type`이 `PURCHASE`인지 확인 |
| `거래처를 찾을 수 없습니다` | master-data 테스트 더블 또는 호스트 앱 데이터에 거래처 코드가 있는지 확인 |
| 공급가액과 세액의 합이 맞지 않음 | `supplyAmount + taxAmount`가 `totalAmount`와 같은지 확인 |
| 취소 실행자와 사유 필수 오류 | `X-User-ID` 헤더와 `reason` 파라미터가 비어 있지 않은지 확인 |
