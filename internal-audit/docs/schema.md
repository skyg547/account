# 🗄️ Internal Audit 데이터 스키마 (Schema)

Flyway V60 마이그레이션 스크립트에 의해 관리되는 내부감사 모듈 전용 테이블 명세입니다.

---

## 1. 테이블 목록

| 테이블명 | 영문 설명 | 주요 컬럼 |
| :--- | :--- | :--- |
| **`rcm_processes`** | 업무 프로세스 분류 | `id`, `process_code`, `process_name`, `owner_department` |
| **`rcm_risks`** | 통제 대상 리스크 | `id`, `process_id`, `risk_code`, `risk_description`, `severity` |
| **`control_activities`** | 핵심 통제 활동 | `id`, `risk_id`, `control_code`, `control_type`, `frequency` |
| **`design_evaluations`** | 통제 설계 적정성 평가 | `id`, `control_id`, `fiscal_year`, `evaluation_status`, `evaluator_id` |
| **`operating_evaluations`** | 통제 운영 효과성 평가 | `id`, `control_id`, `sample_size`, `exception_count`, `effectiveness` |
| **`deficiencies`** | 통제 결함 및 개선 과제 | `id`, `control_id`, `deficiency_type`, `action_plan`, `due_date` |

---

## 2. 스키마 격리 원칙
- 본 모듈은 타 도메인(재무, 대출 등)의 테이블을 직접 참조하지 않으며, 오직 `rcm_*` 및 `*_evaluations`, `deficiencies` 테이블만 단독 소유합니다.