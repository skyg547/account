# 대손충당금 입력 ETL 인터페이스

이 문서는 `account-mart`가 IFRS 9 대손충당금 산출을 위해 수집하는 원천 데이터와 활용 목적을 정리합니다.

| 원천 | 용도 |
| --- | --- |
| `ods_account_ledger` | 잔액, 한도, 만기, 연체일수 기반 EAD/Stage 입력 |
| `ods_customer_mst` | 고객 유형, 등급, 산업, 국가 코드 |
| `ods_product_mst` | 상품 코드와 회계 계정 매핑 보조 |
| `ods_collateral_mst` / `ods_coll_mst` | 담보 유형과 평가액 |
| `ods_apart_coll_detail` | 부동산/아파트 담보의 지역 코드, KB 시세, 전용면적 기반 LGD 선행 입력 검증 |
| `ods_general_ledger` | ODS 잔액 대사 |
| `market_exchange_rate` | 기준일 외화 익스포저의 원화 평가액 환산 |

ETL 결과는 `allowance_input_positions`를 거쳐 `allowance_exposure_snapshots`로 고정됩니다.

## 필수 Job 파라미터

| 파라미터 | 예시 | 설명 |
| --- | --- | --- |
| `baseDate` | `2026-04-30` | snapshot 기준일 |
| `spring.batch.job.name` | `integratedPositionEtlJob` | 실행할 Spring Batch Job 이름 |

CLI 실행 예시는 다음과 같습니다.

```powershell
.\gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30" --console=plain
```

## 인터페이스 정합성 포인트

- 원천 계좌와 고객 기준정보는 같은 기준일 기준으로 맞춰야 합니다.
- 부동산/아파트 담보는 마스터와 상세 데이터가 같은 담보번호로 연결되어야 합니다.
- KRW 포지션의 평가액은 원금에 소수점 네 자리 `HALF_UP` 반올림을 적용합니다. 외화 포지션은 해당 `baseDate`의 외화/KRW `base_rate`를 곱한 뒤 같은 정책으로 반올림합니다.
- 외화 포지션에는 지원하는 통화 코드와 기준일·통화쌍이 일치하는 양수 환율이 필수입니다. 통화 코드가 지원되지 않거나 기준일·환율이 없거나, 조회된 환율의 기준일·통화쌍이 다르거나, 환율값이 `null`·0·음수이면 `cdmLoadStep`과 `integratedPositionEtlJob`이 실패합니다. 원금을 1:1 원화 평가액으로 저장하지 않으며 후속 snapshot 생성도 진행하지 않습니다.
- 동일 기준일 재실행이 실패하면 이전 실행의 snapshot은 남을 수 있으므로, 해당 기준일의 기존 snapshot은 후속 사용 전에 별도 확인해야 합니다.
- ODS와 GL 대사는 잔액 총액뿐 아니라 통화/계정 축을 함께 확인해야 합니다.
- ETL 완료 후 ECL 모듈이 읽는 snapshot은 더 이상 원천 변경에 흔들리지 않는 고정 입력이어야 합니다.
