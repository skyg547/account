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