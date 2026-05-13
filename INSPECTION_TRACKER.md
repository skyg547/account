# 전사 모듈 정밀 검수 실행 계획 및 현황 (Inspection Tracker)

**검수 목적:** MSA/헥사고날 전환 과정에서 누락된 로직, 아키텍처 위반 사항, 테스트 부족분을 모듈별로 전수 조사하여 시스템의 완전성을 확보함.

---

## 📋 모듈별 검수 현황 (Status: `[o]` 완료, `[△]` 부분 보완 필요, `[ ]` 대기)

### Phase 1: 기반 및 인프라 (Foundation)
- [o] `shared-kernel`: 공통 타입 및 마스킹 처리 적정성 검수 완료.
- [o] `contracts`: 포트/DTO 분리 및 엔티티 침투 여부 검수 완료.
- [o] `auth`: 헥사고날 구조 및 외부 의존성 격리 검수 완료.
- [o] `config-server`, `discovery`, `gateway`: 인프라 설정 정합성 확인 완료.

### Phase 2: 기준 정보 및 권한 (Master Data & Governance)
- [o] `master-data`: SCD2 정책 적용 보완 완료 (`Product`, `Department`, `BusinessPartner`, `Currency`). `ExchangeRate` 참조 구조 개선 완료.
- [o] `governance`: 승인 연계 유실 문제 수정 및 `SystemUser` -> `Department` ID 기반 참조 전환 완료. (Phase 2 보완 완료)

### Phase 3: 업무 서브레저 (Subledgers)
- [o] `expenditure-resolution`: 마스터 조회 예외 처리 및 DTO 바인딩 보완 완료.
- [o] `payable`: ID 기반 참조 및 헥사고날 준수 확인.
- [△] `receivable`: **[위반]** `Receivable` -> `BusinessPartner` 객체 참조 존재. 인코딩 깨짐 현상. (Codex Handoff 예정)
- [o] `tax`: 도메인 팩토리 리팩토링 정합성 확인.
- [o] `asset-lease`: IFRS16 계정 분리 로직 및 자산 감가상각 로직 검수 완료.
- [o] `loan`: 자동 전표 생성 및 원장 수렴 호출 로직 검수 완료.

### Phase 4: 회계 엔진 및 결산 (Core Accounting)
- [ ] `journal-ledger`: 전표 생성/전기 로직, 벌크 처리 성능 및 헥사고날 격리 상태 정밀 검수.
- [ ] `closing`: 마감 통제 정책 및 결산 자동 분개 엔진 검수.
- [ ] `reconciliation`: 대량 데이터 집계 성능 및 외부 연동 포트 정합성 검수.
- [ ] `reporting`: 실제 원장 데이터 연동 구조 및 DTO 변환 적정성 검수.

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
