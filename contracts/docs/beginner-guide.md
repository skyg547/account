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

## 6. 원격 회계기간 전이를 다시 확인하는 방법

Closing이 기간을 다시 열 때 Master Data에 먼저 `OPEN`을 저장하면, 응답 유실이나 로컬
승인 저장 실패 후에는 승인되지 않은 기간에 전표가 입력될 수 있습니다. 새 계약은
`FiscalPeriodTransitionCommand`의 동일한 `operationId`와 원본 상태·목표 상태·actor를
Master Data에 준비시키고, 영수증을 조회하여 재시도 결과를 확인합니다. `PREPARED`는 아직
기간 행에 저장된 상태가 바뀌지 않았고 `APPLIED`는 Master Data의 상태 변경이 완료된
뜻입니다. 단, 닫기 준비 중 조회 결과는 전표를 막기 위해 `CLOSED`로 보입니다. 재개 준비
중에는 `OPEN`으로 보이지 않습니다. 재개가 `APPLIED`여도 전표 허용용 조회는 `CLOSED`를
유지합니다. Closing의 승인·달력·감사 기록이 커밋된 뒤 `CONFIRMED`가 되어야 `OPEN`으로
보입니다.

닫기 준비에 필요한 로컬 증거가 만료되면 권한 있는 담당자가 같은 작업 ID와 별도 취소
actor로 준비를 취소합니다. `CANCELLED` 영수증은 더 이상 적용할 수 없는 종료 결과입니다.
이미 적용된 작업을 취소로 되돌릴 수는 없습니다.

로컬 개발에서는 저장소 루트에서 다음 검사를 실행합니다.

```bash
./gradlew :contracts:test --console=plain --max-workers=1 --no-daemon
```

성공 시 `BUILD SUCCESSFUL`이 출력됩니다. 이 검사는 계약 필드 검증과 영수증의 전체 명령
일치 여부를 확인합니다. 실제 응답 유실·DB 복구는 Closing과 Master Data의 통합 테스트로
확인해야 합니다. 자세한 적용 순서는 [process-flow.md](./process-flow.md)를 참조하세요.
