# 대손충당금 입력 마트 입문 가이드

대손충당금 엔진은 원천 원장을 그대로 사용하지 않습니다. `account-mart`가 원장, 고객, 상품, 담보, 환율 데이터를 정리해 기준일 snapshot을 만들고, `ecl` 모듈이 그 snapshot으로 ECL을 계산합니다.

## 흐름

1. 원천 데이터를 읽습니다.
2. 필수값과 잔액 대사를 검증합니다.
3. CDM 포지션을 만듭니다.
4. `allowance_exposure_snapshots`를 생성합니다.

## 확인할 코드

- `IntegratedPositionEtlJobConfig`: 표준 ETL Job 구성.
- `IntegratedPositionProcessor`: 원천 계좌를 CDM 포지션으로 변환.
- `AllowanceExposureSnapshotService`: ECL 입력 snapshot 생성.
