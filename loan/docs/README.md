# Loan Module Docs

`loan` 모듈은 대출 계약 생성, 실행, 선수수료 이연, EIR 상각 스케줄 생성, 중도상환 재계산, 일별 이자 발생분 인식을 담당합니다.

## 문서 목록

- [process-flow.md](./process-flow.md): 대출 업무가 어떤 순서로 처리되는지 설명합니다.
- [schema.md](./schema.md): 주요 엔티티와 관계, 저장되는 핵심 필드를 정리합니다.
- [beginner-guide.md](./beginner-guide.md): 초보자가 대출 회계를 이해할 수 있도록 용어와 실제 사용 순서를 설명합니다.

## 이 모듈이 하는 일

1. 대출 계약의 기본 조건을 등록합니다.
2. 대출 실행 시 자동 분개를 생성합니다.
3. 수수료나 부대비용을 이연자산으로 잡고 기간별로 상각합니다.
4. 유효이자율(EIR) 기준 상각 스케줄을 계산합니다.
5. 중도상환이나 조건 변경 시 재계산 이력을 남깁니다.
6. 일별 이자 발생분을 별도 로그와 함께 기록합니다.

## 핵심 진입점

- `LoanController`
- `LoanService`
- `InterestAccrualService`
- `EIRCalculator`

## 현재 구현 기준에서 먼저 알아둘 점

- 대출 모델이 `Loan`과 `LoanContract` 두 종류로 나뉘어 있습니다.
  - `LoanService`의 주요 API는 `Loan`을 사용합니다.
  - `InterestAccrualService`, `LoanSourceDocumentProvider`는 `LoanContract`를 사용합니다.
- 자동 분개는 일부 경로에서 `JournalService`를 거치지 않고 저장소에 직접 저장됩니다.
- 계정과목 코드가 일부 하드코딩되어 있습니다.
  - 대출 실행: `101000`, `131000`
  - 이연 초기 인식: `101000`, 이연유형의 자산계정
  - EIR 상각: `131000`, `401000`, `171000`
  - 일별 이자 발생: `11501`, `41101`
