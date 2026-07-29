# Issue #41 Follow-up: BusinessPartner 도메인/JPA 분리

## 상태

- PR #225의 문서 전용 커밋 `cdb892e4`가 Issue #41을 한 번 닫았지만, 실제 구현이 없다는 closure audit 결과에 따라 다시 열렸습니다.
- 실제 구현은 `agent/41-business-partner-ddd`에서 `main@39dabd4e`를 기준으로 완료했습니다.
- 구현 PR은 `Fixes #41`로 연결하며 검증된 코드가 병합될 때 Issue를 닫습니다.

## 경계와 책임

- `BusinessPartner`와 `BusinessPartnerAccount`는 JPA 어노테이션이 없는 순수 도메인 모델입니다. 코드 불변성, SCD2 유효기간, 종료와 버전 마감의 차이, 계좌 소유권과 주계좌 규칙을 담당합니다.
- `BusinessPartnerJpaEntity`와 `BusinessPartnerAccountJpaEntity`는 기존 테이블·컬럼·인덱스·자식 FK를 유지하는 영속성 모델입니다.
- `JpaBusinessPartnerPersistenceAdapter`만 도메인과 JPA 모델을 양방향 변환합니다. Application과 다른 모듈은 `BusinessPartnerPersistencePort`를 사용합니다.
- `BusinessPartnerService`는 새 버전을 먼저 검증한 뒤 기존 버전을 마감하고 새 버전을 저장합니다. 새 버전의 계좌 값은 새 자식 ID로 복제되어 과거 FK와 현재 지급 계좌를 모두 보존합니다.
- API DTO가 응답 조립과 사업자등록번호 마스킹을 담당합니다. API/Batch 실행 모듈은 composition root에서 영속성 어댑터의 JPA 엔티티를 명시적으로 등록합니다.

## 호환성과 검증

- REST 경로와 데이터베이스 스키마는 변경하지 않아 migration을 추가하지 않았습니다.
- Master Data Core 12, API 2, Batch 4, Expenditure API 1, Loan Core 30, Journal Ledger 통합 1로 총 50개 테스트가 실패·오류·skip 없이 통과했습니다.
- Master Data API와 Batch `bootJar`가 모두 생성됐습니다.
- 독립 리뷰에서 발견한 SCD2 계좌 유실 가능성을 수정했고, 과거/신규 자식 ID, 저장 순서, 잘못된 갱신의 무쓰기, 기간 중첩 fail-closed를 회귀 테스트로 고정했습니다.

## 잔여 위험

- H2로 매핑과 조회를 검증했지만 PostgreSQL 실DB 테스트는 실행하지 못했습니다.
- 동시 갱신까지 데이터베이스에서 막으려면 기간 중첩 exclusion constraint와 PostgreSQL 통합 테스트가 필요합니다.
- 계좌 `EntityGraph`를 사용하는 무제한 목록 조회에는 별도 pagination 계약이 필요합니다.
