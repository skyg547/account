# Dependency Split Status

이 문서는 `src`와 테스트 코드의 현재 의존 관계를 기준으로, MSA 전환을 위한 모듈 분리 현황과 다음 분리 우선순위를 정리한 문서다.

## 2026-04-03 기준 실제 분리 상태

### 이미 분리된 모듈

- `master-data`
  - `basic` 패키지 전체
  - 계정과목, 거래처, 부서, 통화, 회계기간, 환율, 상품
- `governance`
  - `security`
  - `audit`
- `contracts`
  - 포트 인터페이스와 참조/명령 DTO
- `shared-kernel`
  - 공통 경계 타입
  - `Masked` 어노테이션
- `app`
  - 실행 애플리케이션
  - 나머지 도메인과 통합 어댑터

## 현재 소스 의존 특징

### 1. `master-data`는 가장 많이 참조되는 기반 모듈

아래 도메인이 `AccountSubject`, `BusinessPartner`, `Department`, `Currency`, `FiscalPeriod`를 직접 참조한다.

- `asset`
- `journal`
- `ledger`
- `income`
- `expenditure`
- `loan`
- `closing`
- `reconciliation`
- `report`
- `tax`
- `unsettled`
- `security`

즉, `master-data`는 가장 먼저 빠져야 하는 공통 기반 모듈이 맞다.

### 2. `governance`는 현재 완전 독립 모듈이 아니다

`audit.AuditLoggable`를 `journal.JournalService`가 직접 사용한다.
따라서 현재는 `app -> governance` 의존이 필요하다.

또한 `security.SystemUser`는 `master-data.Department`를 참조하므로 `governance -> master-data` 의존이 필요하다.

### 3. 테스트는 대부분 앱 통합 테스트다

현재 테스트는 개별 모듈 단위보다 `@SpringBootTest` 기반 통합 테스트가 많다.
따라서 다음 특징이 있다.

- `security/SecurityControlTest`는 실제로 `journal`과 `audit`를 함께 검증한다.
- `asset`, `loan`, `income`, `expenditure`, `closing`, `report` 테스트도 대부분 `master-data`와 `journal`을 같이 사용한다.
- 그래서 테스트는 당장 모듈별로 옮기기보다 `app` 통합 테스트로 유지하는 편이 안전하다.

## 남은 고결합 지점

### 1. `journal` 중심 직접 의존

아래 도메인이 `JournalService`, `JournalEntry`, `JournalDetail`을 직접 사용한다.

- `asset`
- `income`
- `expenditure`
- `loan`
- `closing`
- `tax`
- `reconciliation`

이 영역은 `JournalPostingPort`로 대체하는 리팩터링이 선행되어야 추가 분리가 쉽다.

### 2. `common.service.SourceDocumentService`

이 서비스는 여러 도메인 리포지토리를 직접 묶고 있다.
이런 클래스는 모듈 경계를 흐리므로, 장기적으로는 통합 어댑터 전용 모듈이나 애플리케이션 서비스 계층으로 모아야 한다.

### 3. `closing`, `loan`, `reconciliation`의 병렬 모델

- `closing`: 엔티티와 테스트/서비스 일부 시그니처가 서로 다른 세대가 섞여 있다.
- `loan`: `Loan` 모델과 `LoanContract` 모델이 공존한다.
- `reconciliation`: `RECONCILIATION_RESULTS` 계열과 `reconciliation_runs` 계열이 공존한다.

이 영역은 모듈 분리 전에 모델 정리가 먼저 필요하다.

## 다음 분리 우선순위

### 1차 후속

- `journal-ledger` 모듈
  - `journal`
  - `ledger`
  - `unsettled`

### 2차 후속

- `receivable` 모듈
  - `income`
- `payable` 모듈
  - `expenditure`

### 3차 후속

- `asset-lease`
- `loan`
- `closing-reporting`

## 권장 원칙

- JPA 엔티티 직접 참조를 계속 늘리지 않는다.
- 신규 도메인 간 호출은 가능하면 `contracts` 포트를 통해 추가한다.
- 테스트는 단기적으로 `app` 통합 테스트에 두고, 모듈이 안정화되면 모듈 테스트로 분리한다.
