# Spring Batch & ETL 입문 가이드

`account-mart`의 배치는 원천 데이터를 읽고, 검증하고, IFRS 9 대손충당금 엔진이 읽을 수 있는 snapshot으로 저장합니다.

## ETL

1. Extract: ODS/GL 원천 데이터를 읽습니다.
2. Transform: 필수값, 금액, 통화, 계정 매핑을 검증하고 CDM 포지션으로 변환합니다.
3. Load: `allowance_exposure_snapshots`에 기준일 데이터를 고정합니다.

## Spring Batch 구성

- Job: 기준일 전체 처리 단위입니다.
- Step: DQ, 대사, 적재, snapshot 생성 같은 세부 단계입니다.
- Reader: 원천 데이터를 chunk 단위로 읽습니다.
- Processor: core 규칙을 호출해 데이터를 변환합니다.
- Writer: 변환 결과를 저장합니다.

표준 실행 Job은 `integratedPositionEtlJob`입니다.
