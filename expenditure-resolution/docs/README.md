# expenditure-resolution docs

`expenditure-resolution` 모듈은 지출결의, 예산 차감, 세금계산서 연결, 지급(AP Payment), 고정자산/리스 연계를 다룬다.

읽기 순서:

1. [beginner-guide.md](/C:/Users/skyg547/IdeaProjects/account/expenditure-resolution/docs/beginner-guide.md)
2. [process-flow.md](/C:/Users/skyg547/IdeaProjects/account/expenditure-resolution/docs/process-flow.md)
3. [schema.md](/C:/Users/skyg547/IdeaProjects/account/expenditure-resolution/docs/schema.md)

## 패키지 구조 (헥사고날 아키텍처)

```
expenditure/
├── domain/                          # 핵심 도메인 모델 (순수 Java)
│   ├── ExpenditureResolution        # Aggregate Root – 상태전이 메서드 포함
│   ├── ExpenditureDetail            # 지출 상세 라인
│   ├── APPayment                    # AP 지급 엔티티 – 도메인 상태전이 포함
│   ├── APPaymentStatus              # 지급 상태 Enum (PENDING/COMPLETED/FAILED/PARTIALLY_APPLIED)
│   ├── Budget                       # 예산 엔티티 – useBudget() 도메인 로직 포함
│   └── ExpenditureResolutionStatus  # 결의서 상태 Enum
├── application/
│   ├── port/
│   │   ├── in/                      # UseCase 인터페이스 (인바운드 포트)
│   │   │   ├── ExpenditureResolutionUseCase
│   │   │   └── APPaymentUseCase
│   │   └── out/                     # 영속성 포트 (아웃바운드 포트)
│   │       ├── ExpenditureResolutionPersistencePort
│   │       ├── APPaymentPersistencePort
│   │       └── BudgetPersistencePort
│   └── service/                     # UseCase 구현체 (Port/Out 의존)
│       ├── ExpenditureResolutionService
│       ├── APPaymentService
│       └── BudgetService
├── adapter/
│   ├── in/
│   │   ├── web/                     # REST 컨트롤러 (UseCase 인터페이스 의존)
│   │   │   ├── ExpenditureController
│   │   │   └── APPaymentController
│   │   └── contract/                # 타 모듈 contracts 포트 구현체
│   │       ├── BudgetControlAdapter
│   │       └── MonolithLeasePaymentResolutionAdapter
│   └── out/
│       └── persistence/             # JPA Repository 래핑 어댑터
│           ├── ExpenditureResolutionPersistenceAdapter
│           ├── APPaymentPersistenceAdapter
│           └── BudgetPersistenceAdapter
├── repository/                      # Spring Data JPA Repository 인터페이스
└── dto/                             # 요청/응답 DTO
```
