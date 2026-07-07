# asset-lease docs

`asset-lease` 모듈은 고정자산 취득/상각/처분과 IFRS 16 리스 계약, 사용권자산, 리스부채, 지급 스케줄을 함께 관리한다.
기존 인덱스 문서는 내용을 보존하기 위해 [archive/README_legacy_index_2026-06-11.md](./archive/README_legacy_index_2026-06-11.md)로 이동했다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 고정자산, 감가상각, 사용권자산, 리스부채를 초보자 관점에서 설명한다.
2. [process-flow.md](./process-flow.md) - API, 서비스, 도메인, Batch, Kafka 이벤트, 지급결의 포트 흐름을 정리한다.
3. [schema.md](./schema.md) - 핵심 엔티티와 테이블, 상태, migration 주의사항을 코드 기준으로 정리한다.
4. [local-run.md](./local-run.md) - IntelliJ IDEA와 Gradle에서 API와 테스트를 실행하는 방법.

## 보존한 기존 문서

- [api-spec.md](./api-spec.md) - 기존 REST API 명세. 새 흐름 문서와 함께 확인한다.
- [requirements.md](./requirements.md) - 대용량 감가상각, 감사 추적, IFRS 16 요구사항.
- [db/schema.sql](./db/schema.sql) - 과거 단순화된 DB 스케치. 실제 기준은 JPA 엔티티와 `core/src/main/resources/db/migration`을 함께 확인한다.

## 현재 실행 전제

- `asset-lease`는 `core`, `api`, `batch` Gradle 하위 프로젝트로 분리되어 있다.
- API 실행 클래스는 `com.ho.account.asset.api.AssetLeaseApiApplication`, Batch 실행 클래스는 `com.ho.account.asset.batch.AssetLeaseBatchApplication`이다.
- 로컬 API 실행 시 Config Server와 Eureka는 끄고 실행하는 편이 안정적이다.
- 자산 등록/상각/처분은 `AssetEventPort`를 통해 Kafka `transaction-events` 이벤트를 발행한다.
- 리스료 지급결의는 `LeasePaymentResolutionPort`가 필요하다. 포트가 없으면 fallback 어댑터가 예외를 던진다.

## 핵심 코드 입구

- 인바운드 어댑터: `api/src/main/java/com/ho/account/asset/web/FixedAssetController`, `LeaseAccountingController`
- 유즈케이스 서비스: `core/src/main/java/com/ho/account/asset/application/service/FixedAssetEntryService`, `LeaseEntryService`
- Batch 오케스트레이터: `batch/src/main/java/com/ho/account/asset/batch/AssetDepreciationBatchConfig`
- Batch 계산 파이프라인: `core/src/main/java/com/ho/account/asset/application/pipeline/DepreciationPipeline`
- Batch 감가상각 결과 값: `FixedAssetDepreciationResult`
- 도메인 모델: `FixedAsset`, `AssetHistory`, `LeaseContract`, `RightOfUseAsset`, `LeaseLiability`, `LeasePaymentSchedule`
- 기술 어댑터: `AssetJdbcAdapter`, `AssetEventAdapter`, `AssetSourceDocumentProvider`
