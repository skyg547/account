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

- 대출 모델은 `Loan` 단일 도메인 모델을 기준으로 통합되어 있습니다.
  - 일일 이자 발생, 원천 문서 조회, 상각 스케줄 엔트리는 모두 `Loan`을 참조합니다.
  - 별도 계약 엔티티와 저장소는 제거되었습니다.
- 자동 분개는 `JournalUseCase`로 생성한 뒤 승인/전기 호출까지 수행합니다.
- `LoanJournalPostingFlowTest`는 실제 `JournalEntryService`와 `PostingService`를 연결해 대출 실행 전표가 `POSTED`로 수렴하고 GL/SL 엔트리 저장 및 원장 갱신 호출이 발생하는지 검증합니다.
- 대출 회계 계정코드는 `account.loan.accounting.*` 설정으로만 받습니다.
  - 설정 누락 시 코드 기본값으로 대체하지 않고 자동 전표 생성 전에 실패합니다.
  - 필수 키는 `cash-account-code`, `loan-receivable-account-code`, `deferred-asset-account-code`, `recognized-income-account-code`, `accrued-interest-receivable-account-code`, `interest-income-account-code`입니다.
- DB-backed H2/실DB 스키마 검증은 현재 테스트 범위 밖이며 운영 검증 시 별도 보강 대상입니다.
