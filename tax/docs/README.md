# tax docs

`tax` 모듈은 세금계산서 공식 증빙을 관리한다. 현재 웹 API는 매입 세금계산서(AP, `PURCHASE`) 중심으로 구현되어 있다.
기존 인덱스 문서는 내용을 보존하기 위해 [archive/README_legacy_index_2026-06-11.md](./archive/README_legacy_index_2026-06-11.md)로 이동했다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 세금계산서, 공급가액, 세액, 합계금액을 초보자 관점에서 설명한다.
2. [process-flow.md](./process-flow.md) - AP Invoice API, 서비스, 도메인 검증, 논리 취소 흐름을 정리한다.
3. [schema.md](./schema.md) - 핵심 테이블과 상태, 외부 참조 정책을 정리한다.
4. [local-run.md](./local-run.md) - IntelliJ IDEA와 Gradle에서 tax 모듈을 로컬 검증하는 방법.

## 현재 실행 전제

- `tax:core`는 업무 규칙과 persistence/local adapter를 담는 Java library 모듈이다.
- `tax:api`는 `TaxApiApplication`으로 H2 local API 서버를 실행한다.
- `tax:batch`는 `TaxBatchApplication`으로 Batch 컨텍스트를 실행하고, `taxInvoiceValidationJob`으로 매입 세금계산서 대량 검증을 수행한다.
- 로컬에서는 `.\gradlew :tax:core:test`, `.\gradlew :tax:api:bootRun`, `.\gradlew :tax:batch:bootRun`으로 검증한다.

## 핵심 코드 입구

- 인바운드 어댑터: `adapter/in/web/APInvoiceController`
- 유즈케이스 서비스: `application/service/TaxInvoiceService`
- 도메인 모델: `domain/TaxInvoice`
- 출력 포트: `application/port/out/TaxInvoicePersistencePort`
- 기술 어댑터: `adapter/out/persistence/TaxInvoicePersistenceAdapter`, `adapter/out/external/TaxInvoiceQueryAdapter`
