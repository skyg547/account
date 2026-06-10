# receivable beginner guide

## 한 문장 요약

`receivable`은 회사가 고객에게 물건이나 서비스를 팔고 아직 받지 못한 돈, 즉 매출채권을 관리한다.

초보자 관점에서는 다음 흐름으로 보면 된다.

1. 고객에게 매출 인보이스를 발행한다.
2. 시스템은 "이 고객에게 얼마를 받아야 한다"는 매출채권을 만든다.
3. 고객이 입금하면 수납 내역을 기록한다.
4. 입금이 어떤 인보이스를 갚은 것인지 자동 또는 수동으로 매칭한다.
5. 매칭된 금액만큼 채권 잔액을 줄이고 전표 초안을 만든다.

## 주요 업무 용어

| 용어 | 코드 모델 | 설명 |
| --- | --- | --- |
| 매출 인보이스 | `SalesInvoice` | 고객에게 발행한 청구서. 공급가액, 세액, 총액, 수금기일을 가진다. |
| 매출채권 | `Receivable` | 아직 받지 못한 외상값. `applyCollection`으로 잔액과 상태를 스스로 바꾼다. |
| 수납 | `Collection` | 고객이 입금한 돈. 매칭 전에는 어느 채권을 갚은 돈인지 불명확할 수 있다. |
| 매칭 | `CollectionAllocation` | 수납액을 특정 채권에 얼마 배분했는지 남기는 감사 이력이다. |
| 미매칭 | `UnmatchedCollection` | 자동 매칭에 실패해 사람이 확인해야 하는 입금이다. |

## DDD와 헥사고날 관점

`domain`은 돈과 상태가 틀어지지 않게 지키는 곳이다. `Receivable.applyCollection`은 수납 금액이 0보다 큰지, 채권 잔액보다 크지 않은지 확인한 뒤 잔액과 상태를 함께 바꾼다. `Collection.applyAllocation`은 입금액 중 이미 매칭된 금액을 누적하고 남은 미배분 금액을 계산한다.

`application.service`는 업무 순서를 조율한다. `SalesService`는 고객 검증, 인보이스 저장, 채권 생성, 매출 인식 전표 생성을 묶는다. `CollectionService`는 수납 기록, 자동 매칭, 수동 매칭, 채권과 수납의 양쪽 잔액 갱신, 전표 생성을 묶는다.

`application.port.out`은 외부 의존성을 숨기는 인터페이스다. 서비스는 JPA 저장소나 전표 시스템 구현을 직접 알지 않는다. `ReceivablePersistencePort`, `CollectionMatchingPolicyPort`, `JournalPostingPort`, `MasterDataQueryPort` 같은 포트만 사용한다.

`adapter.out`은 기술 구현이다. 현재는 JPA 저장소 어댑터, 설정 기반 계정 매핑 어댑터, 참조번호 우선 자동 매칭 정책 어댑터가 있다. 자동 매칭 알고리즘을 바꾸고 싶다면 `CollectionMatchingPolicyPort` 구현체를 교체하거나 확장하는 방식이 헥사고날 경계에 맞다.

## 외부 모듈과의 관계

- `master-data`: `MasterDataQueryPort`로 고객과 계정과목 존재 여부를 확인한다.
- `journal-ledger`: `JournalPostingPort`로 매출 인식, 수납 인식, 채권 반제 전표 초안을 만든다.
- `contracts`: 위 두 포트의 계약을 제공한다.
- `shared-kernel`: 공통 bounded context, 공통 값 객체를 공유한다.

## 처음 볼 때 체크할 파일

1. `SalesService`: 매출 인보이스를 받아 채권과 전표를 만드는 흐름.
2. `CollectionService`: 입금, 자동 매칭, 수동 매칭, 전표 생성 흐름.
3. `ReferenceFirstCollectionMatchingPolicy`: 참조번호, 만기일, 금액 순으로 후보를 고르는 자동 매칭 정책.
4. `Receivable`: 수납 금액이 들어왔을 때 잔액과 상태를 바꾸는 도메인 규칙.
5. `CollectionAllocation`: 부분 매칭 후 양쪽 잔액을 남기는 감사 이력.
