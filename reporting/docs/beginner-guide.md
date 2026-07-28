# Reporting Beginner Guide

`reporting`은 원장 계정 잔액을 보고 라인으로 모아 재무제표를 만드는 모듈입니다.

## 핵심 용어

- `ReportLineMapping`: 계정 코드가 어느 보고 라인에 들어갈지 정하는 규칙입니다.
- `SCD2`: 과거 기준일의 보고서도 당시 규칙으로 다시 재현할 수 있게 매핑의 유효 시작일과 종료일을 저장하는 방식입니다.
- `FinancialStatement`: 보고서 헤더와 라인 금액을 가진 도메인 모델입니다.
- `Snapshot`: 확정된 보고서를 나중에 비교할 수 있도록 DB에 고정 저장한 결과입니다.
- `DisclosureNoteMart`: 재무제표 주석 번호별 금액을 만기, 금리, 통화, 리스크 관점으로 다시 정리한 공시 데이터입니다.
- `RegulatoryFiling`: 주석 마트 금액을 감독기관 제출 서식의 필드로 변환하고 접수 영수증까지 남긴 제출 이력입니다.

## 기본 흐름

1. 기준일의 GL 잔액을 `LoadLedgerPort`로 조회합니다.
2. 기준일에 유효한 `RPT_LINE_MAPPING` 행을 `LoadReportLineMappingPort`로 조회합니다.
3. 같은 보고 라인의 계정 잔액을 `BigDecimal`로 합산합니다.
4. 전기 FINAL 스냅샷을 조회해 비교 금액을 채웁니다.
5. 보고서를 FINAL 상태로 확정하고 `RPT_SNAPSHOT_*` 테이블에 저장합니다.
6. 주석 마트가 필요하면 FINAL 스냅샷의 note 번호가 있는 라인을 `RPT_DISCLOSURE_NOTE_MART`에 전개합니다.
7. 감독보고 제출 시 `RPT_REGULATORY_REPORT_MAPPING`으로 주석 마트 금액을 제출 필드에 매핑하고 `RPT_REGULATORY_FILING`에 접수 결과를 저장합니다.

## 모듈 구조

`reporting`은 한 모듈처럼 보이지만 실제 코드는 세 하위 모듈로 나뉩니다.

- `reporting:core`: 보고서 생성, 스냅샷, 주석 마트, 감독보고 제출 도메인과 서비스.
- `reporting:api`: HTTP 컨트롤러와 `ReportingApiApplication` 실행 앱.
- `reporting:batch`: 월말 보고서 생성을 호출하는 배치 인바운드 어댑터와 `ReportingBatchApplication` 실행 앱.

현재 `reporting:core`는 library 모듈이고, `reporting:api`와 `reporting:batch`는 standalone Boot 앱입니다. 로컬에서 먼저 실행 흐름을 확인할 때는 `account.reporting.persistence.mode=memory`를 사용하면 journal-ledger나 운영 DB 없이 샘플 원장 잔액으로 보고서 흐름을 볼 수 있습니다.
## API DTO를 따로 두는 이유

초보자 입장에서는 도메인 객체와 API 응답 객체가 비슷해 보여도 역할이 다르다.

- 도메인 객체는 업무 규칙과 상태 전이를 지킨다. 예를 들어 `FinancialStatement.finalizeStatement()`는 항목 없는 보고서를 확정하지 못하게 막는다.
- API response DTO는 외부에 보여줄 JSON 필드를 고정한다. 화면이나 외부 시스템은 이 DTO 계약을 보고 연동한다.
- 따라서 Controller가 도메인 객체를 그대로 반환하지 않고 DTO로 바꾸면, 나중에 도메인 내부 구조를 바꿔도 HTTP 계약을 더 안정적으로 유지할 수 있다.