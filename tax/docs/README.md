# tax docs

`tax` 모듈은 세금계산서 공식 증빙을 관리한다. 현재 웹 API는 매입 세금계산서(AP, `PURCHASE`) 중심으로 구현되어 있다.
기존 인덱스 문서는 내용을 보존하기 위해 [archive/README_legacy_index_2026-06-11.md](./archive/README_legacy_index_2026-06-11.md)로 이동했다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 세금계산서, 공급가액, 세액, 합계금액을 초보자 관점에서 설명한다.
2. [process-flow.md](./process-flow.md) - AP Invoice API, 서비스, 도메인 검증, 논리 취소 흐름을 정리한다.
3. [schema.md](./schema.md) - 핵심 테이블과 상태, 외부 참조 정책을 정리한다.
4. [local-run.md](./local-run.md) - IntelliJ IDEA와 Gradle에서 tax 모듈을 로컬 검증하는 방법.

## 현재 실행 전제

- `tax`는 현재 `java-library` 모듈이다.
- 별도 `SpringBootApplication`이나 `:tax:bootRun` 태스크가 없다.
- 로컬에서는 IntelliJ Gradle 실행 구성 또는 `.\gradlew :tax:test`로 모듈 단위 검증을 수행한다.
- API를 실제 HTTP로 호출하려면 tax 컴포넌트를 스캔하는 호스트 Spring Boot 애플리케이션이 필요하다.

## 핵심 코드 입구

- 인바운드 어댑터: `adapter/in/web/APInvoiceController`
- 유즈케이스 서비스: `application/service/TaxInvoiceService`
- 도메인 모델: `domain/TaxInvoice`
- 출력 포트: `application/port/out/TaxInvoicePersistencePort`
- 기술 어댑터: `adapter/out/persistence/TaxInvoicePersistenceAdapter`, `adapter/out/external/TaxInvoiceQueryAdapter`
