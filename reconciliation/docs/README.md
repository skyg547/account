# reconciliation docs

`reconciliation` 모듈은 대사 단위 정의, 대사 규칙 관리, 실행 이력, 차이 추적, 차이 해소를 담당한다.
기존 인덱스 문서는 내용을 보존하기 위해 [archive/README_legacy_index_2026-06-11.md](./archive/README_legacy_index_2026-06-11.md)로 이동했다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 대사, 자동 매칭, 차이, 조정 전표를 초보자 관점에서 설명한다.
2. [process-flow.md](./process-flow.md) - API, 서비스, 외부 스냅샷 포트, 원장 집계, 차이 해소 흐름을 정리한다.
3. [schema.md](./schema.md) - 핵심 엔티티, 상태, criteriaJson 정책을 코드 기준으로 정리한다.
4. [local-run.md](./local-run.md) - IntelliJ IDEA와 Gradle에서 reconciliation 모듈을 로컬 검증하는 방법.

## 현재 실행 전제

- `reconciliation`은 현재 `java-library` 모듈이다.
- 별도 `SpringBootApplication`이나 `:reconciliation:bootRun` 태스크가 없다.
- 로컬에서는 IntelliJ Gradle 실행 구성 또는 `.\gradlew :reconciliation:test`로 모듈 단위 검증을 수행한다.
- API를 실제 HTTP로 호출하려면 reconciliation 컴포넌트를 스캔하는 호스트 Spring Boot 애플리케이션이 필요하다.

## 핵심 코드 입구

- 인바운드 어댑터: `web/ReconciliationController`
- 유즈케이스 서비스: `service/ReconciliationService`
- 매칭 컴포넌트: `service/AutomatedMatchingEngine`
- 도메인 모델: `ReconciliationUnit`, `ReconciliationRule`, `ReconciliationRun`, `ReconciliationDifference`, `DifferenceReasonCode`, `ReconciliationStageResult`
- 외부 포트: `ExternalReconSnapshotPort`, `JournalQueryPort`, `JournalPostingPort`
- 기술 어댑터: `ExternalReconStageSnapshotAdapter`, Spring Data JPA repositories
