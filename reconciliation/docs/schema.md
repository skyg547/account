# reconciliation schema

## 핵심 ERD

```mermaid
erDiagram
    RECONCILIATION_UNITS ||--o{ RECONCILIATION_RULES : defines
    RECONCILIATION_UNITS ||--o{ RECONCILIATION_RUNS : executes
    RECONCILIATION_RUNS ||--o{ RECONCILIATION_DIFFERENCES : detects
    RECONCILIATION_RUNS ||--o{ RECONCILIATION_STAGE_RESULTS : checkpoints
    DIFFERENCE_REASON_CODES ||--o{ RECONCILIATION_DIFFERENCES : classifies

    RECONCILIATION_UNITS {
        Long id PK
        String name
        String description
        String frequency
        String reconciliation_type
        String criteria_json
        boolean is_active
        String audit_user
    }

    RECONCILIATION_RULES {
        Long id PK
        Long reconciliation_unit_id
        String name
        String rule_definition_json
        String tolerance_type
        BigDecimal tolerance_value
        Integer priority
        boolean is_active
    }

    RECONCILIATION_RUNS {
        Long id PK
        Long reconciliation_unit_id
        LocalDate reconciliation_date
        LocalDateTime run_start_time
        LocalDateTime run_end_time
        String status
        Long total_items_source
        BigDecimal total_amount_source
        Long total_items_target
        BigDecimal total_amount_target
        Long matched_items_count
        BigDecimal matched_amount
        Long unmatched_items_count
        BigDecimal unmatched_amount
        String run_by
    }

    RECONCILIATION_DIFFERENCES {
        Long id PK
        Long reconciliation_run_id
        String difference_type
        BigDecimal amount_expected
        BigDecimal amount_actual
        BigDecimal difference_amount
        String source_item_ref
        String target_item_ref
        Long reason_code_id
        Long adjustment_journal_entry_id
        String status
        String assigned_to_user
        LocalDateTime sla_due_date
        LocalDateTime resolved_at
        String resolved_by
    }

    DIFFERENCE_REASON_CODES {
        Long id PK
        String code
        String name
        boolean is_adjustable
        boolean is_active
    }

    RECON_EXTERNAL_STAGE_RECORD {
        Long id PK
        String unit_id
        String stage_code
        LocalDate reconciliation_date
        String product_code
        String currency_code
        String legal_entity_code
        BigDecimal amount
    }
```

## 상태

| 모델 | 상태 | 의미 |
| --- | --- | --- |
| `ReconciliationRun` | `RUNNING` | 대사 실행 중 |
| `ReconciliationRun` | `SUCCESS` | 대사 실행 완료 |
| `ReconciliationRun` | `FAILED` | 실행 중 예외 발생 |
| `ReconciliationRun` | `PARTIAL` | 부분 완료 확장 지점 |
| `ReconciliationDifference` | `PENDING` | 차이 처리 대기 |
| `ReconciliationDifference` | `ASSIGNED` | 담당자 배정 |
| `ReconciliationDifference` | `IN_REVIEW` | 검토 중 |
| `ReconciliationDifference` | `RESOLVED` | 해결 완료 |
| `ReconciliationDifference` | `IGNORED` | 업무적으로 무시 처리 |

## 기준정보 보존 정책

- `ReconciliationUnit` 삭제 API는 실제 삭제가 아니라 `isActive=false`로 바꾼다.
- `DifferenceReasonCode` 삭제 API도 `isActive=false`로 바꾼다.
- `ReconciliationRule` 삭제 API는 현재 물리 삭제이며, 대사 실행 이력 추적을 위해 논리 비활성화로 전환해야 한다. 코드에 `@todo`로 남겼다.

## 외부 참조

- `ExternalReconSnapshotPort`: 외부 원천 단계 집계 조회.
- `JournalQueryPort`: 대상 원장 집계와 조정 전표 존재 확인.
- `JournalPostingPort`: 조정 전표 초안 생성.

조정 전표 생성 시 현재 source document id가 시간 기반이다. 장애 재시도 시 중복 조정 전표가 생길 수 있어 run/difference 기준 멱등 키로 바꿔야 한다.
