# 🗄️ Internal Audit 데이터 스키마 (Schema)

Flyway V60–V62 마이그레이션 스크립트에 의해 관리되는 내부감사 모듈 전용 테이블 명세입니다.

---

## 1. 테이블 목록

| 테이블명 | 영문 설명 | 주요 컬럼 |
| :--- | :--- | :--- |
| `rcm_process` | 업무 프로세스 | `process_id`, `process_name`, `description`, `owner_id` |
| `rcm_risk` | 통제 대상 리스크 | `risk_id`, `process_id`, `risk_description`, `impact_level`, `likelihood` |
| `rcm_control_activity` | 통제 활동 | `control_id`, `risk_id`, `control_type`, `frequency`, `owner_id` |
| `eval_design` | 설계 평가 | `evaluation_id`, `control_id`, `result`, `evaluator_id` |
| `eval_operating` | 운영 평가 | `evaluation_id`, `control_id`, `sample_size`, `exception_count`, `result` |
| `operating_evaluation_jpa_entity_evidence_file_paths` | 운영 평가 증빙 목록 | `operating_evaluation_jpa_entity_evaluation_id`, `evidence_file_paths` |
| `eval_deficiency` | 결함 및 개선 과제 | `deficiency_id`, `evaluation_id`, `description`, `remediation_plan`, `status` |
| `internal_audit_log` | V61 append-only 감사 | `actor`, `action`, `aggregate_type`, `aggregate_id`, `idempotency_key`, `details_json` |
| `internal_audit_command_receipt` | V62 명령 재시도 결과 | `idempotency_key`, `fingerprint_version`, `fingerprint`, `snapshot_version`, `snapshot_json` |
| `internal_audit_key_lock` | V62 최초 키 경합 잠금 | `bucket` (0–255) |

---

## 2. 스키마 격리 원칙
- 본 모듈은 타 도메인(재무, 대출 등)의 테이블을 직접 참조하지 않으며, `rcm_*`, `eval_*`, 운영평가 증빙 및 `internal_audit_*` 테이블을 단독 소유합니다.
- 실제 정의는 [로컬 migration](../core/src/main/resources/db/migration/)과 [PostgreSQL migration](../core/src/main/resources/db/postgresql-migration/)에서 확인합니다.

## V62 명령 receipt와 키 잠금

`internal_audit_command_receipt`는 전체 `idempotency_key`를 PK로 가지며 fingerprint 버전/SHA-256과
최초 결과 snapshot 버전/JSON을 저장한다. 예약은 snapshot 버전0/NULL이고 완료는 양수 버전/JSON이다.
업무 저장, V61 감사 append, receipt 완료는 같은 트랜잭션이며 예약만 커밋하지 않는다.
`internal_audit_key_lock`의 256개 미리 만든 bucket row는 최초 키 경합과 direct append를 직렬화한다.
기존 감사 row와 V61은 불변이며 legacy row를 receipt로 승격하는 데이터 마이그레이션은 없다.
receipt와 lock은 JDBC adapter가 사용한다. 배포 전 migration-runner V62와 runtime 권한을 확인한다.
