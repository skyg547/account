# 대손충당금 입력 마트 명세

`account-mart`의 목적은 기준일별 IFRS 9 대손충당금 입력 데이터를 만드는 것입니다.

## 주요 테이블

| 테이블 | 설명 |
| --- | --- |
| `ods_account_ledger` | 계좌 원천 원장 |
| `ods_customer_mst` | 고객 기준정보 |
| `ods_collateral_mst` | 담보 기준정보 |
| `ods_general_ledger` | 총계정원장 대사 입력 |
| `allowance_input_positions` | 정제된 CDM 포지션 |
| `allowance_exposure_snapshots` | ECL 산출 입력 snapshot |

## 품질 기준

- 기준일, 계좌번호, 고객번호, 상품코드, 통화, 금액은 필수입니다.
- ODS와 GL 잔액 대사는 기준일 단위로 기록합니다.
- snapshot은 기준일 재실행 시 기존 데이터를 정리한 뒤 다시 생성합니다.

