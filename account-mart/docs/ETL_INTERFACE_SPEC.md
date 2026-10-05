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
| `market_exchange_rate` | 기준일 외화 익스포저의 KRW 평가금액 환산 |

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
- KRW 원금은 환율 조회 없이 소수점 4자리로 평가합니다. 외화 원금은 `baseDate` 당일의 `외화/KRW` 환율이 반드시 있어야 하며, `marketValue = outstandingAmount × base_rate`를 `HALF_UP`으로 소수점 4자리까지 반올림합니다. 원금(`outstandingAmount`)은 원래 통화 금액으로 유지합니다.
- 외화 환율이 없거나 `base_rate`가 null, 0 또는 음수이면 CDM 변환이 실패합니다. 이 오류는 CDM Step의 일반 `IllegalArgumentException` skip 대상에 포함되지 않아 `cdmLoadStep`과 `integratedPositionEtlJob`이 `FAILED`가 되며, 후속 snapshot 생성·이벤트 발행 Step은 실행되지 않습니다. 원금을 KRW 평가액으로 1:1 저장하지 않습니다.
- 배치 재실행 전에는 같은 `baseDate`의 유효한 외화/KRW 환율을 적재하고 CDM/snapshot의 부분 적재 건수를 대사합니다. CDM reader는 `saveState(false)`이므로 동일 식별 파라미터로 재시작하면 CDM Step을 처음부터 다시 읽지만 완료된 전처리 Step은 건너뜁니다. 기존 CDM row와 충돌할 수 있으므로, 기준일 원천·환율을 확인한 뒤 새 식별 파라미터로 Job을 실행해 전처리부터 다시 수행합니다.
- ODS와 GL 대사는 잔액 총액뿐 아니라 통화/계정 축을 함께 확인해야 합니다.
- ETL 완료 후 ECL 모듈이 읽는 snapshot은 더 이상 원천 변경에 흔들리지 않는 고정 입력이어야 합니다.

## 환율 경계 로컬 검증

저장소 루트에서 다음 명령으로 core 환산 규칙과 H2 기반 배치 Job을 검증합니다.

```bash
./gradlew --offline :account-mart:mart-core:test --tests com.ho.account.mart.core.domain.mart.processor.IntegratedPositionProcessorTest :account-mart:mart-batch:test --tests com.ho.account.mart.batch.job.ods.IntegratedPositionEtlJobTest --console=plain --max-workers=1 --no-daemon
```

정상 환율은 KRW 평가액을 생성하고, 누락·무효 환율은 해당 Job이 `FAILED`로 끝나야 합니다. 배치 통합 테스트는 로컬 H2 데이터를 사용하며 운영 환율의 완전성까지 보증하지는 않습니다.
