# Contracts 코드 스키마

이 모듈에는 DB 테이블이 없습니다. 여기서 스키마는 Java Port와 전달 객체의 공개 필드/메서드를
뜻합니다.

## 전표 계약

### `JournalEntryCommand`

- `slipDate`, `accountingDate`: 필수
- `description`, `entryType`, `currencyCode`
- `exchangeRate`: `BigDecimal`
- `createdBy`, `auditUser`
- `lineageSourceType`, `lineageSourceId`
- `lines`: 비어 있지 않은 불변 `List<JournalLineCommand>`

### `JournalLineCommand`

- `drcrType`: `DEBIT` 또는 `CREDIT`로 정규화
- `accountCode`: 공백 불가
- `amount`: 필수 `BigDecimal`
- `baseAmount`: 선택 `BigDecimal`
- `departmentCode`, `businessPartnerCode`, `detailDescription`

`JournalSide` enum이 이미 있지만 기존 생성자 호환을 위해 `drcrType` 필드는 아직 String입니다.
향후 모든 JSON/호출자를 함께 마이그레이션할 때 enum으로 통일해야 합니다.

## 기준정보 계약

- `AccountSubjectRef`: 코드, 이름, 미결/고정자산 여부, 정상잔액 방향
- `BusinessPartnerRef`: 코드, 이름, 거래처 유형, 활성 여부
- `DepartmentRef`: 코드, 이름, 부서 유형
- `FiscalPeriodRef`: 회계연도/기간, 시작일/종료일, 마감 상태
- `MasterDataQueryPort`: 현재 및 기준일 조회

### 회계기간 전이 영수증 (`FiscalPeriodControlPort`)

- `FiscalPeriodTransitionCommand`: `operationId`(재시도마다 동일한 고유 ID), 양수
  `fiscalPeriodId`, `expectedStatus`, `targetStatus`, `auditUser`를 모두 담습니다.
  ID와 actor는 안전한 식별자 문자(`A-Z`, `a-z`, `0-9`, `.`, `_`, `@`, `-`)만 허용하며
  각각 최대 128자와 42자입니다. 상태는 `FiscalPeriodClosingStatus` enum이고 양쪽 상태는
  달라야 합니다.
- `prepareClosingTransition(command)`: 제공자가 원본 명령 전체를 영속화한 뒤
  `PREPARED` 영수증을 돌려줍니다. 기존 상태가 예상 상태와 다르거나 동일 ID의 명령 내용이
  달라지면 충돌입니다. 재시도는 저장된 **동일 명령**의 영수증을 돌려줍니다. 기간 행의
  저장 상태는 `apply` 전까지 그대로지만, 닫기 준비 중 기간 조회(`findFiscalPeriodById`,
  `findFiscalPeriod` 및 공개 GET)는 전표 허용을 막기 위해 `CLOSED`로 투영합니다. 재개
  준비는 `OPEN`으로 투영하지 않습니다.
- `applyClosingTransition(operationId)`: 준비된 명령의 예상 상태에서 목표 상태로 변경하고
  영수증을 `APPLIED`로 바꾸는 작업이 제공자 DB에서 원자적으로 끝나야 합니다.
  응답이 유실되어도 동일 ID 재호출은 이미 적용된 영수증을 돌려줍니다. 재개 적용으로
  저장 상태가 `OPEN`이 되어도 전표 허용용 조회는 확정 전까지 `CLOSED`입니다.
- `confirmClosingTransition(operationId)`: Closing의 승인·달력·감사 확정이 로컬 DB에
  커밋된 뒤 호출합니다. 제공자는 `APPLIED → CONFIRMED`를 영속화한 뒤 재개의
  전표 허용용 조회에서 `OPEN`을 노출합니다. 이미 `CONFIRMED`이면 동일 영수증을
  반환합니다. `PREPARED`나 `CANCELLED`는 확정할 수 없습니다.
- `cancelClosingTransition(operationId, cancellationActor)`: 권한을 검증한 복구 담당자가
  준비된 전이를 종료합니다. 취소 담당자는 원래 명령의 `auditUser`와 별도로 서명·검증·
  감사되며 같은 안전한 문자와 42자 제한을 따릅니다. 제공자는 기간/영수증 잠금 아래
  `PREPARED → CANCELLED`만 허용합니다. `APPLIED`/`CONFIRMED` 취소는 거절하고 반복 취소는 기존
  `CANCELLED` 영수증을 돌려줍니다. `CANCELLED`의 적용은 거절합니다. 영수증은 원본
  명령 필드를 그대로 보존하고 취소 담당자는 별도 감사 사건에 남깁니다.
- `findTransitionReceipt(operationId)`: 응답 유실 후 조회합니다. `matches(command)`로
  기간 ID, 전·후 상태, actor까지 원래 로컬 명령과 대조해야 합니다. `APPLIED`는 원격
  저장 상태 적용을 뜻하며 Closing의 로컬 승인 완료나 전표 허용을 대신 증명하지
  않습니다. `CONFIRMED`는 원격 전표 허용 조회가 확정된 결과입니다. `CANCELLED`는
  원격 상태를 변경하지 않은 종료 결과이므로 동일 ID로 다시 적용할 수 없습니다.

기존 `updateClosingStatus`는 호환용입니다. 새 메서드를 구현하지 않은 기존 어댑터의 기본
구현은 `UnsupportedOperationException`으로 실패하므로 회복 흐름이 조용히 우회되지
않습니다. 이 계약은 제공자의 트랜잭션이나 저장소를 구현하지 않습니다.

정상잔액 방향은 `DEBIT/CREDIT`만 허용합니다. 기존 4개 인자 생성자는 호환상 `DEBIT`를
사용하지만 부채/자본/수익 계정 오류를 막기 위해 제거 TODO가 있습니다.

## 조회 결과 계약

`JournalSummary`, `JournalDetailSummary`, `LedgerBalanceSummary`는 기존 projection/직렬화
호환을 위해 mutable JavaBean 형태입니다. 새 코드에서 도메인 엔티티로 사용하면 안 됩니다.
불변 DTO 전환은 모든 setter 기반 매핑 사용처를 확인한 뒤 진행합니다.

## 변경 호환성 체크

- record 필드 순서와 JSON 이름
- enum/코드 값 추가·삭제
- 생성자 호환
- nullable 여부
- 금액 scale/rounding 책임
- 날짜와 timezone 의미
- 호출 모듈 컴파일
- 제공 Adapter 및 계약 테스트
