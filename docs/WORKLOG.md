# WORKLOG (Source of Truth)

> 이 문서는 프로젝트의 전체 작업 이력과 컨텍스트를 유지하기 위한 통합 워크로그입니다.
> 이전 작업 내역은 사용자 요청에 의해 초기화되었습니다.

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
