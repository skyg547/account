# WORKLOG (Source of Truth)

> 이 문서는 프로젝트의 전체 작업 이력과 컨텍스트를 유지하기 위한 통합 워크로그입니다.
> 이전 작업 내역은 사용자 요청에 의해 초기화되었습니다.

### 📅 2026-05-21 (Gemini YOLO 모드 - 7차)
### [프론트엔드]
- **Frontend (Next.js) UI 연동 및 개발 진입**:
  - `frontend/src/services/closingService.ts` 신규 생성. 결산 태스크 조회 및 '외화 평가(FX)', '기대신용손실(ECL)' 배치 재실행(Retry Batch) API 연동 로직 추가.
  - `frontend/src/app/closing/page.tsx` 결산 관리 화면 수정. 하드코딩된 Mock 데이터를 `closingService`를 통해 API로 연동하도록 변경 (`useEffect` 사용).
  - 결산 관리 화면 내 "RETRY BATCH" 버튼 클릭 시 실제로 `runValuationBatch` 및 `runProvisionBatch` API를 호출하여 배치를 재가동하도록 `handleRetryBatch` 로직 연동 완료.
  - `frontend/docs/development-status.md`에 결산 관리(Closing) 화면 API 연동 상태 업데이트(`✅ [X]`).

### 📅 2026-05-21 (Gemini YOLO 모드 - 6차)
### [백엔드]
- **결산(Closing) 모듈 대손충당금(ECL) 및 결산조정 배치 구현**:
  - `closing/batch` 하위에 `EclProvisionService` 및 `EclProvisionBatchConfig`를 신규 구현.
  - 원장(GL)의 대출채권 잔액을 기반으로 목표 대손충당금(ECL)을 산출하고, 기존 충당금 잔액을 차감하는 '보충법' 방식의 배치 처리 로직 완성.
  - 대손상각비(비용)와 대손충당금(부채성 자산차감) 자동 분개 생성(Journal Entry) 연동 및 테스트 컴파일 검증 성공.
  - `docs/todo.md`의 `12.4 충당/손상(ECL 연계)` 및 `12.5 결산조정/재분류` 완료(o) 마킹 및 Git 동기화 완료.

### 📅 2026-05-21 (Gemini YOLO 모드 - 5차)
### [백엔드 & 모델러]
- **대출 모듈 11.4 & 11.5 기 구현 확인**:
  - `LoanService.recalculateLoan` 및 `EIRCalculator` 내부에 중도상환, 조건변경 시의 Newton-Raphson 기반 EIR 재계산과 상각 전표, 잔액 조정 전표 발행 로직이 이미 완벽히 구현되어 있음을 코드 및 테스트(`LoanServiceTest`)로 확인. `todo.md`에 완료(o) 마킹.
- **결산(Closing) 모듈 외화평가(FX Valuation) 배치 구현**:
  - `closing/batch` 모듈을 신규 구성 (Spring Batch, JPA 설정 포함).
  - `GlAccountBalanceRepository`에 특정 일자 기준 외화(비 KRW) 잔액을 조회하는 `findLatestForeignCurrencyBalances` 네이티브 Query 추가.
  - `FxValuationService` 생성: 기말 환율을 조회하여 장부 원화 금액과 평가 원화 금액의 차액을 계산하고, "외화환산이익/손실" 회계 전표(Journal Entry)를 자동 발행하는 핵심 로직 구현 및 주석(`🐣 초보자를 위한 설명`) 작성.
  - `FxValuationBatchConfig` 생성: 기말 평가 대상을 조회하여 평가 서비스를 호출하는 Spring Batch (Reader/Processor/Writer) 구성 완료.
  - `closing:batch:compileJava` 성공 확인 및 백그라운드 데몬 정리 완료.

### 📅 2026-05-21 (Gemini YOLO 모드 - 4차)
### [백엔드]
- **대출(Loan) 모듈 일일 EIR 상각 배치 구현**:
  - `loan/batch` 하위에 `LoanInterestAccrualBatchConfig.java` 신규 생성.
  - Spring Batch(Reader/Processor/Writer)를 활용해 매일 자정 ACTIVE 대출의 상각 전표(Journal) 생성 로직 구현 및 주석 추가.
  - `InterestAccrualService`의 개별 상각 처리 메서드를 Public 으로 노출.
  - `loan:batch:compileJava` 성공 확인 및 백그라운드 Gradle 데몬 정리 완료.

### 📅 초기화
- 전체 작업 이력 정리 및 초기화 완료.

### 📅 2026-05-21 (Codex 구현)
### [보안 보강] Governance -> Auth 내부 역할 반영 API 토큰 보호
- **수정 범위**:
  - Auth: 내부 역할 할당 API `POST /api/auth/internal/users/{username}/role-assignments`에 `X-Internal-Auth-Token` 검증을 추가.
  - Auth: `auth.internal-api.token` 설정과 `AUTH_INTERNAL_API_TOKEN` 환경변수 기본값을 추가.
  - Governance: Auth RestClient 역할 반영 호출에 `X-Internal-Auth-Token` 헤더를 포함하도록 보강.
  - Governance: `governance.integrations.auth.internal-token` 설정과 `GOVERNANCE_AUTH_INTERNAL_TOKEN` 환경변수 기본값을 추가.
  - 테스트: Auth 컨트롤러 내부 토큰 검증 테스트와 Governance RestClient 헤더 전송/토큰 설정 검증 테스트를 추가.
- **문서 갱신**:
  - `auth/README.md`, `governance/README.md`, `governance/docs/README.md`
- **검증**:
  - `.\gradlew :auth:test :governance:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
- **남은 리스크**:
  - 운영/스테이징 배포 시 `AUTH_INTERNAL_API_TOKEN`과 `GOVERNANCE_AUTH_INTERNAL_TOKEN`을 같은 값으로 주입해야 한다.
  - 토큰 회전, mTLS, 네트워크 ACL 같은 운영 수준의 서비스 간 인증 정책은 별도 설계가 필요하다.

### 📅 2026-05-21 (Codex 구현)
### [고도화] Reporting 감독보고 제출본 버전/정정/검증
- **수정 범위**:
  - Reporting Core: `SubmitRegulatoryReportUseCase`와 `RegulatoryReportSubmissionService`를 추가해 확정 재무제표 스냅샷을 감독보고 제출본으로 등록.
  - Reporting Domain: `RegulatoryReportSubmission` 모델을 추가하고 제출 전 필수 라인/금액, 중복 라인, BS 총계, IS 순액 검증을 수행.
  - Reporting Persistence: JPA/인메모리 제출본 저장 어댑터와 `RPT_REGULATORY_SUBMISSION` Flyway 마이그레이션 추가.
  - Reporting API: `POST /api/v1/reporting/submissions/regulatory` 엔드포인트 추가. 2차 제출부터 정정 사유를 필수로 검증.
  - `docs/todo.md`의 14.1, 14.4, 14.5 완료 표시 보강.
- **문서 갱신**:
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md`
- **검증**:
  - `.\gradlew :reporting:core:test :reporting:api:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
- **남은 리스크**:
  - 실제 감독기관 제출 전문/파일 전송, 제출 결과 수신, 반려 후 재제출 상태 모델은 아직 별도 구현이 필요하다.
  - 주석 마트(만기/금리/통화/리스크)와 CF 라인 매핑은 후속 작업 범위로 남아 있다.

### 📅 2026-05-21 (Codex 구현)
### [고도화] Reporting 주석 마트 생성/조회
- **수정 범위**:
  - Reporting Core: `DisclosureNoteMartUseCase`와 `DisclosureNoteMartService`를 추가해 확정 재무제표 스냅샷의 주석 라인을 마트로 전개.
  - Reporting Domain: `DisclosureNoteMart`, `DisclosureNoteMartEntry`를 추가하고 주석 번호/라인 코드 기반으로 만기, 금리, 통화, 리스크 범주를 분류.
  - Reporting Persistence: JPA/인메모리 주석 마트 어댑터와 `RPT_DISCLOSURE_NOTE_MART` Flyway 마이그레이션 추가.
  - Reporting API: `POST /api/v1/reporting/disclosure-notes/generate`, `GET /api/v1/reporting/disclosure-notes` 엔드포인트 추가.
  - `docs/todo.md`의 14.2 완료 표시.
- **문서 갱신**:
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md`, `reporting/docs/beginner-guide.md`
- **검증**:
  - 첫 `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 실행은 Gradle daemon stop으로 중단.
  - 동일 명령 재실행 성공.
- **남은 리스크**:
  - 현재 분류는 note 번호와 라인 코드/라벨 기반 기본 규칙이다. 운영 수준에서는 별도 SCD2 공시 분류 매핑 테이블로 정책화가 필요하다.
  - 공시 주석의 상세 원천 drill-through는 아직 보고 라인 코드 수준이며 전표/원천 이벤트까지의 상세 링크는 후속 작업이다.

### 📅 2026-05-21 (Codex 구현)
### [고도화] Reporting 감독보고 매핑/제출
- **수정 범위**:
  - Reporting Core: `SubmitRegulatoryFilingUseCase`와 `RegulatoryFilingService`를 추가해 READY 제출본과 주석 마트를 감독보고 제출 패키지로 변환.
  - Reporting Domain: `RegulatoryReportMapping`, `RegulatoryFiling`, `RegulatoryFilingLine`, `RegulatoryFilingPackage`, `RegulatoryFilingReceipt` 모델 추가.
  - Reporting Persistence: 감독보고 SCD2 매핑 로더, 제출 이력 JPA/인메모리 어댑터와 `RPT_REGULATORY_REPORT_MAPPING`, `RPT_REGULATORY_FILING` Flyway 마이그레이션 추가.
  - Reporting Infrastructure: `LocalRegulatoryFilingGatewayAdapter`를 추가해 로컬 접수 영수증을 생성.
  - Reporting API: `POST /api/v1/reporting/regulatory-filings/submit`, `GET /api/v1/reporting/regulatory-filings/latest` 엔드포인트 추가.
  - `docs/todo.md`의 14.3 완료 표시.
- **문서 갱신**:
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md`, `reporting/docs/beginner-guide.md`
- **검증**:
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
- **남은 리스크**:
  - 현재 제출 게이트웨이는 로컬 접수 영수증 생성 어댑터이며, 실제 감독기관 전송 프로토콜/인증/반려 응답 처리는 후속 구현이 필요하다.
  - 매핑 seed는 기본 BS/IS 일부 필드만 포함하며, 운영 서식 전체 필드와 버전별 검증 규칙은 추가 보강이 필요하다.

### 📅 2026-05-20 (Codex 구현)
### [수정] Governance 승인 기반 Auth 사용자 역할 반영 연결
- **수정 범위**:
  - Auth: `AuthUserRoleAssignmentUseCase`와 내부 API `POST /api/auth/internal/users/{username}/role-assignments`를 추가해 승인된 역할 목록으로 기존 역할 할당을 교체하고 `roleVersion`을 증가시키도록 구현.
  - Auth: `JpaAuthUserRoleAssignmentAdapter`와 인메모리 대응 어댑터를 추가해 JPA/메모리 모드 모두 역할 교체 흐름을 지원.
  - Governance: `AUTH_USER_ROLE` 승인 apply 어댑터를 추가해 승인 payload의 `username`, `role/roleCode/roles`, `dataScope`를 Auth 내부 API 호출로 변환.
  - Governance: `GOVERNANCE_AUTH_BASE_URL` 기반 Auth RestClient 연동 설정을 추가.
  - Frontend: `/admin/users` 역할 변경 승인 요청 payload에 Auth `username`을 포함하고 `masterKey`도 사용자 이메일 기반으로 변경.
- **문서 갱신**:
  - `auth/README.md`, `governance/README.md`, `governance/docs/README.md`, `frontend/docs/screen-inventory.md`
- **검증**:
  - `.\gradlew :auth:test :governance:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"` 성공.
  - `npm run build` (workdir: `frontend`) 성공. 기존 closing 화면 unused variable warning은 남음.
  - `git diff --check -- auth governance frontend docs\WORKLOG.md CODEX_WORKLOG.md GEMINI_REVIEW_PROMPT.md` 성공(CRLF 경고만 출력).
- **남은 리스크**:
  - 실제 분산 환경에서 Governance -> Auth 네트워크 경로와 내부 API 인증 정책은 별도 smoke/E2E 검증이 필요.
  - 전체 `git diff --check`는 이번 범위 밖의 기존 미커밋 변경(journal-ledger/master-data/payable/receivable 등 trailing whitespace) 때문에 실패했다.

### 📅 2026-05-20 (Gemini YOLO 모드 - 3차)
### [기획/팀장 & 백엔드]
- **전체 모듈 순차 점검 및 주석 고도화 (DDD, 헥사고날, 초보자 가이드)**:
  - **04. 마스터(Master Data)**: \AccountSubject\(계정과목), \AccountSubjectService\ 에 SCD2 원칙 및 초보자 설명 추가.
  - **05. 전표/룰 엔진(Journal Ledger)**: \JournalRule\(분개 규칙), \JournalRuleEngine\ 에 자동 분개 과정 설명 추가.
  - **06. 원장(GL)**: \GlAccountBalance\ 에 실시간 잔액 계산 목적 설명 추가.
  - **07. P2P/AP (Payable)**: \PurchaseInvoice\, \Payable\ 에 외상매입금 생성 및 결제 흐름 설명 추가.
  - **08. O2C/AR (Receivable)**: \SalesInvoice\, \Receivable\ 에 매출채권 및 연령(Aging) 분석 설명 추가.
  - **09. 고정자산(FA)**: \FixedAsset\, \FixedAssetEntryService\ 에 감가상각 및 처분 흐름 설명 추가.
  - **10. 리스(Lease)**: \LeaseContract\, \LeaseEntryService\ 에 IFRS16 기반 사용권자산/부채 상각 설명 추가.
  - **11. 대출(Loan)**: \Loan\, \LoanService\ 에 상환 스케줄 생성 및 유효이자율(EIR) 역할 설명 추가.
  - **12. 결산(Closing)**: \ClosingPeriod\, \ClosingService\ 에 마감잠금 및 재오픈 승인 프로세스 설명 추가.
  - **13. 대사(Reconciliation)**: \ReconciliationRun\, \ReconciliationService\ 에 이기종 데이터 대조 및 차이 조정 설명 추가.
- **상태 업데이트**: 전 모듈에 대한 업무 주석 및 DDD 아키텍처 코멘트 적용 완료.
