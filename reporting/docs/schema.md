# Reporting Schema

## RPT_LINE_MAPPING

SCD2 보고 라인 매핑 테이블입니다. 한 행은 하나의 `account_code`가 하나의 보고 라인에 연결되는 규칙을 뜻합니다. 같은 `statement_type`, `line_code`, `display_order`, 유효기간을 가진 여러 행은 애플리케이션에서 하나의 `ReportLineMapping`으로 그룹핑됩니다.

주요 컬럼:

- `statement_type`: `BALANCE_SHEET`, `INCOME_STATEMENT`
- `line_code`: 보고 라인 코드
- `label`: 보고 라인명
- `account_code`: 원장 계정 코드
- `note_number`: 공시 주석 번호
- `line_level`: 표시 계층
- `display_order`: 보고서 표시 순서
- `valid_from`, `valid_to`: SCD2 유효 기간

## RPT_SNAPSHOT_HEADER

FINAL 재무제표 헤더입니다. `statement_type`, `base_date`, `status`는 유니크하게 관리하여 같은 기준일의 FINAL 제출본을 교체 저장합니다.

## RPT_SNAPSHOT_DETAIL

FINAL 재무제표 라인 상세입니다. `current_amount`, `previous_amount`는 `DECIMAL(19, 4)`로 저장하고, 헤더 삭제 시 함께 삭제됩니다.

## RPT_REGULATORY_SUBMISSION

확정 재무제표 스냅샷을 감독보고 제출본으로 등록한 이력을 저장합니다.
같은 `statement_type`, `base_date` 조합은 제출할 때마다 `submission_version`을 1씩 증가시키며, 2차 제출부터는 정정 사유가 필요합니다.

주요 컬럼:

- `submission_id`: 외부 제출본 식별자
- `statement_id`: 제출 기준이 된 `RPT_SNAPSHOT_HEADER.statement_id`
- `statement_type`, `base_date`: 보고서 종류와 기준일
- `submission_version`: 제출 버전
- `submitted_by`, `submitted_at`: 제출 요청자와 제출 시각
- `correction_reason`: 정정 제출 사유
- `status`: 현재는 제출 준비 완료 상태인 `READY`
- `validation_messages`: 검증 메시지 보관 영역

## RPT_DISCLOSURE_NOTE_MART

확정 재무제표 스냅샷의 주석 번호별 금액을 만기, 금리, 통화, 리스크 관점으로 전개한 공시 마트입니다.
재생성 시 같은 `statement_type`, `base_date` 조합의 기존 엔트리를 삭제하고 새 마트 엔트리로 교체합니다.

주요 컬럼:

- `entry_id`, `mart_id`: 주석 마트 엔트리와 생성 실행 식별자
- `statement_id`, `statement_type`, `base_date`: 원천 재무제표 스냅샷 식별자
- `note_number`, `note_category`: 주석 번호와 공시 범주
- `source_line_code`, `source_line_label`: 보고서 라인 추적 키
- `maturity_bucket`, `rate_type`, `currency_code`, `risk_category`: 공시 분석 차원
- `current_amount`, `previous_amount`: 당기/전기 비교 금액
- `generated_by`, `generated_at`: 생성자와 생성 시각

## Migration

- `reporting/core/src/main/resources/db/migration/V60__reporting_persistence_schema.sql`
- 기본 BS/IS 매핑 seed를 포함합니다.
- `reporting/core/src/main/resources/db/migration/V61__reporting_regulatory_submission.sql`
- 감독보고 제출본 버전 이력 테이블을 생성합니다.
- `reporting/core/src/main/resources/db/migration/V62__reporting_disclosure_note_mart.sql`
- 주석 마트 테이블과 조회/분류 인덱스를 생성합니다.
