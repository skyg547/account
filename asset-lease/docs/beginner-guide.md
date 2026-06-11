# asset-lease beginner guide

## 한 문장 요약

`asset-lease`는 회사가 오래 쓰는 물건과 장기 리스 계약을 장부에 어떻게 올리고, 매달 비용으로 어떻게 나눌지 관리한다.

초보자 관점에서는 두 업무를 나누어 보면 쉽다.

1. 고정자산: 노트북, 서버, 차량처럼 오래 쓰는 물건을 한 번에 비용 처리하지 않고 사용 기간에 나누어 비용화한다.
2. IFRS 16 리스: 사무실, 차량, 장비를 장기 임차할 때 앞으로 낼 돈을 리스부채로 잡고, 사용할 권리를 사용권자산으로 잡는다.

## 고정자산 흐름

회사가 1,200만 원짜리 장비를 사고 5년 동안 쓸 예정이라면, 매입한 달에 1,200만 원을 모두 비용으로 처리하지 않는다. 장비는 여러 달 동안 회사에 효익을 주기 때문이다. 이 비용을 월별로 나누어 장부가치를 줄이는 작업이 감가상각이다.

코드에서는 `FixedAsset`이 이 규칙을 가진다. `depreciate(processDate)`는 자산이 `ACTIVE`인지 확인하고, 당월 상각액을 누적상각액에 더하고, 현재 장부가액을 줄이고, 마지막 상각일을 갱신한다. 잔존가치까지 도달하면 상태를 `FULLY_DEPRECIATED`로 바꾼다.

## IFRS 16 리스 흐름

IFRS 16 리스는 "렌탈료를 매달 비용으로만 처리"하지 않는다. 장기 리스라면 다음 두 가지를 장부에 올린다.

- 사용권자산: 계약 기간 동안 자산을 사용할 권리
- 리스부채: 앞으로 지급해야 할 리스료의 현재가치

`LeaseEntryService`는 리스 계약을 저장한 뒤 IFRS 16 적용 대상이고 단기/소액 리스가 아니면 `RightOfUseAsset`, `LeaseLiability`, `LeasePaymentSchedule`을 생성한다. 월별 처리에서는 사용권자산 상각, 리스부채 원금 감소, 이자 비용 이벤트를 만든다.

## DDD와 헥사고날 관점

`domain`은 금액과 상태 변경 규칙을 가진다. `FixedAsset`은 상각으로 장부가액과 상태가 어떻게 바뀌는지 직접 관리한다. `LeasePaymentSchedule`은 각 지급 회차의 이자, 원금, 잔여 부채를 기록한다.

`application.service`는 업무 순서를 조율한다. `FixedAssetEntryService`는 등록, 월상각, 처분, 부서 변경을 처리하고 자산 이력을 남긴다. `LeaseEntryService`는 리스 최초 인식, 월별 회계처리, 리스료 지급결의, 재측정을 처리한다.

`application.pipeline`은 Batch 전용 대량 계산 위치다. `DepreciationPipeline`은 Chunk로 들어온 자산 목록을 도메인 규칙에 따라 상각 결과로 변환한다.

`adapter`와 `infrastructure`는 외부 기술을 담당한다. 웹 컨트롤러는 HTTP 요청을 유즈케이스로 전달하고, `AssetJdbcAdapter`는 대량 상각 결과를 JDBC batch로 저장하며, `AssetEventAdapter`는 Kafka로 자산 이벤트를 발행한다.

## 처음 볼 때 체크할 파일

1. `FixedAssetController`: 고정자산 등록, 상각 실행, 처분, 조회 API.
2. `FixedAssetEntryService`: 자산 이력과 Kafka 이벤트를 함께 남기는 업무 흐름.
3. `FixedAsset`: 월 상각 금액이 들어왔을 때 장부가액과 상태를 바꾸는 도메인 규칙.
4. `LeaseAccountingController`: IFRS 16 리스 등록, 월별 처리, 재측정 API.
5. `LeaseEntryService`: 사용권자산, 리스부채, 스케줄, 리스료 지급결의 흐름.
6. `AssetDepreciationBatchConfig`: 대량 상각 Job과 Step 설정.
