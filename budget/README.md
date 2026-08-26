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

## 로컬 단독 실행

`budget:api`를 `local` 프로파일로 실행할 때는 fail-closed 보안 가드로 인해 최소 32바이트 이상의 `AUTH_JWT_SECRET` (또는 `--auth.jwt.secret`)을 필수로 제공해야 합니다.

```powershell
.\gradlew.bat :budget:api:bootRun --args="--spring.profiles.active=local --auth.jwt.secret=ephemeral_test_secret_for_local_development_32bytes"
```

자세한 실행 옵션, 환경 변수 주입, Batch 실행 및 트러블슈팅은 [docs/local-run.md](docs/local-run.md)를 참고하세요.

## 중요한 경계

기존 `expenditure-resolution`의 월별 `Budget`은 기존 데이터와 호출 흐름을 위한 호환
모델입니다. 이번 Issue는 새 `budget_*` 테이블을 사용하며 기존 `budgets` 테이블을
dual-write하거나 자동 이관하지 않습니다. 단일 write owner 전환과 예약/확정/해제 연동은
데이터 대사와 보상 설계가 필요한 후속 #17 범위입니다.

자세한 흐름은 [docs/README.md](docs/README.md)를 참고하세요.
