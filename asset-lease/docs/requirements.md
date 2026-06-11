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
- 배치는 절대 직접 계산하지 않는다. (`DepreciationPipeline` 호출)
- JPA 단건 저장을 지양하고 `JDBC Bulk` 어댑터를 사용한다.
- 모든 금액 계산은 `BigDecimal.ROUND_HALF_UP` (소수점 2자리) 원칙을 고수한다.

## 4. 현재 구현 점검 결과 (2026-06-11)

- `AssetDepreciationBatchConfig`는 `DepreciationPipeline`을 주입하지만 processor에서 `FixedAsset.depreciate`를 직접 호출하고 있다. Batch 계층이 순수 오케스트레이터가 되도록 코드에 `@todo`를 남겼다.
- Batch 회계월은 현재 `LocalDate.now().minusMonths(1)`로 결정된다. 재실행 가능성을 위해 `targetDate` JobParameter로 분리해야 한다.
- 고정자산 API는 `X-User-ID`를 받아 이력과 이벤트에 실행자를 남긴다.
- 리스 API는 아직 실행자 감사를 받지 않아 `@todo`로 남겼다.
- 리스 회계 계정 `25100`, `93100`, `21100`은 현재 서비스 상수다. 운영에서는 회사별 회계 정책에 맞게 설정 기반 AccountMappingPort로 분리해야 한다.
