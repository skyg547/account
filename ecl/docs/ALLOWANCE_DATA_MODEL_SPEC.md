# 대손충당금(IFRS 9) 데이터 모델 명세

현재 런타임은 IFRS 9 대손충당금 산출과 회계 summary 생성에 필요한 테이블만 사용합니다.

## 입력

### `allowance_exposure_snapshots`

`account-mart`가 기준일별로 고정한 ECL 입력 snapshot입니다.

| 컬럼 | 의미 |
| --- | --- |
| `base_date` | 기준일 |
| `exposure_id` | 익스포저 식별자 |
| `customer_code` | 고객 코드 |
| `product_code` | 상품 코드 |
| `currency_code` | 통화 |
| `outstanding_amount` | 현재 잔액 |
| `undrawn_amount` | 미사용 한도 |
| `staging` | IFRS 9 Stage |
| `accounting_account_code` | 원천 계정 |

## 산출 결과

### `allowance_model_parameters`

모델 계산 정책을 코드 하드코딩 대신 데이터로 관리합니다.

| 키 | 필수 여부 | 의미 |
| --- | --- | --- |
| `PD_FLOOR` | 필수 | 최소 PD |
| `SECURED_LGD_FLOOR` | 필수 | 담보부 최소 LGD |
| `UNSECURED_LGD_FLOOR` | 필수 | 무담보부 최소 LGD |
| `DEFAULT_DISCOUNT_RATE` | 선택 | 별도 할인율이 없을 때 사용할 기본 할인율 |
| `DEFAULT_CCF_RATE` | 선택 | 상품 CCF 마스터가 없을 때 사용할 보수적 기본 CCF |

비율 값은 0~1 범위여야 합니다. 필수 값이 없으면 산출을 중단하며, 선택 값이 없으면
`AllowanceModelParams`에 이름이 부여된 기본 정책을 사용합니다.

### `allowance_ecl_results`

계좌별 ECL 산출 결과 테이블입니다. 신규 로직은 PD, LGD, EAD, weighted ECL을 사용합니다.

| 컬럼 | 의미 |
| --- | --- |
| `base_date` | 기준일 |
| `account_id` | 입력 계좌 FK |
| `staging` | 산출 시점 Stage |
| `pd` | 부도율 |
| `lgd` | 부도 시 손실률 |
| `ead` / `ead_star` | 노출액 |
| `ecl_boom`, `ecl_base`, `ecl_recession` | 시나리오별 ECL |
| `weighted_ecl` | 최종 가중 ECL |
| `status` | 산출 상태 |

애플리케이션 내부 EAD/CRM 계산 결과는 `EadCalculationResult` 값 객체로 전달합니다.
필드명으로 `eadStar`, `eadRaw`, `crmDeduction`, `weightedLgd`를 구분하여 배열 인덱스 오사용을 방지합니다.

## 회계 summary

### `allowance_account_mappings`

상품/사업부/통화별 회계 계정 매핑입니다. 매핑이 없으면 summary 생성을 중단합니다.

### `allowance_summary`

closing이 조회하는 기준일별 대손충당금 summary입니다.

| 컬럼 | 의미 |
| --- | --- |
| `base_date` | 기준일 |
| `run_id` | 실행 식별자 |
| `model_version` | 모델 버전 |
| `legal_entity_code` | 법인 |
| `currency_code` | 통화 |
| `exposure_account_code` | 원천 노출 계정 |
| `allowance_account_code` | 대손충당금 계정 |
| `bad_debt_expense_account_code` | 대손상각비 계정 |
| `reversal_income_account_code` | 환입 계정 |
| `target_allowance_amount` | 목표 충당금 |
| `source_exposure_amount` | 원천 익스포저 금액 |
| `stage1/2/3_allowance_amount` | Stage별 충당금 |

