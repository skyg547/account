# Tax Module (세무)

## 1. 비즈니스 목적

`tax` 모듈은 세금계산서 데이터를 관리하며, 특히 매입(`PURCHASE`) 증빙 정합성을 보장해
지출결의/AP 지급 같은 후속 모듈이 신뢰할 수 있는 기준 데이터를 사용하도록 지원합니다.

## 2. 헥사고날 구조 요약

- `domain`
  - `TaxInvoice`: 금액 합계 검증(`validateAmounts`)과 타입 보조 로직(`isPurchaseType`)을 포함한 핵심 도메인
- `application/port/in`
  - `TaxInvoiceUseCase`: 외부 진입점이 의존하는 인바운드 포트
- `application/service`
  - `TaxInvoiceService`: 유즈케이스 구현, 거래처 검증, 타입 경계(`PURCHASE`) 강제
- `application/port/out`
  - `TaxInvoicePersistencePort`: 영속성 기술(JPA 등) 은닉용 아웃바운드 포트
- `adapter/in/web`
  - `APInvoiceController`: REST API 어댑터
- `repository`
  - `TaxInvoiceRepository`: JPA 인터페이스

## 3. 핵심 비즈니스 규칙

- 금액 정합성: `supplyAmount + taxAmount = totalAmount`
- AP 경계: AP API는 `PURCHASE` 타입만 허용
- 거래처 검증: `BusinessPartner`가 존재해야 생성/수정 가능

## 4. API

- `POST /api/ap/invoices`
- `GET /api/ap/invoices/{id}`
- `GET /api/ap/invoices/issue-id/{issueId}`
- `GET /api/ap/invoices?startDate=...&endDate=...`
- `PUT /api/ap/invoices/{id}`
- `DELETE /api/ap/invoices/{id}`

## 5. 개발 초보자 실행 순서

1. 모듈 문서 읽기
   - `tax/docs/beginner-guide.md`
   - `tax/docs/process-flow.md`
   - `tax/docs/schema.md`
2. 코드 읽기
   - `domain -> application/service -> adapter/in/web` 순서
3. 검증
   - `./gradlew :tax:build --console=plain`
   - `./gradlew :tax:test --console=plain`

## 6. 변경 시 체크리스트

- 타입 검증(`PURCHASE`)이 생성/조회/수정/삭제 전 구간에서 유지되는가?
- 금액 검증이 도메인에서 반드시 호출되는가?
- 컨트롤러가 포트(`UseCase`)에만 의존하는가?
- API 변경 시 `tax/docs` 문서 3종을 함께 갱신했는가?
