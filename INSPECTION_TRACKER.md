# 📋 전사 모듈 정밀 검수 트래커 (2026-05-14)

> ⚠️ **공지:** 본 트래커의 상세 검수 결과와 아키텍처 위반 사항은 **[TOTAL_QUALITY_REPORT.md](./TOTAL_QUALITY_REPORT.md)**에 최종 정리되었습니다.

---


## 📋 모듈별 검수 현황 (Status: `[o]` 완료, `[△]` 부분 보완 필요, `[ ]` 대기)

### Phase 1: 기반 및 인프라 (Foundation)
- [o] `shared-kernel`: 공통 타입 및 마스킹 처리 적정성 검수 완료.
- [o] `contracts`: 포트/DTO 분리 및 엔티티 침투 여부 검수 완료. `LedgerQueryPort` GL/SL 잔액 조회 계약 확장 완료.
- [o] `auth`: 헥사고날 구조 및 외부 의존성 격리 검수 완료.
- [o] `config-server`, `discovery`, `gateway`: 인프라 설정 정합성 확인 완료.

### Phase 2: 기준 정보 및 권한 (Master Data & Governance)
- [o] `master-data`: SCD2 정책 적용 보완 완료 (`Product`, `Department`, `BusinessPartner`, `Currency`). `ExchangeRate` 참조 구조 개선 완료.
- [o] `governance`: 승인 연계 유실 문제 수정, `SystemUser` -> `Department` ID 기반 참조 전환, 웹 DTO 경계 및 `TracingService` 포트 의존 전환 완료. (Phase 2 보완 완료)

### Phase 3: 업무 서브레저 (Subledgers)
- [o] `expenditure-resolution`: 마스터 조회 예외 처리 및 DTO 바인딩 보완 완료.
- [o] `payable`: ID 기반 참조 및 헥사고날 준수 확인.
- [o] `receivable`: 고객 `customerCode` 값 참조 및 웹 어댑터 DTO 전환 완료.
- [o] `tax`: 도메인 팩토리 리팩토링 정합성 확인.
- [o] `asset-lease`: IFRS16 계정 분리 로직 및 자산 감가상각 로직 검수 완료.
- [o] `loan`: 자동 전표 생성 및 원장 수렴 호출 로직 검수 완료.

### Phase 4: 회계 엔진 및 결산 (Core Accounting)
- [o] `journal-ledger`: master-data 직접 import/@ManyToOne 제거 상태와 주석 정합성 재확인 완료.
- [o] `closing`: `FiscalPeriod` 직접 엔티티 참조 제거 완료. 자동 평가/충당 분개의 계정/금액 설정 룰 전환 완료.
- [o] `reconciliation`: 인코딩 잔여 점검, 복합 매칭 조건, 조정분개 계정 산정 정책 도메인화 완료.
- [△] `reporting`: `LedgerQueryPort` 기반 실제 GL 잔액 연동 완료. 스냅샷 영속화와 SCD2 라인 매핑은 보완 필요.

---

## 📝 검수 실행 가이드 (Checklist)
1. **Architecture:** Port/Adapter 명확한 분리 및 도메인 고립 여부.
2. **References:** 타 모듈 엔티티 직접 참조(`@ManyToOne`) 제거 및 ID(`Long`/`String`) 기반 통신.
3. **SCD2:** 기준 정보 변경 시 `terminate()` 호출 및 신규 버전 생성 여부.
4. **Logic Integrity:** 핵심 비즈니스 규칙의 엔티티 내 응집 여부.
5. **Testing:** 유즈케이스별 단위/통합 테스트 커버리지 및 성공 여부.

---
## 📝 작업 로그
* 2026-05-13: 전사 모듈 정밀 검수 계획 수립 및 Phase 1~3 중간 결과 동기화 완료.
* 2026-05-13: Phase 4 (회계 엔진/결산) 정밀 검수 및 조치 완료. `journal-ledger`, `receivable` 모듈의 객체 직접 참조 위반 해결. `expenditure-resolution`, `loan` 연관 오류 수정 및 전사 테스트 통과 확인. `reconciliation` 인코딩 복구 작업 착수.
* 2026-05-14: `closing`의 `ClosingAdjustment`, `PeriodLock`, `ReopenApproval`, `ValuationBatch`, `ProvisionBatch`에서 `FiscalPeriod` 직접 참조를 제거하고 contracts 기반 `FiscalPeriodControlPort`로 전환. 상세 결과는 `TOTAL_QUALITY_REPORT.md`와 `CODEX_HANDOFF_TASKS.md`에 반영.
* 2026-05-14: `reconciliation` 엔티티/서비스 Java 파일 인코딩 잔여 점검 완료. `ReconciliationVariance`의 깨진 주석 1건을 정리하고 `:reconciliation:test` 통과 확인.
* 2026-05-14: `reporting`의 `LedgerClientAdapter` 목업 잔액/과거보고서 반환을 제거하고 `LedgerQueryPort` 기반 GL 잔액 조회로 전환. `:reporting:core:test`, `:reporting:api:compileJava`, `:reporting:batch:compileJava`, `:reporting:api:test`, `:reporting:batch:test` 통과 확인.
* 2026-05-14: `closing` 자동 평가/충당 분개의 더미 계정/고정 금액을 제거하고 `account.closing.accounting.*` 설정 룰로 전환. `:closing:core:test`, `:closing:api:compileJava`, `:closing:batch:compileJava` 통과 확인.
* 2026-05-15: `receivable`의 `SalesController`, `CollectionController`를 DTO 입출력으로 전환하고 수동 매칭 `Map` 요청을 `ManualMatchingRequest`로 교체. `:receivable:test` 통과 확인.
* 2026-05-18: `governance`의 `AuditController` 도메인/Map 노출을 DTO로 정리하고 `TracingService`의 `AuditLogRepository` 직접 의존을 `AuditLogPersistencePort`로 전환. `:governance:test` 통과 확인.
* 2026-05-18: `journal-ledger`의 master-data 직접 import/@ManyToOne 잔여 여부를 재확인하고 stale 엔티티 변환 주석을 코드 값 저장 설명으로 정리. `:journal-ledger:core:test` 통과 확인.
* 2026-05-18: `reconciliation` 자동 매칭에 전표번호/적요/계좌번호 복합 조건을 보강하고 조정분개 계정 산정을 `ReconciliationAdjustmentPolicy`로 분리. `:contracts:compileJava`, `:journal-ledger:core:compileJava`, `:reconciliation:test` 통과 확인.
* 2026-05-18: `contracts`의 `LedgerQueryPort`에 SL/거래처/부서 단위 잔액 조회 계약을 추가하고 `journal-ledger` 어댑터 구현/테스트 보강. `:contracts:compileJava`, `:journal-ledger:core:test` 통과 확인.
