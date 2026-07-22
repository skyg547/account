# Contracts 초보자 가이드

## 1. 공용 결재 양식 비유

회사에서 자산 부서가 지출 부서에 리스료 지급을 요청한다고 가정합니다. 자산 부서가 지출 부서의
DB 테이블을 직접 수정하면 승인, 예산, 감사 흐름을 우회하게 됩니다. 대신 정해진 결재 양식인
`LeasePaymentResolutionCommand`를 작성하고 `LeasePaymentResolutionPort`로 요청합니다.

포트는 콘센트 모양, Command는 콘센트에 전달하는 표준 입력이라고 생각하면 됩니다. 실제 전기를
공급하는 구현은 각 업무 모듈의 Adapter입니다.

## 2. 계약이 스켈레톤 코드가 아닌 이유

인터페이스만 보면 구현이 없어 보이지만 이 모듈의 책임은 **컴파일 가능한 경계 정의**입니다.
완성 여부는 다음 세 부분을 함께 확인해야 합니다.

1. 호출 모듈이 Port만 의존한다.
2. 제공 모듈에 실제 Adapter 구현이 있다.
3. 제공 모듈 Domain/Application이 업무 규칙을 수행한다.

세 번째 단계가 없는 단순 빈 구현은 스켈레톤입니다. 반대로 계약 모듈에 DB 처리나 업무 계산을
넣는 것도 잘못입니다.

## 3. 현재 데이터 흐름 예시

전표 생성:

```text
Payable/Receivable/Closing Service
  -> JournalEntryCommand 생성
  -> JournalPostingPort.createDraftEntry()
  -> journal-ledger Adapter
  -> journal-ledger 전표 도메인 검증
  -> 저장 및 JournalPostingResult 반환
```

기준일 기준정보 조회:

```text
Closing FX 평가일
  -> MasterDataQueryPort.findAccountSubjectAt(code, valuationDate)
  -> master-data MonolithMasterDataQueryAdapter
  -> validFrom <= valuationDate <= validTo JPA 조회
  -> AccountSubjectRef 반환
```

현재 조회와 과거 기준일 조회는 결과가 다를 수 있습니다. 결산은 반드시 평가일 기준 버전을
사용해야 합니다.

## 4. 변경할 때 확인할 질문

- 이 필드는 모든 호출자와 제공자가 같은 뜻으로 이해하는가?
- String 코드가 허용하는 값이 명확한가?
- 금액은 `BigDecimal`인가?
- List/Set은 외부에서 변경할 수 없게 복사했는가?
- 기준일을 현재 날짜로 조용히 바꾸지 않는가?
- 원격 호출이 필요하면 timeout, 오류 의미, 버전 호환을 Adapter에서 정의했는가?
- 기존 생성자/JSON 필드 변경이 하위 호환성을 깨지 않는가?

## 5. 남은 TODO를 읽는 방법

코드의 `@todo`는 빈 구현을 허용한다는 뜻이 아닙니다. 현재 호환 경로와 목표 구조 사이의
마이그레이션 조건을 적습니다. 예를 들어 기준일 조회 default 제거 TODO는 모든 어댑터가 실제
SCD2 조회를 구현했다는 증거가 갖춰져야 완료할 수 있습니다.