# payable beginner guide

## 한 문장 요약

`payable`은 회사가 거래처에서 물건이나 서비스를 먼저 받고 나중에 갚아야 하는 돈, 즉 매입채무를 관리한다.

초보자 관점에서는 다음 흐름으로 보면 된다.

1. 거래처가 청구서인 매입 인보이스를 보낸다.
2. 시스템은 "나중에 이 거래처에 얼마를 갚아야 한다"는 매입채무를 만든다.
3. 지급일이 오면 지급 런이 오늘 갚을 채무를 모은다.
4. 지급 실행이 성공하면 채무 잔액을 줄이고 전표 초안을 만든다.
5. 미리 준 선급금이 있으면 채무와 상계해 잔액을 줄인다.

## 주요 업무 용어

| 용어 | 코드 모델 | 설명 |
| --- | --- | --- |
| 매입 인보이스 | `PurchaseInvoice` | 공급업체가 보낸 청구서. 공급가액, 세액, 총액, 지급기일을 가진다. |
| 매입채무 | `Payable` | 아직 지급하지 않은 외상값. `applyPayment`, `applyOffset`으로 잔액과 상태를 스스로 바꾼다. |
| 지급 런 | `PaymentRun` | 특정 날짜에 지급할 채무를 모아 실행 그룹을 만든다. |
| 지급 | `Payment` | 실제 지급 후보 또는 실행 결과. `payableId`로 정확한 채무를 가리킨다. |
| 선급금 | `AdvancePayment` | 물건을 받기 전에 먼저 준 돈. 나중에 채무와 상계할 수 있다. |

## DDD와 헥사고날 관점

`domain`은 돈과 상태가 틀어지지 않게 지키는 곳이다. 예를 들어 `Payable.applyPayment`는 지급 금액이 0보다 큰지, 잔액보다 크지 않은지 확인한 뒤 잔액과 상태를 함께 변경한다. 이런 규칙을 컨트롤러나 DB 어댑터에 흩뿌리지 않는 것이 Rich Domain Model의 핵심이다.

`application.service`는 업무 순서를 조율한다. `PurchaseService`는 거래처 검증, 중복 인보이스 확인, 채무 생성, 전표 포트 호출을 하나의 유즈케이스로 묶는다. `PaymentService`는 지급 런 생성, 지급 실행, 선급금 기록, 상계를 조율한다.

`application.port.out`은 외부 의존성을 숨기는 인터페이스다. 서비스는 JPA, 은행 API, 원장 구현을 직접 알지 않는다. 대신 `PayablePersistencePort`, `PaymentExecutionPort`, `JournalPostingPort`, `MasterDataQueryPort` 같은 포트만 바라본다.

`adapter.out`은 기술 구현이다. 현재는 JPA 저장소 어댑터, 설정 기반 계정 매핑 어댑터, 로컬 지급 실행 어댑터가 있다. 운영 은행 API를 붙일 때도 `PaymentExecutionPort` 구현체를 교체하면 애플리케이션 서비스의 업무 흐름을 유지할 수 있다.

## 외부 모듈과의 관계

- `master-data`: `MasterDataQueryPort`로 공급업체와 계정과목 존재 여부를 확인한다.
- `journal-ledger`: `JournalPostingPort`로 매입 인식, 지급, 선급금, 상계 전표 초안을 만든다.
- `contracts`: 위 두 포트의 계약을 제공한다.
- `shared-kernel`: 공통 bounded context, 공통 값 객체를 공유한다.

외부 엔티티를 직접 JPA 관계로 묶지 않고 `vendorCode`, `journalEntryId`, `sourceDocumentId` 같은 ID/Code 기반으로 참조한다. 이 방식은 모듈 경계를 단단하게 유지하고, 한 모듈의 테이블 변경이 다른 모듈 도메인 모델까지 전파되는 일을 줄인다.

## 처음 볼 때 체크할 파일

1. `PurchaseService`: 매입 인보이스를 받아 채무와 전표를 만드는 흐름.
2. `PaymentService`: 지급 런, 지급 실행, 선급금, 상계 흐름.
3. `Payable`: 지급 또는 상계가 들어왔을 때 잔액과 상태를 바꾸는 도메인 규칙.
4. `LocalPaymentExecutionAdapter`: 로컬에서 은행 API 없이 멱등 지급 결과를 만드는 테스트용 어댑터.
5. `ConfiguredPayableAccountMappingAdapter`: 계정과목 코드를 설정값으로 바꾸는 어댑터.
