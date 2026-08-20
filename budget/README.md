# Budget Control

`budget`은 월별 부서·계정 예산을 편성하고 승인한 뒤, 전용과 집행을 통제하는 독립
bounded context입니다. `core`가 금액·상태 규칙과 트랜잭션 유즈케이스를 소유하고,
`api`와 `batch`는 각각 HTTP 및 연말마감 실행 진입점만 담당합니다.

## 모듈

- `budget:core`: `BudgetPlan`, `BudgetTransfer`, `BudgetExecution` Aggregate, use case,
  output port, JPA outbound adapter
- `budget:api`: 입력 검증, command 변환, 응답 DTO, standalone Spring Boot API
- `budget:batch`: 필수 `fiscalYear` 파라미터를 받는 `budgetYearEndCloseJob`

## 핵심 규칙

- 금액은 현재 회계 저장 계약과 같은 `DECIMAL(19,2)`이며 무음 반올림하지 않습니다.
- 계획은 `DRAFT → APPROVED → CLOSED` 순서로만 진행합니다.
- 전용은 같은 `YYYYMM` 계획 사이에서만, 집행은 계획과 같은 월의 집행일에만 가능하며
  승인 상태와 가용액을 함께 확인합니다.
- 전용 승인 시 두 계획을 ID 오름차순으로 잠가 교착 가능성을 줄입니다.
- 회계연도별 영구 제어 행을 먼저 잠그므로 마감과 생성·승인·전용·집행이 경쟁해도
  `CLOSED` 이후 새 변경이 남지 않습니다.
- 전용 request key와 집행 source lineage는 사전 생성된 잠금 shard에서 첫 요청부터
  직렬화하므로 동시 재시도도 같은 결과를 반환합니다.
- API는 Auth가 서명한 JWT를 자체 검증하고 역할을 확인하며 감사 actor는 JWT `sub`만 사용합니다.
- Batch는 마감 규칙을 계산하지 않고 core use case만 호출합니다.

## 중요한 경계

## 로컬 실행 방법 (Local Run Guide)

`budget:api`는 Fail-Closed 보안 원칙에 따라 토큰 검증용 비밀키(`AUTH_JWT_SECRET`)가 환경변수로 주입되지 않으면 안전하게 기동을 차단합니다 (`application.yml`의 의도된 설계).

### 1. 빌드
```powershell
.\gradlew.bat :budget:api:bootJar
```

### 2. 환경변수 설정 및 실행
- `AUTH_JWT_SECRET`: **필수 (Mandatory)**. HMAC-SHA256(HS256) 규격에 따라 **32바이트(256bit) 이상**의 개발용 비밀키를 주입해야 합니다.
- `AUTH_JWT_ISSUER`: 선택 사항 (기본값: `auth-service`).
- ⚠️ **주의**: 실제 운영 비밀키나 평문 시크릿을 저장소 코드나 커밋에 절대 포함하지 마십시오.

**PowerShell:**
```powershell
$env:AUTH_JWT_SECRET = "<32바이트_이상의_로컬_개발용_임의_비밀키>"
java -jar budget\api\build\libs\account-budget-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

**Bash / Linux:**
```bash
export AUTH_JWT_SECRET="<32바이트_이상의_로컬_개발용_임의_비밀키>"
java -jar budget/api/build/libs/account-budget-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

자세한 흐름 및 개발 Compose 가이드는 [docs/local-run.md](docs/local-run.md)와 [docs/README.md](docs/README.md)를 참고하세요.
