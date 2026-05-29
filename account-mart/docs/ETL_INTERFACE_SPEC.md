# 대손충당금 입력 ETL 인터페이스

이 문서는 `account-mart`가 IFRS 9 대손충당금 산출을 위해 수집하는 원천 데이터와 활용 목적을 정리합니다.

| 원천 | 용도 |
| --- | --- |
| `ods_account_ledger` | 잔액, 한도, 만기, 연체일수 기반 EAD/Stage 입력 |
| `ods_customer_mst` | 고객 유형, 등급, 산업, 국가 코드 |
| `ods_product_mst` | 상품 코드와 회계 계정 매핑 보조 |
| `ods_collateral_mst` | 담보 유형과 평가액 |
| `ods_general_ledger` | ODS 잔액 대사 |
| `exchange_rate` | 외화 익스포저 원화 환산 |

ETL 결과는 `allowance_input_positions`를 거쳐 `allowance_exposure_snapshots`로 고정됩니다.

