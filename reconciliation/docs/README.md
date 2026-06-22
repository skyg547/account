# reconciliation docs

`reconciliation` 모듈은 대사 단위 정의, 대사 규칙 관리, 실행 이력, 차이 추적, 차이 해소를 담당한다.
기존 인덱스 문서는 내용을 보존하기 위해 [archive/README_legacy_index_2026-06-11.md](./archive/README_legacy_index_2026-06-11.md)로 이동했다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 대사, 자동 매칭, 차이, 조정 전표를 초보자 관점에서 설명한다.
2. [process-flow.md](./process-flow.md) - API, 서비스, 외부 스냅샷 포트, 원장 집계, 차이 해소 흐름을 정리한다.
3. [schema.md](./schema.md) - 핵심 엔티티, 상태, criteriaJson 정책을 코드 기준으로 정리한다.
4. [local-run.md](./local-run.md) - IntelliJ IDEA와 Gradle에서 reconciliation 모듈을 로컬 검증하는 방법.

## 현재 실행 전제

- `reconciliation:core`는 업무 규칙과 persistence/local adapter를 담는 Java library 모듈이다.
- `reconciliation:api`는 `ReconciliationApiApplication`으로 H2 local API 서버를 실행한다.
- `reconciliation:batch`는 `ReconciliationBatchApplication`으로 Batch 컨텍스트를 실행하고, `reconciliationDailyJob`으로 활성 대사 단위를 일괄 실행한다.
- 로컬에서는 `.\gradlew :reconciliation:core:test`, `.\gradlew :reconciliation:api:bootRun`, `.\gradlew :reconciliation:batch:bootRun`으로 검증한다.

## 핵심 코드 입구

- 인바운드 어댑터: `web/ReconciliationController`
- 유즈케이스 서비스: `service/ReconciliationService`
- 매칭 컴포넌트: `service/AutomatedMatchingEngine`
- 도메인 모델: `ReconciliationUnit`, `ReconciliationRule`, `ReconciliationRun`, `ReconciliationDifference`, `DifferenceReasonCode`, `ReconciliationStageResult`
- 외부 포트: `ExternalReconSnapshotPort`, `JournalQueryPort`, `JournalPostingPort`
- 기술 어댑터: `ExternalReconStageSnapshotAdapter`, Spring Data JPA repositories
