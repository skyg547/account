# payable docs

`payable` 모듈은 매입 인보이스, 매입채무, 지급 실행, 선급금, 선급금 상계를 관리한다.
기존 인덱스 문서는 내용을 보존하기 위해 [archive/README_legacy_index_2026-06-10.md](./archive/README_legacy_index_2026-06-10.md)로 이동했다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 매입채무와 지급 업무를 초보자 관점에서 설명한다.
2. [process-flow.md](./process-flow.md) - API, 서비스, 도메인, 전표 포트가 어떤 순서로 움직이는지 정리한다.
3. [schema.md](./schema.md) - 핵심 테이블, 상태, 계정 매핑 설정을 코드 기준으로 정리한다.
4. [local-run.md](./local-run.md) - IntelliJ IDEA와 Gradle에서 payable 모듈을 로컬 검증하는 방법.

## 현재 실행 전제

- `payable:core`는 업무 규칙과 persistence/local adapter를 담는 Java library 모듈이다.
- `payable:api`는 `PayableApiApplication`으로 H2 local API 서버를 실행한다.
- `payable:batch`는 `PayableBatchApplication`으로 Batch 컨텍스트를 실행하고, `payablePaymentRunJob`으로 지급런을 생성한다.
- 로컬에서는 `.\gradlew :payable:core:test`, `.\gradlew :payable:api:bootRun`, `.\gradlew :payable:batch:bootRun`으로 검증한다.

## 핵심 코드 입구

- 인바운드 어댑터: `adapter/in.web/PurchaseController`, `adapter/in.web/PaymentController`
- 유즈케이스 서비스: `application/service/PurchaseService`, `application/service/PaymentService`
- 도메인 모델: `domain/PurchaseInvoice`, `domain/Payable`, `domain/Payment`, `domain/PaymentRun`, `domain/AdvancePayment`
- 아웃바운드 포트: `application/port/out/*PersistencePort`, `PayableAccountMappingPort`, `PaymentExecutionPort`
- 기술 어댑터: `adapter/out/persistence`, `adapter/out/policy/ConfiguredPayableAccountMappingAdapter`, `adapter/out/execution/LocalPaymentExecutionAdapter`
