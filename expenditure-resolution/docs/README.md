# expenditure-resolution docs

`expenditure-resolution` 모듈은 지출결의, 예산 차감, 세금계산서 연결, 지급(AP Payment), 고정자산/리스 연계를 다룬다.

읽기 순서:

1. [beginner-guide.md](./beginner-guide.md)
2. [process-flow.md](./process-flow.md)
3. [schema.md](./schema.md)
4. [local-run.md](./local-run.md)

## 패키지 구조

```text
expenditure-resolution/
├── core
│   └── src/main/java/com/ho/account/expenditure
│       ├── domain/                  # ExpenditureResolution, ExpenditureDetail, APPayment, Budget
│       ├── application/port/in      # UseCase와 Command
│       ├── application/port/out     # Persistence Port
│       ├── application/service      # 지출결의, 예산, AP 지급 업무 흐름
│       ├── adapter/in/contract      # contracts 포트 구현체
│       ├── adapter/out/persistence  # JPA Repository 래핑 어댑터
│       └── resolution/infrastructure/local # local profile 외부 포트 더블
├── api
│   └── src/main/java/.../api
│       ├── adapter/in/web           # REST Controller와 DTO assembler
│       └── dto                      # HTTP 요청/응답 DTO
└── batch
    └── src/main/java/.../batch      # Batch Job/Step 실행 구성
```

## 핵심 경계

- `core`는 HTTP DTO와 Controller를 소유하지 않는다.
- `api` 요청 DTO는 `ExpenditureResolutionCommand`, `APPaymentCommand`로 변환한 뒤 core UseCase를 호출한다.
- `core`는 master-data 내부 Repository/Entity가 아니라 `MasterDataQueryPort`로 부서, 계정, 거래처를 확인한다.
- `Budget`은 `deptCode`, `accountCode` 값을 저장해 기준정보 Aggregate와 생명주기를 분리한다.
- Batch 모듈은 Job/Step/Tasklet orchestration만 담당한다.

## 로컬 실행

```powershell
.\gradlew :expenditure-resolution:core:test :expenditure-resolution:api:test --console=plain --max-workers=1
.\gradlew :expenditure-resolution:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
.\gradlew :expenditure-resolution:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

자세한 IntelliJ/H2 실행 순서는 [local-run.md](./local-run.md)를 따른다.