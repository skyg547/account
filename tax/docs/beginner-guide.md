# tax beginner guide

## 한 문장 요약

`tax`는 회사 거래의 공식 증빙인 세금계산서를 저장하고, 공급가액과 세액, 합계금액이 맞는지 확인한다.

초보자 관점에서는 세금계산서를 공식 영수증으로 보면 된다.

- 공급가액: 물건이나 서비스 자체 가격
- 세액: 부가가치세
- 합계금액: 공급가액과 세액을 더한 최종 금액

이 세 금액이 맞지 않으면 회계 전표, 부가세 신고, 거래처 정산이 모두 틀어질 수 있다.

## 현재 구현 범위

도메인 모델 `TaxInvoice`는 `SALES`와 `PURCHASE` 타입을 표현할 수 있다. 하지만 현재 웹 컨트롤러 이름과 경로는 `APInvoiceController`, `/api/ap/invoices`이고, 서비스도 `createAPInvoice`에서 `PURCHASE` 타입만 허용한다. 따라서 현재 API 문서는 매입 세금계산서 중심으로 읽어야 한다.

## DDD와 헥사고날 관점

`TaxInvoice` 도메인은 생성과 수정 시 `validateAmounts()`를 호출한다. 공급가액과 세액의 합이 합계금액과 다르면 예외를 던진다. 이 규칙은 컨트롤러나 DB가 아니라 도메인에 들어 있어야 증빙 정합성이 한 곳에서 지켜진다.

`TaxInvoiceService`는 업무 순서를 조율한다. 매입 세금계산서 타입인지 확인하고, `MasterDataQueryPort`로 거래처가 존재하는지 확인한 뒤, `TaxInvoicePersistencePort`로 저장한다.

`APInvoiceController`는 `tax:api`에 있고 HTTP 요청을 DTO로 받는다. `TaxInvoiceRequestDto`는 Bean Validation으로 필수값과 0 이상 금액을 먼저 검사한 뒤 `TaxInvoiceCommand`로 변환한다. `tax:core`의 `TaxInvoiceService`는 DTO를 모르고 command만 받아 거래처 검증, 금액 정합성, 취소 정책을 처리한다.

`TaxInvoiceQueryAdapter`는 다른 모듈이 세금계산서 참조 정보를 조회할 때 사용하는 출력 어댑터다. 참조값에는 `type`과 `status`가 포함되고, `TaxInvoiceRef.purchase()`, `active()`, `usableForPurchaseSettlement()`로 소비 모듈이 매입/유효 상태를 명시적으로 판단할 수 있다.

## 취소가 삭제가 아닌 이유

세금계산서는 공식 증빙이므로 물리 삭제하면 감사 추적이 끊긴다. 현재 `DELETE /api/ap/invoices/{id}`는 실제 DB delete가 아니라 `TaxInvoice.cancel(actor, reason)`을 호출해 상태를 `CANCELLED`로 바꾸고 취소자, 사유, 시각을 남긴다.

취소된 세금계산서는 조회 자체를 막지 않는다. 다만 지출결의나 AP 지급처럼 새 업무 처리에 연결하는 흐름에서는 `ACTIVE` 상태만 허용한다.

## 처음 볼 때 체크할 파일

1. `APInvoiceController`: 매입 세금계산서 등록, 조회, 수정, 취소 API.
2. `TaxInvoiceService`: PURCHASE 타입 제한, 거래처 검증, 저장 흐름.
3. `TaxInvoice`: 금액 검증과 논리 취소 도메인 규칙.
4. `tax/api/.../TaxInvoiceRequestDto`: 요청 필수값과 금액 0 이상 검증, core `TaxInvoiceCommand` 변환.
5. `TaxInvoiceQueryAdapter`: 다른 모듈이 세금계산서 참조를 조회하는 경계.
