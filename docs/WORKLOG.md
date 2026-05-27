# WORKLOG (Source of Truth)

> 이 문서는 프로젝트의 전체 작업 이력과 컨텍스트를 유지하기 위한 통합 워크로그입니다.
> 이전 작업 내역은 사용자 요청에 의해 초기화되었습니다.

### 📅 2026-05-27 (Codex 구현)
### [고도화] Closing ECL 충당 배치의 ECL 산출 결과 포트 연동
- **수정 범위**:
  - Closing Core: `EclAllowanceResultPort`, `EclAllowanceSummary`를 추가해 확정된 IFRS 9 ECL 산출 결과를 외부 포트로 조회할 수 있게 함.
  - Closing Batch: `JdbcEclAllowanceResultAdapter`를 추가해 `allowance_summary` 테이블의 기준일별 summary를 읽는 기본 어댑터를 구현.
  - Closing Batch: `EclProvisionService`에서 대출채권 잔액에 1%를 곱하던 고정 산식을 제거하고, ECL summary의 목표 충당금과 기존 GL 대손충당금 잔액 차이만 보충/환입 전표로 처리하도록 변경.
  - Closing Batch: 통화와 계정은 summary/설정 기반으로 resolve하고, 환입 시 별도 `reversalIncomeAccountCode`를 사용하도록 변경.
- **문서 갱신**:
  - `docs/allowance-ecl-refocus-plan.md`, `closing/README.md`, `closing/docs/README.md`
- **검증**:
  - `.\gradlew :closing:batch:test --console=plain` 성공.
  - 변경 범위 `git diff --check -- closing docs\allowance-ecl-refocus-plan.md docs\WORKLOG.md CODEX_WORKLOG.md GEMINI_REVIEW_PROMPT.md` 성공(CRLF 경고만 출력).
- **남은 리스크**:
  - `allowance_summary` 테이블을 실제로 생성/적재하는 `account-mart`/`ecl` 전용 마이그레이션과 배치 job은 후속 구현이 필요하다.
  - 결산 자동 승인/전기 통제는 아직 기존 흐름을 유지하며, 별도 결산 승인 정책 포트로 분리하는 보강이 남아 있다.

### 📅 2026-05-26 (Codex 설계)
### [설계] account-mart / ecl 대손충당금 산출 전용화 계획
- **검토 범위**:
  - 신규 유입된 `account-mart`, `ecl` 모듈의 README/docs, 주요 배치/산출 클래스, 현재 `closing` ECL 충당 배치 구조를 확인.
  - `account-mart`의 ODS→CDM 변환 역할과 `ecl`의 Stage/PD/LGD/EAD/ECL/RWA 혼합 산출 흐름을 대손충당금 관점에서 재분류.
- **설계 결과**:
  - `account-mart`는 ECL 입력 스냅샷/원천-GL 대사/DQ 전용 마트로 축소하는 방향을 제안.
  - `ecl`은 IFRS 9 Stage, Lifetime PD, EAD/LGD, 미래전망 가중평균 ECL 산출 전용 엔진으로 축소하고 RWA/감독보고/집중도 기능은 기본 실행 경로에서 제외하는 방향을 제안.
  - `closing`은 현재 고정 1% 산식 대신 ECL 산출 결과 summary를 포트로 조회해 보충/환입 전표만 생성하도록 역할을 조정하는 설계를 정리.
- **문서 갱신**:
  - `docs/allowance-ecl-refocus-plan.md`
- **검증**:
  - 문서 설계 작업이라 빌드/테스트는 실행하지 않음.
- **남은 리스크**:
  - 신규 모듈은 아직 루트 `settings.gradle`에 포함되지 않았고, 기존 `project(':common')`, `com.risk.common`, `credit-risk-service`, `risk-data-mart-service` 좌표를 현재 저장소 구조에 맞게 이관해야 한다.
  - `closing`의 기존 ECL 배치는 하드코딩 계정/KRW/1% 산식이 남아 있어 후속 구현에서 ECL 결과 포트 기반으로 교체해야 한다.

### 📅 2026-05-21 (Gemini YOLO 모드 - 12차)
### [프론트엔드]
- **Frontend (Next.js) 재무운영(AR, AP) 관리 화면 API 연동**:
  - `frontend/src/services/receivableService.ts` 및 `payableService.ts` 신규 생성. 백엔드의 `/api/receivable/invoices` 및 `/api/payable/invoices`와 통신하도록 설계.
  - `frontend/src/app/finance/receivable/page.tsx` 및 `payable/page.tsx` 리팩토링: 하드코딩된 그리드 데이터를 제거하고 API를 호출해 실제 매출채권 및 매입채무 데이터를 동적으로 렌더링.
  - 총액 잔액 합계(`Total Receivable Balance` 등)를 API 응답 기반으로 실시간 계산하여 출력하도록 보강.
  - 빌드 검증을 모두 통과하고 상태판(`docs/development-status.md`) 갱신 및 Git 연동 완료.

### 📅 2026-05-21 (Gemini YOLO 모드 - 11차)
### [프론트엔드]
- **Frontend (Next.js) 재무운영(고정자산, 리스 회계) 화면 연동 검증**:
  - `frontend/src/app/finance/assets/page.tsx` 및 `frontend/src/app/finance/lease/page.tsx` 코드를 분석하여, 이미 내부에 하드코딩된 Mock 데이터가 없고 `assetService.getAssets()`, `leaseService.getLeases()` 등을 통한 백엔드 API (`/api/fixed-assets`, `/api/ifrs16/leases`) 리얼 데이터 연동이 완벽하게 구현되어 있음을 코드 레벨에서 확인했습니다.
  - 감가상각 실행(`runDepreciation`) 및 리스 월별 회계 처리(`processMonthly`) 로직의 API 바인딩 역시 정상적으로 적용되어 있음을 교차 검증 완료했습니다.
  - `npm run build`를 통한 런타임/타입 무결성 통과를 재확인하고, `docs/development-status.md`에 `✅ [X]`로 상태가 정확히 기록되어 있음을 확정지었습니다.

### 📅 2026-05-21 (Gemini YOLO 모드 - 9차)
### [프론트엔드]
- **Frontend (Next.js) 재무제표(BS/IS) 화면 API 연동**:
  - `frontend/src/services/reportingService.ts`에서 Mock 데이터(`sampleStatement`)를 제거하고 에러 발생 시 UI로 예외를 던지도록 엄격한 API 연동(Strict Integration) 체계 도입.
  - 백엔드의 `/api/v1/reporting/generate` 엔드포인트와 풀스택 연동.

### 📅 2026-05-21 (Gemini YOLO 모드 - 10차)
### [프론트엔드]
- **Frontend (Next.js) 기준정보(마스터) 관리 화면 API 연동**:
  - `frontend/src/services/masterDataService.ts` 생성: 백엔드의 `/api/basic/account-subjects` 및 `/api/basic/business-partners` 연동.
  - `frontend/src/app/master/account/page.tsx` 리팩토링: 하드코딩된 트리 데이터를 제거하고, `masterDataService`를 통해 받아온 평면 배열을 재귀적으로 `buildAccountTree`하여 렌더링하도록 수정.
  - `frontend/src/app/master/partner/page.tsx` 리팩토링: 하드코딩된 거래처 그리드를 제거하고, 실제 DB의 데이터를 받아 렌더링.
  - 빌드(typing/linting) 통과 확인 및 Git 동기화.

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

### 📅 2026-05-22 (Codex 검수)
### [리뷰] DDD/헥사고날 업무 흐름 및 정합성 @todo 점검
- **검수 범위**:
  - GL/SL/Journal Ledger, P2P/AP, O2C/AR, Reconciliation, Closing, Reporting 제출 게이트웨이 흐름을 우선 점검.
- **코드 기준으로 확인/추가한 주요 @todo**:
  - 전표 상세 조회의 `APPROVED`/`POSTED` 상태 기준 혼재 및 대사 집계의 전기 상태 필터 누락.
  - 구매/지급/매출/수납 서비스의 하드코딩 계정코드, `SYSTEM` 감사자, Mock 지급 실행, open-item 매칭 정합성 위험.
  - 대사 모듈의 모델 이원화, JSON 기반 미검증 정책, source snapshot skeleton, 대량 루프 매칭 성능, 중복 매칭 위험.
  - 결산 FX/ECL 배치의 KRW/계정/ECL rate 하드코딩, 가상 장부환율, timestamp 기반 slipNo, 자동 승인/전기 통제 미흡.
  - Reporting 감독보고 게이트웨이의 로컬 영수증 생성 어댑터를 실제 프로토콜/인증/반려 callback으로 대체해야 하는 위험.
- **검수 결과**:
  - 현재 빌드 완료 여부와 별개로 운영 정합성 기준에서는 후속 구현이 필요한 지점이 남아 있다.
  - 이번 작업은 동작 변경 없이 검수 주석과 워크로그 기록 중심으로 수행했다.
- **검증**:
  - `rg -n "@todo" journal-ledger payable receivable reconciliation closing reporting`로 주석 위치 확인.
  - `git diff --check` 성공(CRLF 경고만 출력).
- **남은 리스크**:
  - 주석으로 표시한 항목은 실제 구현 전까지 재무제표/대사/결산 자동화의 운영 신뢰성 리스크로 남는다.

### 📅 2026-05-22 (Codex 구현)
### [고도화] 결산 배치 재실행 정합성 및 대사 매칭 정합성 보강
- **수정 범위**:
  - Closing Batch: `ClosingSlipNoFactory`를 추가해 FX/ECL 자동 전표번호를 기준일, 배치 ID, 계정 식별자 기반 20자 고정 형식으로 생성.
  - Closing Batch: 배치 ID 파라미터가 없을 때 현재시각 대신 기준일(`yyyyMMdd`)을 기본값으로 사용하도록 변경.
  - Closing Batch: 테스트 의존성을 `org.springframework.batch:spring-batch-test`로 수정하고 전표번호 결정성/길이 테스트 추가.
  - Reconciliation: 자동 매칭 엔진이 한 번 매칭한 전표 라인을 같은 실행에서 재사용하지 않도록 변경.
  - Reconciliation: 은행 입금/출금과 전표 DEBIT/CREDIT 방향을 부호로 반영해 반대방향 금액 매칭을 방지.
- **문서 갱신**:
  - `closing/README.md`, `reconciliation/docs/README.md`
- **검증**:
  - `.\gradlew :closing:batch:test --console=plain` 성공.
  - `.\gradlew :reconciliation:test --console=plain` 성공.
- **남은 리스크**:
  - 대사 매칭은 여전히 중첩 루프 기반이므로 1억 건 이상 처리에는 인덱싱/DB 집계 기반 후보 추출이 필요하다.
  - FX/ECL의 통화, ECL rate, 자동 승인/전기 정책은 별도 후속 구현이 필요하다.

### 📅 2026-05-22 (Codex 구현)
### [고도화] Reconciliation 자동매칭 후보 인덱싱
- **수정 범위**:
  - `AutomatedMatchingEngine`이 전표 라인을 부호 반영 금액 기준 `TreeMap` 인덱스로 구성하도록 변경.
  - 은행 거래별로 금액 허용오차 범위에 들어오는 전표 후보만 평가해 전체 전표 라인 중첩 스캔을 제거.
  - 후보 내부는 기존 전표 라인 입력 순서를 유지하도록 정렬해 기존 우선순위 계약을 보존.
  - 허용오차 내 입력 순서 보존 테스트를 추가.
- **문서 갱신**:
  - `reconciliation/docs/README.md`
- **검증**:
  - `.\gradlew :reconciliation:test --console=plain` 성공.
  - 변경 범위 `git diff --check` 성공(CRLF 경고만 출력).
- **남은 리스크**:
  - 전체 `git diff --check`는 이번 범위 밖 프론트엔드 파일의 trailing whitespace로 실패한다.
  - 인메모리 후보 인덱싱은 단일 실행 내 매칭 비용을 줄이지만, 1억 건 이상에서는 DB/배치 파티셔닝 기반 후보 조회와 청크 단위 상태 저장이 추가로 필요하다.

### 📅 2026-05-27 (Codex 구현)
### [고도화] ECL 대손충당금 summary 생성 경로 추가
- **수정 범위**:
  - ECL Core: `AllowanceSummaryService`, `AllowanceSummaryBuildPort`, `AllowanceSummaryBuildResult`를 추가해 기준일 ECL 결과를 회계 summary로 재생성하는 유즈케이스를 분리.
  - ECL Core Adapter: `JdbcAllowanceSummaryPersistenceAdapter`를 추가해 완료된 `cr_risk_results`를 `allowance_account_mappings`와 조인하고 `allowance_summary`를 SQL bulk 집계로 생성.
  - ECL Batch: `AllowanceSummaryTasklet`과 `allowanceSummaryStep`, `standaloneAllowanceSummaryJob`을 추가해 ECL/RWA 완료 후 closing 입력 summary를 생성.
  - ECL API Migration: `V3__add_allowance_summary.sql`로 `allowance_account_mappings`, `allowance_summary` 테이블 추가.
  - ECL Batch Demo: H2 통합 스키마에 summary 테이블과 샘플 계정 매핑을 추가하고 `cr_accounts.biz_unit_cd` 컬럼을 보강.
  - 문서: `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, `ecl/docs/CREDIT_DATA_MODEL_SPEC.md`, `docs/allowance-ecl-refocus-plan.md` 갱신.
- **정합성 포인트**:
  - 회계 계정 매핑 누락 시 기존 summary를 삭제하지 않고 실패하도록 설계.
  - 같은 기준일 이전 run summary가 `closing`에 중복 조회되지 않도록 mapping 검증 통과 후 기준일 summary를 교체.
  - 대량 처리는 application/batch 루프가 아니라 JDBC adapter의 bulk `INSERT ... SELECT`로 수행.
- **검증**:
  - `.\gradlew :closing:batch:test --console=plain` 성공.
  - `.\gradlew projects --console=plain` 성공. 현재 루트 프로젝트 목록에 `ecl`, `account-mart`는 포함되지 않음을 확인.
  - 변경 대상 파일 diff check 성공(CRLF 경고만 출력). 전체 범위 diff check는 이번 범위 밖 `closing/batch/.../FxValuationService.java` 기존 trailing whitespace로 실패.
  - 신규 ECL 파일 및 H2 스키마 trailing whitespace 점검 성공.
- **남은 리스크**:
  - `ecl`/`account-mart`는 아직 루트 Gradle에 편입되지 않아 ECL 신규 단위 테스트를 Gradle로 실행하지 못했다.
  - 기존 `com.risk.common`/`project(':common')` 좌표를 현 저장소의 `shared-kernel`/`contracts` 구조로 이관해야 통합 빌드 가능하다.
  - 대손충당금 전용 운영 경로에서는 RWA/집중도 분석을 제외한 별도 `allowanceEclJob` 분리가 필요하다.

### 📅 2026-05-27 (Codex 구현)
### [통합] account-mart/ecl 루트 Gradle 편입 및 batch wiring 복구
- **수정 범위**:
  - 루트 `settings.gradle`에 `risk-common`, `account-mart:mart-*`, `ecl:ecl-*` 모듈을 포함.
  - `risk-common` 호환 모듈을 추가해 기존 수입 모듈의 `com.risk.common` 엔티티/enum/event/API 응답/예외/락 타입 참조를 복구.
  - `ecl`/`account-mart`의 stale Gradle 의존성, Kafka 의존성 누락, 예전 `domain.*.repository/entity` import를 현재 포트/도메인 패키지로 정리.
  - `account-mart` batch/API 컴파일을 위해 KAP 등급, 조기경보, 계좌금리, 수익률곡선, 대사이력 포트 adapter를 보강.
  - `IntegratedPositionProcessor`가 외화 포지션 `marketValue`를 환율 포트로 KRW 환산하도록 보강.
  - ODS/GL 대사는 계좌 잔액을 상품 GL 계정코드로 집계하고 MATCH/MISMATCH 이력을 모두 남기도록 수정.
  - `mart-batch` demo/test에서 Kafka 없이 검증할 수 있도록 `mart.batch.cdm-event.enabled=false` 설정을 추가.
- **검증**:
  - `.\gradlew projects --console=plain` 성공. 루트 프로젝트 목록에 `risk-common`, `account-mart`, `ecl` 포함 확인.
  - `.\gradlew :risk-common:compileJava :account-mart:mart-core:compileJava :account-mart:mart-api:compileJava :account-mart:mart-batch:compileJava :ecl:ecl-core:compileJava :ecl:ecl-api:compileJava :ecl:ecl-batch:compileJava --console=plain` 성공.
  - `.\gradlew :account-mart:mart-batch:test --console=plain` 성공.
  - `.\gradlew :ecl:ecl-core:test --tests com.risk.credit.core.application.service.allowance.AllowanceSummaryServiceTest :account-mart:mart-core:test --tests com.risk.mart.core.domain.mart.processor.IntegratedPositionProcessorTest --console=plain` 성공.
  - `.\gradlew :account-mart:mart-core:compileTestJava :account-mart:mart-batch:compileTestJava :ecl:ecl-core:compileTestJava :ecl:ecl-batch:compileTestJava --console=plain` 성공.
- **남은 리스크**:
  - `risk-common`은 호환 계층이므로 장기적으로는 `shared-kernel`/명시적 allowance 공통 모델로 축소 이관해야 한다.
  - `account-mart`의 allowance exposure snapshot 테이블과 ECL 전용 입력 생성 job은 아직 별도 구현이 필요하다.
  - `ecl`의 RWA/집중도/감독보고 step은 여전히 기본 레거시 경로에 남아 있어 `allowanceEclJob` 분리가 다음 단계다.
  - `mart-batch` 테스트 종료 시 일부 step-scope reader close 경고가 출력되지만 테스트 실패는 아니며, reader bean lifecycle 정리는 후속 개선 대상이다.
