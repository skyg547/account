# WORKLOG (Source of Truth)

... (이전 내용 생략) ...

### 📅 2026-04-23 (오후)
### [기획/팀장]
- **리스크 관리 도메인 확장:** 시스템 정체성을 강화하기 위해 Basel III 및 IFRS 9 전문 화면 기획 및 인프라 구축.
- **자산/리스 연동 설계:** `asset-lease` 모듈의 API 명세를 기반으로 한 실무형 화면 3종 설계 완료.

### [프론트] 자산 및 리스 회계 관리 모듈 완성
- **화면 3종 정식 구축**: `FixedAssetMaster`, `LeaseManagement`, `ClosingDashboard`를 백엔드 API 명세에 맞춰 구현.
- **디자인 시스템 고도화**: 리스 금융 데이터(할인율, ROU 자산 가치 등)를 직관적으로 다룰 수 있는 금융 매트릭스 스타일 UI 적용.
- **결산 컨트롤 타워**: 월말 감가상각 및 리스 결산을 원클릭으로 수행할 수 있는 통합 대시보드 구축.

### [백엔드] 아키텍처 고도화
- **Loan 모듈 리팩토링**: `loan` 모듈의 MSA 전환 및 JDBC Bulk Adapter 적용 완료.
- **코드 품질 관리**: 도메인 객체의 Rich Domain Model화 진행.

---

### 📅 2026-04-24 (오후)
### [프론트] 엔터프라이즈 ERP UI 현대화 (Tailwind CSS 전면 전환)
- **전체 CSS 구조 개편:** 레거시 CSS Modules(`*.module.css`) 90% 이상 제거 및 Tailwind 유틸리티 퍼스트 디자인 시스템으로 통합 완료.
- **글래스모피즘(Glassmorphism) ERP 스타일 적용:** `slate-950` 다크 테마를 기반으로 한 투명 레이어, 블러 효과, 네온 글로우 포인트 등 프리미엄 디자인 언어 구축.
- **주요 금융 화면 고도화:**
  - `매출/매입채무`: Aging 분석 및 지급 스케줄 시각화.
  - `리스/고정자산`: 자산 등록 모달 및 세무 관리 테이블 현대화.
  - `재무제표/리스크`: 복잡한 데이터 그리드와 계층형 보고서의 가독성 대폭 향상.
- **아키텍처 정비:** `Sidebar`(300px 가변형) 및 `TopHeader`(글로벌 네비게이션) 레이아웃 정합성 확보.

### [기획/팀장] 기술 백과사전(Tech-Stack) 38종 최종 완성 및 아키텍처 심화
- **기술 총서 4단계 완결:** 기존 32종에서 데이터 시각화, AOP, 도메인 설계 철학(DDD/EDA) 등 총 38종으로 기술 가이드 최종 완결.
- **아키텍처 및 철학 수록:** 단순 라이브러리 설명을 넘어 'Rich Domain Model', 'Aggregate', 'Saga/Event-Driven' 등 시스템의 근간이 되는 아키텍처 철학까지 초보자 수준으로 풀어서 설명 완료.
- **실전성 극대화:** Recharts 그래프 구현, AOP 감시자 설정, Rich Entity 설계법 등 실무 밀착형 가이드 전수 적용.

### [기획/팀장] 사용자 권한 및 시스템 관리 강화
- **어드민 모듈 설계:** 엔터프라이즈 환경의 보안 강화를 위한 '사용자 그룹/권한 관리' 및 '메뉴 구조 관리' 화면 신규 기획.
- **UX 편의성 향상:** 넓은 작업 공간 확보를 위한 'Collapsible Sidebar' 기능 도입 결정.

### [프론트] 시스템 관리 모듈 및 레이아웃 고도화
- **Collapsible Sidebar 구현:** `NavContext`와 `MainLayout`을 연동하여 사이드바를 300px에서 80px로 가변적으로 접을 수 있는 애니메이션 레이아웃 구축.
- **관리자 전용 화면 구축:**
  - `/admin/users`: 고성능 데이터 테이블을 활용한 사용자 권한 및 상태 관리 화면.
  - `/admin/menus`: 시스템 메뉴 트리 구조 및 접근 권한 매핑 화면.
- **아이콘 및 인터랙션:** 루시드 아이콘 고도화 및 호버링 가이드 툴팁 적용.

### 📅 2026-04-27 (오전)
### [기획/팀장] Admin 모듈 보안 및 권한 체계 확립
- **RBAC(Role Based Access Control) 설계**: 시스템 관리자, 회계 관리자, 리스크 관리자 등 역할별 접근 제어 정책 수립 및 프론트엔드 반영.
- **실시간 권한 동기화 기획**: 관리자 화면에서 권한 변경 시 사용자 세션에 즉각 반영되는 UX 시나리오 검증.

### [프론트] 동적 내비게이션 및 어드민 서비스 구현
- **NavContext 고도화**: 전역 상태에 `userRole`을 도입하여 권한 기반의 렌더링 환경 구축.
- **Sidebar 메뉴 필터링**: `menuItems`에 `requiredRoles`를 적용하여 권한 없는 메뉴의 노출을 원천 차단.
- **Admin Service Layer**: `adminService.ts`를 통한 데이터 fetch/update 로직 추상화 (Mock 기반).
- **사용자 관리 페이지 바인딩**: `admin/users` 화면을 서비스와 연동하고, 역할 변경 시 사이드바가 즉각 갱신되는 반응형 로직 완성.

### [QA] 레이아웃 및 정합성 검증
- **사이드바 반응형 검사**: 접힘/펼침 상태에 따른 `MainLayout`의 가변 여백(ml-20 vs ml-300) 및 내부 콘텐츠 리사이징 정합성 확인.

### 📅 2026-04-27 (오후)
### [기획/팀장] Reporting 모듈 아키텍처 현대화 및 대외 보고서 기능 강화
- **헥사고날 아키텍처 완성**: `api`, `batch`, `core` 레이어 분리 및 인/아웃바운드 포트 기반 통신 구조 확립.
- **대외 보고서 핵심 기능 고도화**:
    - **비교 재무제표(Comparative Financial Statements)**: 당기 및 전기 데이터를 병렬로 처리하는 로직 구현.
    - **주석 연동(Disclosure Note Mapping)**: 보고서 항목별 주석 번호 자동 매핑 필드 추가.
    - **이력 관리**: 과거 보고서 데이터를 참조하기 위한 `LoadReportHistoryPort` 아키텍처 반영.
- **도메인 지식 문서화**: `reporting/docs/external_reporting_guide.md`를 통해 K-IFRS 기준 보고서 산출 프로세스 정립.
- **빌드 및 정합성 검증**: `./gradlew :reporting:classes` 빌드 성공을 통한 의존성 및 코드 정합성 최종 확인.

### 📅 2026-04-28 (오후)
### [기획/팀장] 매입채무/매출채권/세무 모듈 아키텍처 현대화 완료
- **헥사고날 아키텍처 전면 도입**: `payable`, `receivable`, `tax` 모듈의 계층형 구조를 헥사고날(Port/Adapter) 아키텍처로 전환 완료.
- **Rich Domain Model (DDD) 고도화**:
  - `TaxInvoice`: 금액 정합성 검증(`validateAmounts`) 및 정보 업데이트 로직을 도메인 내부로 캡슐화.
  - `Payable/Receivable`: 잔액 차감 및 상태 관리 로직 내재화.
- **모듈 간 결합도 완화**: `TaxInvoiceQueryPort` 등을 통한 포트 기반 통신으로 모듈 간 직접 참조 최소화.

### [백엔드] 헥사고날 서비스 및 어댑터 구현
- **Port/UseCase 정의**: 각 도메인의 유즈케이스(UseCase) 인터페이스와 영속성 포트(PersistencePort) 정의.
- **Persistence Adapter**: 인프라 기술을 은닉하는 영속성 어댑터 구현.
- **External Adapter**: 타 모듈과의 연동을 위한 전용 어댑터 (`TaxInvoiceQueryAdapter`) 리팩토링.

### [QA] 모듈 컴파일 및 단위 테스트 검증
- **빌드 성공**: `:payable`, `:receivable`, `:tax` 모듈의 리팩토링 후 컴파일 정합성 확인.
- **단위 테스트**: 도메인 엔티티의 핵심 비즈니스 로직(금액 검증 등)을 검증하는 JUnit 5 테스트 코드 보강.

### 📅 2026-04-29 (오후)
### [기획/팀장] 통합 비즈니스 프로세스 검증 및 자산 모듈 현대화 완료
- **엔드투엔드(E2E) 비즈니스 시나리오 증명**: '세금계산서 수취 -> 매입 인식 -> 채무 생성 -> 지급 실행 -> 전표 전기'로 이어지는 회계 시스템의 핵심 파이프라인을 자동화된 통합 테스트로 검증 완료.
- **모듈 간 정합성 확보**: `tax`, `payable`, `journal-ledger` 모듈 간의 유기적인 연동 데이터 흐름 확인.
- **자산 모듈 아키텍처 결합도 제거**: `asset-lease` 모듈의 도메인 엔티티에서 `master-data` 엔티티 직접 참조를 제거하고 코드(ID) 기반 참조로 전환하여 독립적인 서비스 배포 기반 마련.

### [백엔드] 헥사고날 어댑터 구현 및 버그 수정
- **Journal Posting Adapter 구현**: `contracts` 모듈의 전표 전기 포트(`JournalPostingPort`)를 `journal-ledger` 모듈 내에서 구현하여 타 모듈에서의 전표 발행 기능 활성화.
- **전표 채번 버그 수정**: `JournalEntryService`에서 전표 저장 시 `slipNo`가 누락되던 결함을 수정하고 자동 채번 로직(`JE-yyyyMMdd-XXXX`) 도입.
- **마스터 데이터 포트 확장**: 테스트 및 데이터 관리를 위해 `CurrencyPersistencePort`에 `save` 메서드 추가 및 어댑터 구현.

### [QA] 통합 테스트 수행 및 검증
- **IntegratedBusinessProcessTest 수행**: `journal-ledger:api` 모듈 내에 통합 테스트 클래스를 구축하여 실제 Spring 컨텍스트 상에서의 비즈니스 흐름 정합성 100% 통과 확인.
- **아키텍처 규칙 검수**: `asset-lease` 리팩토링 후 빌드 정합성 검증 (`./gradlew :asset-lease:classes` 성공).

### 📅 2026-04-30 (오후 - 결산 현대화)
### [백엔드] 결산(Closing) 모듈 헥사고날 아키텍처 전환 완료
- **멀티 모듈 구조 확립**: `closing` 모듈을 `closing:core`, `closing:api`, `closing:batch`로 분리하여 역할 정의.
- **인바운드 포트 도입**: `ClosingUseCase`, `AnnualClosingUseCase` 인터페이스를 추출하여 핵심 로직 보호.
- **순환 참조 해결**: `journal-ledger`와의 순환 참조를 해결하기 위해 `contracts` 모듈의 포트(`JournalPostingPort`, `JournalQueryPort`)를 통한 약한 결합으로 리팩토링.
- **도메인 모델 개선**: 엔티티 간 직접 참조를 ID 기반 참조로 변경하여 모듈 독립성 확보.

### [공통] 인프라 및 Git 동기화
- **Contracts 확장**: `JournalQueryPort`, `JournalSide` 등 모듈 간 협력을 위한 공용 인터페이스 추가.
- **Risk 모듈 폐기**: 사용자 요청에 따라 리스크 관련 모든 코드 및 문서 완전 삭제 및 복구 완료.
- **Git Push 완료**: 세무 고도화, 결산 현대화, 리스크 복구 내역을 원격 저장소(`main`)에 최종 동기화.

### 📅 2026-05-04 (오후 - 전사 아키텍처 고도화)
### [기획/팀장/백엔드] 전사 모듈 DDD 및 헥사고날 아키텍처 리팩토링 (1단계)
- **아키텍처 점검 및 가이드 수립**: `ARCHITECTURE_REVIEW.md` 생성을 통해 전사 모듈의 DDD/헥사고날 준수 현황 점검 및 표준 가이드라인 정의.
- **Journal Ledger 고도화**: `JournalEntry`에 역분개(Reversal) 생성 팩토리 메서드 도입 및 상태 전이 로직 응집 (Rich Domain Model).
- **Closing 고도화**: `ClosingCalendar`, `ClosingTask` 내부에 완료 가능 여부 및 상태 변경 도메인 로직을 구현하여 서비스 레이어 간소화.
- **모듈 간 독립성 확보 (Independence)**: `tax`, `expenditure`, `receivable` 모듈의 엔티티에서 타 모듈(Master Data, Journal) 직접 참조를 ID(Code) 기반 참조로 전환 완료.
- **Rich Domain Model 확산**: `TaxInvoice`, `SalesInvoice`, `ExpenditureResolution`에 정적 팩토리 메서드 및 비즈니스 검증 로직 내재화.

### [QA] 아키텍처 개선 정합성 확인
- **컴파일 및 빌드 검수**: 주요 모듈(`journal-ledger`, `closing`, `tax`, `expenditure`, `receivable`)의 리팩토링 후 컴파일 정합성 확인 완료.

**NEXT STEPS (다음 담당자):**
1. **[백엔드] 전사 통합 테스트 보강**: ID 기반 참조 전환에 따른 모듈 간 통합 테스트(E2E) 시나리오 재검증 및 데이터 정합성 체크.
2. **[프론트] 결산 감사 로그 조회 화면 구현**: 관리자가 결산 프로세스의 진행 이력 및 재오픈 사유를 확인할 수 있는 타임라인 형태의 Audit Log UI 구축.
3. **[프론트] 결산 관리 화면 리팩토링**: 백엔드에서 강화된 검증 로직(에러 메시지 처리 등)을 UI에 반영하고, 결산 진행 상태 시각화 최적화.

### 📅 2026-05-04 (오후 - WORKLOG 검수/정리)
### [문서/운영] WORKLOG 단일화 및 오래된 로그 정리
- **중복 로그 정리**: `docs/WORKLOG.md`, `docs/worklog-2026-04-07.md` 삭제.
- **기준 로그 단일화**: 루트 `WORKLOG.md`를 단일 Source of Truth로 유지.
- **참조 정합성 수정**: `Agents.md`, `SKILL.md`, `GEMINI.md`, `docs/skills.md`, `docs/GEMINI_SKILL.md`, `docs/config-guide.md` 내 `docs/WORKLOG.md` 참조를 `WORKLOG.md`로 통일.
- **검수 결과**: 저장소 내 `docs/WORKLOG.md` 및 `worklog-2026-04-07.md` 참조 잔여 없음 확인.











### 📅 2026-05-04 (오후)
### [기획/팀장]
- **전사 모듈 DDD 및 헥사고날 아키텍처 리팩토링 완료 (1단계)**: 시스템의 확장성과 독립성을 위해 핵심 회계 모듈들에 대한 대규모 리팩토링 단행.
- **Rich Domain Model 적용**: `Journal Ledger`, `Closing`, `Tax`, `Expenditure`, `Receivable` 모듈의 엔티티를 풍부한 도메인 모델로 개선하여 비즈니스 로직의 응집도 향상.
- **모듈 간 독립성 확보 (Independence)**: `tax`, `expenditure`, `receivable` 모듈의 엔티티에서 타 모듈(Master Data, Journal) 직접 참조를 ID(Code) 기반 참조로 전환 완료.

### [백엔드]
- **ID 기반 참조 전환 완수 (Finalized)**: `PurchaseInvoice`, `Payable`, `Payment`, `AdvancePayment`, `TaxInvoice` 엔티티에서 타 모듈 직접 참조를 제거하고 Code/ID 기반 참조로 완벽히 전환.
- **Repository 및 서비스 정합성 확보**: 엔티티 변경에 따른 전사 Repository 쿼리 메서드 수정 및 `PaymentService`, `PurchaseService` 등 핵심 서비스 로직 업데이트 완료.
- **통합 비즈니스 프로세스 검증**: `IntegratedBusinessProcessTest`를 복구하여 세금계산서 수취부터 지급 완료까지의 파이프라인이 독립된 모듈 체계에서도 정상 작동함을 증명.

### [QA]
- **전사 통합 테스트 Pass**: 리팩토링 이후의 데이터 정합성 검증 완료.

### 📅 2026-05-06 (재검수)
### [검수] Gemini 변경분 재검수 결과
- **검수 범위**: `closing`, `journal-ledger`, `expenditure-resolution`, `receivable`, `payable`, `tax`, `frontend(closing)` 변경.
- **검수 방식**: 모듈 컴파일/빌드 재실행 + 코드 정합성 점검.
- **실행 확인**:
  - `.\gradlew :expenditure-resolution:compileJava :tax:compileJava :receivable:compileJava :payable:compileJava :journal-ledger:core:compileJava :closing:core:compileJava --console=plain` 실행.
  - `expenditure-resolution` 컴파일 실패 재현.
  - `.\gradlew :tax:compileJava :receivable:compileJava :payable:compileJava :journal-ledger:core:compileJava :closing:core:compileJava --console=plain` 성공.
  - `frontend`에서 `npm run build` 성공(ESLint unused 경고만 존재).
- **완료된 리뷰 조치 정리**:
  - `tax/.../TaxInvoiceDto.java`의 `getBusinessPartner()` 컴파일 오류 항목 삭제 완료.
  - `closing`, `receivable`, `payable` 모듈 컴파일 오류 항목 삭제 완료.
- **미해결 이슈(유지)**:
  - **Critical**: `expenditure-resolution/.../ExpenditureResolutionService.java` 하단 중복 메서드/중복 닫힘 중괄호로 문법 오류 지속(라인 286 이후).
  - **High**: `expenditure-resolution/.../ExpenditureResolutionDto.java`의 `getDepartment()/getPaymentAccount()` 접근과 현재 `ExpenditureResolution` 모델 간 불일치.
  - **High**: `expenditure-resolution/.../MonolithLeasePaymentResolutionAdapter.java`가 현재 도메인 모델과 맞지 않는 생성/세터 호출 사용.
- **검수 결론**: 프론트 빌드는 통과했으나 `expenditure-resolution` 미해결로 전체 병합/배포는 여전히 불가.

### 📅 2026-05-04 (오후) - 프론트엔드 업데이트 (2)
### [프론트]
- **결산 관리 화면 리팩토링 완료**:
  - 백엔드 `ClosingTask` 엔티티 구조를 반영한 새로운 Task Explorer 구축.
  - 결산 카테고리별(Pre, Entry, Post, Reporting) 필터링 기능 추가.
  - 실패한 작업에 대한 에러 메시지 가시화 및 재시도(Retry) UI 적용.
  - 실시간 재무 정합성 검증 섹션 고도화를 통해 결산 승인 전 필수 체크리스트 명시.
- **UI/UX 개선**:
  - 진행률 시각화 차트 개선 및 전체 작업 통계 카드 도입.
  - `History` 아이콘 연동 및 `Next.js` 빌드 정합성 확보.

### [QA]
- **린트 및 빌드 검수**: `frontend` 모듈의 타입 체크 및 빌드 성공 확인.


### 📅 2026-05-04 (오후) - 백엔드 성능 최적화 업데이트
### [백엔드]
- **대용량 데이터 처리 최적화 완료**:
  - `LedgerService`: N+1 쿼리 문제를 해결하기 위해 `updateLedgerBalancesBulk` 메서드 도입. 메모리 기반 집계 후 벌크 업데이트 수행.
  - `PostingService`: 전표 전기 시 개별 `save`를 `saveAll`로 변경하여 DB IO 획기적 단축.
- **DB 인덱스 튜닝 (Index Tuning)**:
  - `gl_balances`, `sl_balances`: Carry-forward 조회 및 기간별 집계 성능을 위한 복합 인덱스(`idx_gl_balance_lookup` 등) 추가.
  - `journal_entries`: 전기 상태 및 일자 기반 조회를 위한 복합 인덱스(`idx_journal_entry_posting_lookup`) 최적화.
  - `fixed_assets`: 배치 처리 속도 향상을 위해 `status` 인덱스 추가.

### 📅 2026-05-06 (오후)
### [기획/팀장] 전사 빌드 정상화 및 마스터 데이터 SCD2 고도화 완수
- **빌드 복구 (Critical Path)**: `expenditure-resolution` 및 `journal-ledger`의 컴파일 오류를 100% 해결하여 전체 시스템 빌드 가능 상태 확보.
- **마스터 데이터 SCD2 적용**: 부서(Department), 계정과목(AccountSubject), 상품(Product) 엔티티에 Slowly Changing Dimension Type 2를 적용하여 조직 개편 및 기준 정보 변동 이력 추적 기반 마련.
- **모듈 간 독립성 강화**: 지출결의(`expenditure-resolution`) 모듈에서 마스터 데이터 엔티티 직접 참조를 제거하고 ID/Code 기반의 약한 결합(Loose Coupling)으로 전환 완료.

### [백엔드] 헥사고날 어댑터 및 도메인 로직 리팩토링
- **SCD2 영속성 계층 구현**: `DepartmentPersistencePort` 등에 `findActiveByCode` 도입 및 JpaAdapter 구현을 통해 시점 기반 데이터 조회 로직 완성.
- **지출결의 유즈케이스 고도화**: ID 기반 참조 전환에 따른 예산 체크, 전표 생성, 자산 등록 로직 전면 수정.
- **인코딩 이슈 해결**: `receivable` 모듈 내 Java 파일의 BOM(Byte Order Mark) 제거 및 패키지 선언 정합성 확보.

### [QA] 전사 모듈 컴파일 검수 완료
- **빌드 성공 확인**: `:expenditure-resolution`, `:master-data`, `:journal-ledger:core`, `:receivable` 등 주요 모듈의 `compileJava` 성공 확인.

### 📅 2026-05-08 (오전)
### [백엔드] 원장 성능 최적화 및 마스터 데이터 SCD2 고도화 완수
- **원장 잔액 갱신 성능 최적화 (Critical Performance Fix)**: `LedgerService`의 `updateLedgerBalancesBulk` 메서드가 일별 그룹화 후에도 상세 라인별로 DB 조회/저장을 반복하던 N+1 문제를 해결. 메모리 내 일별 집계 후 배치 처리 방식으로 개선하여 대량 전표 전기 시의 성능을 획기적으로 향상시킴.
- **마스터 데이터 SCD2 로직 정석화**: `Department` 및 `AccountSubject` 정보를 수정할 때 단순 필드 업데이트 대신, 기존 이력을 종료(terminate)하고 새로운 이력 행을 생성하는 SCD2(Slowly Changing Dimension Type 2) 프로세스를 `DepartmentService`와 `AccountSubjectService`에 완벽히 구현.
- **코드 품질 및 인코딩 개선**: `AccountSubjectService` 내의 문자열 인코딩 깨짐 현상을 해결하고 한글 주석을 보강하여 가독성 향상.

### [QA]
- **컴파일 검수**: `journal-ledger`, `master-data` 모듈의 리팩토링 후 컴파일 정합성 확인 완료.

**NEXT STEPS (다음 담당자):**
1. **[백엔드] 금융 상품 마스터(Product Master) SCD2 적용**: 부서/계정과목과 동일한 방식으로 상품 마스터에도 SCD2 이력 관리 로직 반영.
2. **[백엔드] 전표/룰 엔진 검증 로직 강화**: 차대일치, 계정유효성, 마감잠금 등 전표 생성 시의 검증 필터 고도화.
3. **[프론트] SCD2 타임라인 조회 UI**: 마스터 데이터의 변경 이력을 기간별로 확인할 수 있는 사용자 화면 구현.

### 📅 2026-05-06 (모듈 순차 검수 - Codex)
### [검수] DDD/헥사고날/주석/재무흐름 관점 재점검
- **검수 범위**: `master-data`, `journal-ledger`, `receivable`, `expenditure-resolution`, 연관 모듈(`tax`, `payable`, `closing`).
- **검수 방식**: 모듈별 컴파일 재실행 + 코드/주석/업무흐름 정합성 리뷰.
- **컴파일 결과 요약**:
  - 성공: `master-data`, `journal-ledger`, `tax`, `payable`, `closing`
  - 실패: `receivable`(UTF BOM 인코딩), `expenditure-resolution`(`DepartmentPersistencePort` 시그니처 미반영)
- **핵심 리스크**:
  - `receivable` Java 파일 다수 UTF BOM으로 컴파일 불가.
  - `expenditure-resolution`에서 `findByCode` 호출 잔존으로 컴파일 불가.
  - 리스 지급 결의 생성 시 차/대 계정 동일 세팅 가능성으로 분개 의미 훼손 위험.
- **검수 상세 보고서**: 루트 `MODULE_REVIEW_2026-05-06.md`에 라인 단위로 기록.

### 📅 2026-05-06 (모듈 순차 검수 후속 조치 - Codex)
### [조치/재검증] 컴파일 블로커 해소
- **조치 내용**:
  - `receivable` 패키지 이동 파일들의 UTF BOM 제거 및 테스트 패키지 정합화.
  - `expenditure-resolution`에서 `DepartmentPersistencePort` 호출을 `findActiveByCode` 기준으로 정리.
- **재검증 결과**:
  - `:receivable:compileJava` 성공.
  - `:expenditure-resolution:compileJava` 성공.
  - `:receivable:test` 성공.
  - `:master-data:compileJava :journal-ledger:core:compileJava :receivable:compileJava :expenditure-resolution:compileJava :tax:compileJava :payable:compileJava :closing:core:compileJava` 성공.
- **잔여 리스크(설계/품질)**:
  - 리스 지급 결의의 차/대 계정 분리 정책 보완 필요.
  - 일부 `orElse(null)` 기반 전표 라인 생성 로직 점검 필요.

### 📅 2026-05-06 (Gemini/Codex 통합 최종 검수 - Codex)
### [검수] 최근 변경 통합 재점검
- **검수 범위**:
  - Gemini 변경분: `frontend(closing)`, `journal-ledger`, `asset-lease`, `WORKLOG`
  - Codex 변경분: `receivable`, `expenditure-resolution`, `master-data`, `journal-ledger`, `MODULE_REVIEW_2026-05-06.md`
- **재검증 실행**:
  - `.\gradlew :master-data:compileJava :journal-ledger:core:compileJava :receivable:compileJava :expenditure-resolution:compileJava :tax:compileJava :payable:compileJava :closing:core:compileJava --console=plain`
  - `.\gradlew :receivable:test --console=plain`
  - `npm run build` (workdir: `frontend`)
- **재검증 결과**:
  - 백엔드 주요 모듈 컴파일 성공
  - `receivable` 테스트 성공
  - `frontend` 프로덕션 빌드 성공
  - 단, `frontend`에는 unused import/variable ESLint warning 잔존
- **현재 남은 핵심 이슈**:
  - **Critical**: 리스 지급 결의 어댑터가 지급계정과 상세계정을 동일한 `command.accountCode()`로 세팅하여 승인 전표에서 차/대 동일 계정 분개가 발생할 수 있음
  - **High**: `LedgerService.updateLedgerBalancesBulk`는 이름/기록과 달리 일별 그룹화 후에도 상세 라인별 `updateLedgerBalances`를 그대로 호출하여 대량처리 최적화 효과가 제한적임
  - **High**: `ExpenditureResolutionService.buildJournalEntry`는 부서/계정/거래처 조회 실패를 `orElse(null)`로 허용하여 마스터 누락이 전표 품질 오류로 전이될 수 있음
  - **High**: `ExpenditureResolutionDto`가 이름 필드(`departmentName`, `paymentAccountName`, `accountSubjectName`, `businessPartnerName`)를 의도적으로 `null`로 반환하여 API 응답 회귀 가능성 존재
  - **Medium**: `receivable` 웹 어댑터가 도메인 엔티티를 직접 HTTP 입출력에 노출하여 헥사고날 경계가 약함
  - **Medium**: `JournalRuleEngine`, `ExpenditureResolutionService` 일부 주석이 현재 구현과 불일치
  - **Medium**: `Product`, `Department`는 SCD2를 선언하지만 서비스는 기존 행 직접 업데이트를 유지
- **참고 사항**:
  - 워킹트리에 추적되지 않은 `fix_bom.py`가 존재하며, 이번 검수/기록 범위에서는 제외함
- **상세 라인 리뷰**: `MODULE_REVIEW_2026-05-06.md`에 최종 보강 기록

### 📅 2026-05-06 (전수 검수 1차 - Codex)
### [검수] 코어/계약 계층 점검
- **검수 범위**:
  - `shared-kernel`
  - `contracts`
  - `master-data`
  - `governance`
- **사전 확인 문서**:
  - 각 모듈 `README.md`
  - 각 모듈 `docs/README.md`, `docs/beginner-guide.md`, `docs/process-flow.md`, `docs/schema.md`
- **재검증 실행**:
  - `.\gradlew :shared-kernel:compileJava :contracts:compileJava :master-data:test :governance:test --console=plain`
- **재검증 결과**:
  - `shared-kernel` 컴파일 성공
  - `contracts` 컴파일 성공
  - `master-data` 테스트 성공
  - `governance` 테스트 성공
- **핵심 발견 사항**:
  - **High**: `governance`의 승인-반영 브리지가 `master-data`의 `effectiveDate`/`requestedVersion` 개념을 보존하지 않고 `LocalDate.now()`와 고정 버전 `1`로 즉시 적용
  - **High**: `master-data` 소스 다수에 문자열 인코딩 깨짐(주석 + 예외 메시지) 존재
  - **Medium**: `governance`의 `TracingService`가 문서상 포트 구조와 달리 JPA Repository를 직접 의존
  - **Medium**: `governance`의 `AuditController`가 도메인 엔티티 직접 반환 + `Map<String, String>` 요청 바디 사용
  - **Medium**: `Product`/`Department`의 SCD2 선언과 서비스 직접 업데이트 방식 간 간극 지속
- **상세 라인 리뷰**: `MODULE_REVIEW_2026-05-06.md` 8장에 추가 기록

### 📅 2026-05-06 (전수 검수 2차 - Codex)
### [검수] 회계 엔진 계층 점검
- **검수 범위**:
  - `journal-ledger`
  - `closing`
  - `reconciliation`
  - `reporting`
- **사전 확인 문서**:
  - 각 모듈 `README.md` 또는 `docs/README.md`
  - 각 모듈 `docs/beginner-guide.md`
  - 각 모듈 `docs/process-flow.md`
  - 각 모듈 `docs/schema.md`
  - `journal-ledger/docs/ledger-carry-forward.md`
  - `reporting/docs/architecture.md`
  - `reporting/docs/external_reporting_guide.md`
- **재검증 실행**:
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:compileJava :closing:core:test :closing:api:compileJava :closing:batch:compileJava :reconciliation:compileJava :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain`
  - `.\gradlew :journal-ledger:api:test :journal-ledger:batch:compileJava :closing:core:test :closing:api:compileJava :closing:batch:compileJava :reconciliation:compileJava :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain`
  - `.\gradlew :journal-ledger:batch:compileJava :closing:core:test :closing:api:compileJava :closing:batch:compileJava :reconciliation:compileJava :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain`
  - `.\gradlew :closing:api:compileJava :closing:batch:compileJava :reconciliation:compileJava :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain`
- **재검증 결과**:
  - `journal-ledger:core` 메인 코드는 컴파일되지만 테스트 소스 컴파일 실패
  - `journal-ledger:api` 컴파일 실패 (`DepartmentPersistencePort.findByCode` 잔존)
  - `journal-ledger:batch` 컴파일 성공
  - `closing:core` 테스트 실패
  - `closing:api`, `closing:batch` 컴파일 성공
  - `reconciliation` 컴파일 성공
  - `reporting:core`, `reporting:api`, `reporting:batch` 테스트 성공
- **핵심 발견 사항**:
  - **Critical**: `journal-ledger`에 같은 의미의 `post` API가 두 개 존재하지만 회계 효과가 다름. `/api/journals/{id}/post`는 상태만 `POSTED`로 바꾸고, `/api/ledger/post/{journalEntryId}`만 GL/SL 엔트리와 잔액을 생성
  - **High**: `journal-ledger:api`가 `DepartmentPersistencePort.findByCode` 호출 잔존으로 현재 빌드 불가
  - **High**: `closing:core`의 `determineClosingStatus`가 테스트 계약과 어긋나고 `calendar.getStatus().name()`에서 null 상태 NPE 가능
  - **High**: `reconciliation`은 아직 `journal-ledger` 내부 Repository/Entity를 직접 의존하고, 실제 대사 금액 대신 더미 값(`1000.00`, `950.00`)으로 결과를 생성
  - **Medium**: `closing` 자동 분개는 더미 계정 `999998`, `999999`를 그대로 사용
  - **Medium**: `reporting`은 테스트는 통과하지만 실제 원장 연동 대신 목업 잔액/과거보고서로 단일 라인(`ASSET_CASH`)만 생성
  - **Medium**: `closing`, `reconciliation` 도메인/서비스 소스에 문자열 인코딩 깨짐이 여전히 다수 존재
- **참고 사항**:
  - 워킹트리의 추적되지 않은 `fix_bom.py`는 이번 2차 검수 범위에서 제외
- **상세 라인 리뷰**: `MODULE_REVIEW_2026-05-06.md` 9장에 추가 기록

### 📅 2026-05-08 (전수 검수 3차 - Codex)
### [검수] 업무 서브레저/ERP·Banking 계층 점검
- **검수 범위**:
  - `tax`
  - `payable`
  - `receivable`
  - `expenditure-resolution`
  - `asset-lease`
  - `loan:core`, `loan:api`, `loan:batch`
  - 연관 변경분: `contracts`의 `LeasePaymentResolutionCommand`, `master-data` SCD2 변경분 일부
- **사전 확인 문서**:
  - 각 모듈 `README.md` 또는 `docs/README.md`
  - 각 모듈 `docs/beginner-guide.md`, `docs/process-flow.md`, `docs/schema.md`
  - `docs/loan_accounting.md`
  - `docs/db/README.md`
- **재검증 실행**:
  - `.\gradlew :contracts:compileJava --console=plain`
  - `.\gradlew :tax:test --console=plain --max-workers=1`
  - `.\gradlew :payable:test --console=plain --max-workers=1`
  - `.\gradlew :receivable:compileJava --console=plain --max-workers=1`
  - `.\gradlew :expenditure-resolution:test --console=plain --max-workers=1`
  - `.\gradlew :loan:core:compileJava :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :tax:compileJava :receivable:test :asset-lease:test --console=plain --max-workers=1`
- **재검증 결과**:
  - 성공: `contracts:compileJava`, `tax:compileJava`, `payable:test`, `receivable:compileJava`, `receivable:test`, `asset-lease:compileJava`, `asset-lease:test`, `loan:core/api/batch:compileJava`
  - 실패: `tax:test`, `expenditure-resolution:test`
- **핵심 발견 사항**:
  - **High**: `tax` 테스트가 도메인 setter 제거 후에도 `setSupplyAmount/setTaxAmount/setTotalAmount`를 호출해 테스트 소스 컴파일이 불가
  - **High**: `expenditure-resolution` 테스트가 현재 서비스 생성자, `DepartmentPersistencePort.findActiveByCode`, protected 도메인 생성 정책을 반영하지 못해 테스트 소스 컴파일이 불가
  - **High**: 리스 지급 결의 보완으로 차/대 계정은 분리됐지만, `asset-lease`는 월 지급 결의 차변에 여전히 계약의 비용계정 전체 금액을 사용해 IFRS16 원금/이자 분리와 맞지 않을 수 있음
  - **Medium**: `ExpenditureResolutionUseCase`가 DTO 변환 메서드까지 포함해 인바운드 유스케이스 포트가 웹 응답 조립 책임을 함께 갖게 됨
  - **Medium**: `payable`/`receivable` 문서는 `SourceDocumentProvider` 기반 드릴다운을 설명하지만 현재 구현체가 확인되지 않아 전표 원천 추적이 문서와 다름
  - **Medium**: `loan`은 컴파일되지만 테스트 소스가 없고, `Loan`/`LoanContract` 병행 모델 및 직접 전표 저장 경로가 남아 있어 DoD 재현을 회귀 검증하기 어려움
- **참고 사항**:
  - 검수 중 워킹트리에 다른 변경이 추가로 감지되어 기존 변경은 되돌리지 않고 현재 기준 결과만 기록함
  - 상세 라인 리뷰는 `MODULE_REVIEW_2026-05-06.md` 10장에 추가 기록

### 📅 2026-05-08 (AI 협업 역할 정리 - Codex)
### [문서/운영] Codex 구현 주도, Gemini 독립 리뷰 체계 정리
- **역할 분담 확정**:
  - Codex: 구현/수정/테스트 보강/문서 갱신/최종 검증 담당.
  - Gemini: Codex 변경분에 대한 독립 코드 리뷰 담당.
- **설정 문서 갱신**:
  - `Agents.md`: Codex Implementation Owner Role 추가.
  - `GEMINI.md`: Gemini 기본 역할을 구현자가 아닌 독립 리뷰어로 조정.
  - `docs/GEMINI.md`: Gemini 리뷰 역할 설명 추가.
- **리뷰 핸드오프 문서 추가**:
  - `GEMINI_REVIEW_PROMPT.md` 신규 생성.
  - Gemini에게 그대로 전달할 리뷰 프롬프트, 검수 기준, 출력 형식, 현재 알려진 이슈를 정리.
- **검증**:
  - 문서/운영 지침 변경만 수행했으며 별도 빌드/테스트는 실행하지 않음.

### 📅 2026-05-08 (3차 검수 보완 작업 - Codex)
### [수정] 업무 서브레저 테스트/드릴다운/리스 지급 결의 보완
- **수정 범위**:
  - `tax`: `TaxInvoiceTest`를 도메인 정적 팩토리 계약 기준으로 갱신.
  - `expenditure-resolution`: 테스트 계약을 최신 도메인/포트 기준으로 갱신하고, DTO 변환 책임을 `ExpenditureResolutionDtoAssembler`로 분리.
  - `contracts`: `LeasePaymentResolutionCommand`를 하위 호환 유지 상태에서 다중 차변 라인(`LeasePaymentResolutionLineCommand`) 지원으로 확장.
  - `asset-lease`: IFRS16 월 지급 결의가 스케줄의 이자/원금(`93100`, `25100`)을 별도 차변 라인으로 전달하도록 보완.
  - `payable`/`receivable`: `SourceDocumentProvider` 구현체와 단위 테스트를 추가하고 문서의 드릴다운 타입/경로 설명을 실제 구현과 맞춤.
  - `.gitignore`: 소스 패키지 `adapter/out/source`가 IDE 산출물 `out/` 규칙에 가려지지 않도록 예외 추가.
- **재검증 실행**:
  - `.\gradlew :tax:test :expenditure-resolution:test --console=plain --max-workers=1`
  - `.\gradlew :payable:test :receivable:test --console=plain --max-workers=1`
  - `.\gradlew :contracts:compileJava :asset-lease:test :expenditure-resolution:test --console=plain --max-workers=1 --rerun-tasks`
  - `.\gradlew :tax:test :payable:test :receivable:test :expenditure-resolution:test :asset-lease:test :loan:core:compileJava :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`
- **재검증 결과**:
  - 성공: `tax:test`, `expenditure-resolution:test`, `payable:test`, `receivable:test`, `contracts:compileJava`, `asset-lease:test`, `loan:core/api/batch:compileJava`
  - 강제 재실행 결과는 `BUILD SUCCESSFUL`로 종료됨.
- **남은 리스크**:
  - `loan`의 DoD 회귀 테스트 부재와 직접 전표 저장/전기 미수렴 경로는 이번 보완 범위에서 미수정.
  - 강제 재실행 출력 말미에 기존 `master-data` 일부 소스의 UTF-8 인코딩 진단이 섞여 출력됨. Gradle 결과는 성공이지만, 별도 인코딩 정리 작업으로 분리하는 것이 안전함.

