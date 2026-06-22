# receivable docs

`receivable` 모듈은 매출 인보이스, 매출채권, 수납, 자동/수동 매칭, 미매칭 수납 관리를 담당한다.
기존 인덱스 문서는 내용을 보존하기 위해 [archive/README_legacy_index_2026-06-10.md](./archive/README_legacy_index_2026-06-10.md)로 이동했다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 매출채권과 수납 매칭 업무를 초보자 관점에서 설명한다.
2. [process-flow.md](./process-flow.md) - API, 서비스, 도메인, 자동 매칭 정책, 전표 포트 흐름을 정리한다.
3. [schema.md](./schema.md) - 핵심 테이블, 상태, 계정 매핑 설정을 코드 기준으로 정리한다.
4. [local-run.md](./local-run.md) - IntelliJ IDEA와 Gradle에서 receivable 모듈을 로컬 검증하는 방법.

## 현재 실행 전제

- `receivable:core`는 업무 규칙과 persistence/local adapter를 담는 Java library 모듈이다.
- `receivable:api`는 `ReceivableApiApplication`으로 H2 local API 서버를 실행한다.
- `receivable:batch`는 `ReceivableBatchApplication`으로 Batch 컨텍스트를 실행하고, `receivableAutoMatchingJob`으로 수납 자동 매칭을 실행한다.
- 로컬에서는 `.\gradlew :receivable:core:test`, `.\gradlew :receivable:api:bootRun`, `.\gradlew :receivable:batch:bootRun`으로 검증한다.

## 핵심 코드 입구

- 인바운드 어댑터: `adapter/in.web/SalesController`, `adapter/in.web/CollectionController`
- 유즈케이스 서비스: `application/service/SalesService`, `application/service/CollectionService`
- 도메인 모델: `domain/SalesInvoice`, `domain/Receivable`, `domain/Collection`, `domain/CollectionAllocation`, `domain/UnmatchedCollection`
- 아웃바운드 포트: `application/port/out/*PersistencePort`, `ReceivableAccountMappingPort`, `CollectionMatchingPolicyPort`
- 기술 어댑터: `adapter/out/persistence`, `adapter/out/policy/ConfiguredReceivableAccountMappingAdapter`, `adapter/out/policy/ReferenceFirstCollectionMatchingPolicy`
