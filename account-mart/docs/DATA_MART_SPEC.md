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

## 소유권과 모듈 경계

- `allowance_input_positions`의 쓰기 소유권은 `account-mart`입니다.
- `ecl`은 `account-mart`의 JPA 엔티티를 직접 공유하지 않습니다.
- 외부 모듈은 snapshot 조회 계약 또는 API를 통해 필요한 데이터를 읽습니다.
- 대량 적재 성능은 batch/application 루프가 아니라 repository/adapter의 bulk 처리와 chunk 설정으로 확보합니다.

## 기준일 재실행 체크

1. 같은 `baseDate`의 기존 snapshot 정리 범위를 확인합니다.
2. ODS 필수값 검증 결과와 skip 건수를 확인합니다.
3. ODS-GL 대사 차이가 허용 범위인지 확인합니다.
4. ECL로 넘길 snapshot 건수와 잔액 합계가 기준일 원천과 맞는지 확인합니다.
