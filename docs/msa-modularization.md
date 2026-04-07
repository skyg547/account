# MSA Modularization Guide

이 문서는 현재 `account` 저장소를 도메인 기준으로 점진적으로 분리하기 위한 구조 기준이다.

## 현재 상태

- 현재 코드는 단일 Spring Boot 애플리케이션에 도메인 패키지가 함께 존재한다.
- 도메인 패키지는 이미 어느 정도 분리되어 있지만, 서비스 간 직접 JPA 엔티티/리포지토리 참조가 많다.
- 즉시 완전한 MSA로 나누기보다는 `멀티모듈 모놀리스 -> 계약 기반 분리 -> 서비스 분리` 순서가 안전하다.

## 이번에 적용한 구조

- `app`
  - 기존 Spring Boot 애플리케이션 코드
  - 현재 실행 진입점과 JPA 엔티티, 서비스, 컨트롤러 유지
- `master-data`
  - `basic` 패키지 추출
  - 계정과목, 거래처, 부서, 통화, 회계기간 등 공통 마스터
- `governance`
  - `security`, `audit` 패키지 추출
  - 권한, 감사, 마스킹, 승인 관련 모듈
- `contracts`
  - 도메인 간 통신에 사용할 공통 계약 객체와 포트 인터페이스
  - 이후 REST/gRPC/Event DTO의 기준 계층
- `shared-kernel`
  - 여러 서비스가 함께 써도 되는 최소 공통 개념
  - 서비스 식별자, 공통 예외/메타데이터 같은 항목 중심

## 권장 서비스 경계

### 1. master-data

- `basic`
- `security`
- 일부 `audit`

공통 마스터와 사용자/권한은 다른 도메인이 가장 많이 참조하므로 별도 서비스 후보가 강하다.

### 2. journal-ledger

- `journal`
- `ledger`
- `unsettled`
- 일부 `tax`

전표, 전기, GL/SL, 미결, 세금 전표 생성은 회계 코어이므로 한 경계로 묶는 편이 낫다.

### 3. receivable

- `income`

매출, 수금, 매칭은 독립적인 흐름이 있어 분리 가치가 높다.

### 4. payable

- `expenditure`

매입, 지급, 선급금, 지출결의는 별도 서비스 후보가 명확하다.

### 5. asset-lease

- `asset`

고정자산과 리스는 마스터/전표 포트를 통해 외부와 연동하는 구조가 적절하다.

### 6. loan

- `loan`

대출 도메인은 자체 모델과 일정 계산이 많아 독립 서비스 후보가 강하다.

### 7. closing-reconciliation-reporting

- `closing`
- `reconciliation`
- `report`
- 일부 `audit`

운영 배치와 통제, 보고는 서로 연계가 깊어 초기에는 묶고 이후 추가 분리를 검토하는 편이 적절하다.

## 먼저 끊어야 하는 의존

### 1. 마스터 조회

다수 도메인이 `AccountSubject`, `BusinessPartner`, `Department` 엔티티를 직접 참조한다.
장기적으로는 엔티티 대신 코드/참조 DTO로 바꿔야 한다.

### 2. 전표 생성

여러 도메인이 `JournalService`와 `JournalEntry`/`JournalDetail`을 직접 사용한다.
장기적으로는 `JournalPostingPort`와 전표 명령 DTO로 수렴해야 한다.

### 3. 라인리지와 원천문서 조회

`SourceDocumentService`처럼 여러 도메인을 직접 엮는 서비스는 통합 어댑터 레이어로 모으는 것이 좋다.

## 점진 전환 단계

1. 멀티모듈 구조 유지
2. `contracts`에 포트와 참조 DTO 추가
3. `app`에서 모놀리스 어댑터 구현
4. 신규 기능은 가능하면 포트를 통해 호출
5. 호출이 안정화되면 서비스별 Gradle 모듈 또는 별도 저장소로 분리

## 이번에 우선 추가한 계약

- `MasterDataQueryPort`
- `JournalPostingPort`
- 계정/거래처/부서 참조 DTO
- 전표 헤더/라인 명령 DTO
- 전표 처리 결과 DTO

## 이번에 실제로 분리한 패키지

- `basic` -> `master-data`
- `security` -> `governance`
- `audit` -> `governance`

나머지 도메인은 아직 `app`에 남아 있다.

## 참고 문서

- `docs/dependency-split-status.md`

## 폴더 기준

- 현행 DDL: `docs/db/current`
- 레거시 DDL: `docs/db/legacy`
- 구조 가이드: `docs/msa-modularization.md`
