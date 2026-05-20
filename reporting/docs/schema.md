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

## Migration

- `reporting/core/src/main/resources/db/migration/V60__reporting_persistence_schema.sql`
- 기본 BS/IS 매핑 seed를 포함합니다.
