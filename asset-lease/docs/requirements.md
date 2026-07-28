# 🚜 Asset-Lease 모듈 요구사항 정의 (고도화 버전)

## 1. 비즈니스 목적
- 수백만 대의 고정자산 및 리스 자산에 대한 감가상각비를 매달 말일 **30분 이내에 일괄 산출**하고 전표 이벤트를 발행한다.
- 모든 자산 상태 변경(부서 이동, 처분 등)은 100% 이력으로 남겨 감사 추적(Audit Trail)을 보장한다.

## 2. 핵심 기능 요구사항
- **[P0] 대용량 감가상각:** 1억 건 규모의 자산 데이터에 대해 Chunk 단위로 상각비를 계산한다.
- **[P0] 비동기 전표 연계:** 상각 결과를 Kafka를 통해 `journal-ledger`로 전송한다.
- **[P1] 자산 이력 자동화:** 부서 이동 및 상태 변경 시 `AssetHistory`를 원자적으로 기록한다.
- **[P1] IFRS 16 준수:** 리스부채 및 사용권자산의 현재가치 재측정 및 상환 스케줄을 관리한다.

## 3. 기술적 제약 사항 (Agents.md 규율)
- 배치는 직접 계산하지 않고 `DepreciationPipeline`을 호출한다.
- Batch pipeline은 JPA 엔티티를 변경하지 않고 `FixedAssetDepreciationResult` 값 객체만 만든다.
- 실제 대량 반영은 `AssetJdbcAdapter`의 JDBC bulk update가 한 번만 수행한다.
- 모든 금액 계산은 `BigDecimal`을 사용하고, 소수점 정책은 도메인 규칙에서 명시한다.

## 4. 현재 구현 점검 결과 (2026-07-07)

- `AssetDepreciationBatchConfig`는 Reader/Processor/Writer와 `targetDate` JobParameter orchestration만 담당한다.
- `DepreciationPipeline`은 `FixedAsset.calculateDepreciation()`으로 mutation 없는 batch preview 결과를 만든다.
- `FixedAsset.depreciate()`는 단건 API/서비스 경로에서만 상태 전이를 수행한다.
- `AssetJdbcAdapter`는 상각누계액, 장부가액, 상태, 최종상각일을 bulk update로 반영한다.
- 고정자산 API와 리스 등록/월별 처리/재측정 API는 `X-User-ID`를 받아 감사 이벤트에 실행자를 남긴다.
- 리스 회계 계정 `25100`, `93100`, `21100` 기본값은 `LeaseAccountMappingPort`와 설정 기반 어댑터로 분리되어 있다.