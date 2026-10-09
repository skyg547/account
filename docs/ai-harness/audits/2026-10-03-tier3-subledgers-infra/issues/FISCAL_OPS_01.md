# [tax] 세금계산서 금액이 소수 셋째 자리까지 검증을 통과해 저장 시 암묵 반올림됨

- **Issue ID:** FISCAL_OPS_01
- **Priority:** P2
- **Module:** tax
- **Area / category:** core / financial
- **Labels:** module:tax, area:core, type:financial, priority:p2, agent-loop, status:draft, spec-driven

## Code reference
- tax/core/src/main/java/com/ho/account/tax/domain/TaxInvoice.java:39
- tax/core/src/main/java/com/ho/account/tax/domain/TaxInvoice.java:75
- tax/core/src/main/java/com/ho/account/tax/domain/TaxInvoice.java:89
- tax/api/src/main/java/com/ho/account/tax/api/dto/TaxInvoiceRequestDto.java:33
- tax/core/src/main/resources/db/tax-migration/V1__tax_baseline.sql:7

## Problem statement
`TaxInvoice.validateAmounts()`는 null과 `supplyAmount + taxAmount == totalAmount`만 확인한다. HTTP DTO에도 금액의 소수 자릿수 제약이 없다. 따라서 DB의 `NUMERIC(19,2)`로 손실 없이 표현할 수 없는 금액이 생성·수정 유즈케이스를 통과한다. 이는 부가세율 자체를 10%로 강제해야 한다는 주장이 아니다. 확인된 결함은 요청 금액과 저장 금액의 정밀도 계약 불일치다.

## Reproduction / evidence
`createAPInvoice`은 DTO 금액을 그대로 `TaxInvoice.create()`에 전달한 뒤 저장한다(`TaxInvoiceService.java:40-54`). 예를 들어 공급가액 `1.005`, 세액 `0`, 합계 `1.005`는 도메인 합계 검사를 통과한다. 세 금액 컬럼은 `NUMERIC(19,2)`여서 DB 저장 시 공급가액과 합계가 `1.01`로 바뀔 수 있다. 독립 반올림 결과가 합계와 어긋나는 입력은 DB CHECK 제약(`V1__tax_baseline.sql:17-19`)에서 뒤늦게 거부된다. 기존 `TaxInvoiceTest.java:11-35`는 두 자리 정상 합계와 불일치 합계만 검사한다.

## Financial / architectural impact
공식 매입 세금계산서의 공급가액·세액·합계가 접수된 값과 달라질 수 있고, 일부 입력은 도메인에서 승인된 뒤 영속화 시 실패한다. 금액 무손실 검증이 DB에 의존하여 core 직접 호출과 HTTP 호출의 결과가 달라진다.

## Proposed solution
정확한 수정 allowlist (5파일): `tax/core/src/main/java/com/ho/account/tax/domain/TaxInvoice.java`, `tax/api/src/main/java/com/ho/account/tax/api/dto/TaxInvoiceRequestDto.java`, `tax/core/src/test/java/com/ho/account/tax/domain/TaxInvoiceTest.java`, `tax/api/src/test/java/com/ho/account/tax/api/TaxAmountValidationTest.java`(신규), `tax/docs/process-flow.md`. core에서 세 금액의 `NUMERIC(19,2)` 무손실 표현 가능 여부와 전체 정밀도를 생성·수정 전에 검증하고, API Bean Validation에서 같은 경계를 400으로 반환한다. `1.000`처럼 값 손실 없이 두 자리로 표현 가능한 입력의 허용 여부를 테스트와 문서에 명시한다.

## Acceptance criteria
- [ ] 생성과 수정에서 무손실로 저장할 수 없는 금액(`1.005` 등)을 영속화 전에 거부한다.
- [ ] HTTP 요청은 해당 금액에 4xx를 반환하고, 유효한 두 자리 금액의 공급가액+세액=합계 계약을 유지한다.
- [ ] core/API 회귀 테스트가 정상·초과 자릿수·독립 반올림 시 합계 불일치 사례를 검증한다.

## Test gap and verification limits
현재 도메인 테스트는 소수 셋째 자리와 DB 표현 가능성을 검사하지 않는다. 검증 명령: `./gradlew :tax:core:test :tax:api:test --offline --no-daemon --console=plain`. 이 감사에서는 실제 PostgreSQL 반올림/제약 실행을 재현하지 않았으므로 DB별 변환 양상은 해당 환경에서 추가 확인한다.
