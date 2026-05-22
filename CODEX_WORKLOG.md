# CODEX WORKLOG

> Codex 에이전트의 전용 작업 이력 관리 문서입니다.
> 이전 작업 내역은 사용자 요청에 의해 초기화되었습니다.

### 📅 초기화
- 작업 이력 정리 및 초기화 완료.

## 2026-05-21 (Governance -> Auth 내부 API 토큰 보호)
- 사용자 요청: "커밋 진행하고 다음 작업 진행해".
- 선확인:
  - `git status --short --branch`: `main...origin/main [ahead 1]`, 사용자/Gemini로 보이는 기존 미커밋 변경은 범위에서 제외.
  - `docs/WORKLOG.md`, `auth/README.md`, `governance/README.md`, `governance/docs/README.md` 확인.
  - `auth/docs`는 존재하지 않음.
- 수정 내용:
  - Auth 내부 역할 할당 API에 `X-Internal-Auth-Token` 헤더 검증 추가.
  - `auth.internal-api.token` / `AUTH_INTERNAL_API_TOKEN` 설정 추가.
  - Governance Auth RestClient 역할 반영 호출에 내부 토큰 헤더 추가.
  - `governance.integrations.auth.internal-token` / `GOVERNANCE_AUTH_INTERNAL_TOKEN` 설정 추가.
  - Auth 컨트롤러 테스트와 Governance RestClient 테스트 보강.
  - 관련 README/docs/worklog/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :auth:test :governance:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
- 결과:
  - Auth/Governance 테스트 성공.
- 남은 리스크:
  - 실제 환경에서 Auth/Governance 양쪽 내부 토큰 환경변수 값이 불일치하면 승인 반영 호출이 403으로 실패한다.
  - 장기적으로는 토큰 회전, mTLS, 내부망 ACL 같은 서비스 간 인증 강화 설계가 필요하다.

## 2026-05-21 (Reporting 감독보고 제출본 버전/정정/검증)
- 사용자 요청: "다음 작업 진행해줘".
- 선확인:
  - `docs/todo.md`: 14번 재무보고/공시/감독보고 영역 확인.
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md` 확인.
  - `rg -n "@todo|TODO|todo"` 결과 코드 내 미처리 TODO 문자열은 없음.
- 수정 내용:
  - `SubmitRegulatoryReportUseCase`, `RegulatoryReportSubmissionService` 추가.
  - `RegulatoryReportSubmission` 도메인 모델 추가. 제출 전 FINAL 스냅샷, 라인 중복/금액/계층, BS 총계, IS 순액 검증.
  - `StoreRegulatoryReportSubmissionPort`와 JPA/인메모리 어댑터 추가.
  - `RPT_REGULATORY_SUBMISSION` Flyway 마이그레이션 추가.
  - `POST /api/v1/reporting/submissions/regulatory` API 추가.
  - 서비스/API/JPA 테스트와 reporting 문서, `docs/todo.md`, 워크로그/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :reporting:core:test :reporting:api:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
- 결과:
  - Reporting core/api 테스트 성공.
- 남은 리스크:
  - 실제 감독기관 제출 전문/파일 전송 및 제출 결과 수신 상태 모델은 후속 구현 필요.
  - 주석 마트와 CF 라인 매핑은 후속 과제로 남음.

## 2026-05-21 (Reporting 주석 마트 생성/조회)
- 사용자 요청: "진행해 깃 동기화도".
- Git 동기화:
  - `git fetch origin` 실행.
  - `git rev-list --left-right --count origin/main...HEAD`: `0 3`.
  - `git push origin main` 성공. `d502fae`, `42ddcc8`, `cfe79a5`가 `origin/main`에 반영됨.
- 선확인:
  - `docs/todo.md`: 14.2 주석 마트 미완료 확인.
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/schema.md`, `reporting/docs/process-flow.md`, `reporting/docs/beginner-guide.md` 확인.
- 수정 내용:
  - `DisclosureNoteMartUseCase`, `DisclosureNoteMartService` 추가.
  - `DisclosureNoteMart`, `DisclosureNoteMartEntry` 도메인 모델 추가.
  - note 번호/라인 코드/라벨 기반으로 만기, 금리, 통화, 리스크 범주 분류.
  - `LoadDisclosureNoteMartPort`, `StoreDisclosureNoteMartPort`와 JPA/인메모리 어댑터 추가.
  - `RPT_DISCLOSURE_NOTE_MART` Flyway 마이그레이션 추가.
  - `POST /api/v1/reporting/disclosure-notes/generate`, `GET /api/v1/reporting/disclosure-notes` API 추가.
  - 서비스/API/JPA 테스트와 reporting 문서, `docs/todo.md`, 워크로그/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
- 결과:
  - 첫 실행은 Gradle daemon stop으로 중단.
  - 동일 명령 재실행 성공.
- 남은 리스크:
  - 운영 수준의 공시 분류 정책은 SCD2 매핑 테이블로 분리 필요.
  - 전표/원천 이벤트 단위 drill-through는 후속 구현 필요.

## 2026-05-21 (Reporting 감독보고 매핑/제출)
- 사용자 요청: "진행해".
- 선확인:
  - `git status --short --branch`: `main...origin/main`, clean.
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/todo.md` 확인.
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md`, `reporting/docs/beginner-guide.md` 확인.
- 수정 내용:
  - `SubmitRegulatoryFilingUseCase`, `RegulatoryFilingService` 추가.
  - `RegulatoryReportMapping`, `RegulatoryFiling`, `RegulatoryFilingLine`, `RegulatoryFilingPackage`, `RegulatoryFilingReceipt` 도메인 모델 추가.
  - READY 제출본, 주석 마트, SCD2 감독보고 매핑을 조합해 제출 패키지를 생성.
  - `LocalRegulatoryFilingGatewayAdapter`로 로컬 접수 영수증 생성.
  - `RPT_REGULATORY_REPORT_MAPPING`, `RPT_REGULATORY_FILING` Flyway 마이그레이션 및 기본 FSS BS/IS 매핑 seed 추가.
  - JPA/인메모리 매핑 로더와 제출 이력 어댑터 추가.
  - `POST /api/v1/reporting/regulatory-filings/submit`, `GET /api/v1/reporting/regulatory-filings/latest` API 추가.
  - 서비스/API/JPA 테스트와 reporting 문서, `docs/todo.md`, 워크로그/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
- 결과:
  - Reporting core/api/batch 테스트 성공.
- 남은 리스크:
  - 실제 감독기관 전송 프로토콜, 인증, 반려 응답 수신은 후속 구현 필요.
  - 운영 서식 전체 필드 매핑과 버전별 검증 규칙은 추가 보강 필요.

## 2026-05-20 (Governance 승인 기반 Auth 역할 반영)
- 사용자 요청: "다음 작업 진행해줘".
- 선확인:
  - `git status --short --branch`: `main...origin/main` clean.
  - `docs/WORKLOG.md`, `governance/README.md`, `governance/docs/README.md`, `auth/README.md` 확인.
- 수정 내용:
  - Auth 내부 역할 할당 API 추가: `POST /api/auth/internal/users/{username}/role-assignments`.
  - `AuthUserRoleAssignmentUseCase`, `AuthUserRoleAssignmentService`, `AuthUserRoleAssignmentPersistencePort` 추가.
  - JPA/인메모리 역할 교체 어댑터 추가. JPA 모드는 기존 role assignments를 교체하고 `roleVersion`을 증가.
  - Governance `AUTH_USER_ROLE` 승인 apply 어댑터와 Auth RestClient 연동 추가.
  - Frontend `/admin/users` 승인 요청 payload에 `username`을 포함하고 `masterKey`를 이메일 기반으로 변경.
  - 관련 README/docs/worklog/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :auth:test :governance:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
  - `npm run build` (workdir: `frontend`)
  - `git diff --check -- auth governance frontend docs\WORKLOG.md CODEX_WORKLOG.md GEMINI_REVIEW_PROMPT.md`
- 결과:
  - Auth/Governance 테스트 성공.
  - Frontend build 성공. 기존 `closing` unused variable warning은 남음.
  - 변경 범위 diff check 성공(CRLF 경고만 출력).
- 남은 리스크:
  - 실제 MSA 환경에서 Governance -> Auth 내부 API 인증/네트워크 경로 smoke 필요.
  - 전체 `git diff --check`는 이번 범위 밖 미커밋 파일들의 trailing whitespace 때문에 실패.

## 2026-05-22 (DDD/헥사고날 업무 흐름 @todo 검수)
- 사용자 요청: "모듈들을 순차적으로 점검하면서 @todo를 남겨줘".
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/todo.md` 확인.
  - `journal-ledger`, `payable`, `receivable`, `reconciliation`, `closing`, `reporting` 주요 README/docs 및 업무 흐름 코드 확인.
  - 이번 범위의 프론트엔드 변경은 없음.
- 수정 내용:
  - GL/SL/Journal Ledger: 전표 상태 기준 혼재, 대사 집계 필터 누락, 자동분개 감사자/통화/Rule DSL null 처리 위험 `@todo`를 확인.
  - P2P/AP: 하드코딩 계정, Mock 지급 실행, 지급-open item 매칭 정합성, 감사자 기본값 위험 `@todo`를 확인.
  - O2C/AR: 하드코딩 계정, 수납 자동매칭/부분매칭 잔액 처리 위험 `@todo`를 확인.
  - Reconciliation: 모델 이원화, JSON 정책, skeleton source snapshot, 대량 매칭 성능, 중복 매칭, 조정 정책 위험에 `@todo`를 추가/확인.
  - Closing: FX/ECL 하드코딩, 가상 장부환율, idempotency, 자동 승인/전기 통제 위험에 `@todo`를 추가.
  - Reporting: 로컬 감독보고 영수증 게이트웨이의 실제 연동 대체 필요성을 `@todo`로 추가.
  - `docs/WORKLOG.md`에 검수 결과와 남은 리스크 기록.
- 검증:
  - `rg -n "@todo" journal-ledger payable receivable reconciliation closing reporting`로 주석 위치 확인.
  - `git diff --check` 성공(CRLF 경고만 출력).
- 남은 리스크:
  - 이번 작업은 검수 주석 추가이며 실제 업무 로직 보완은 후속 구현 필요.
  - 테스트는 동작 변경이 없는 주석/문서 변경 중심이라 아직 실행하지 않음.

## 2026-05-22 (결산 배치 재실행 정합성 및 대사 매칭 정합성 보강)
- 사용자 요청: "계속 진행해 줘".
- 선확인:
  - `git status --short --branch`: `main...origin/main`, 이전 검수 주석 변경 미커밋 상태 확인.
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `closing/README.md`, `closing/docs/README.md`, `reconciliation/README.md`, `reconciliation/docs/README.md` 확인.
- 수정 내용:
  - `ClosingSlipNoFactory` 추가.
  - FX/ECL 배치 전표번호를 `System.currentTimeMillis()` 대신 기준일, 배치 ID, 계정 식별자 해시로 결정적으로 생성.
  - 배치 ID 파라미터가 없을 때 현재시각 대신 기준일(`yyyyMMdd`)을 기본값으로 사용.
  - `closing:batch` 테스트 의존성을 `org.springframework.batch:spring-batch-test`로 수정.
  - 전표번호 결정성/20자 길이 테스트 추가.
  - `AutomatedMatchingEngine`에서 한 번 매칭된 전표 라인을 재사용하지 않도록 변경.
  - 은행 출금과 전표 CREDIT 라인을 음수로 비교해 반대방향 금액 매칭을 방지.
  - `closing/README.md`, `reconciliation/docs/README.md`, `docs/WORKLOG.md` 갱신.
  - `GEMINI_REVIEW_PROMPT.md`를 이번 변경 범위로 갱신.
- 실행 명령:
  - `.\gradlew :closing:batch:test --console=plain`
  - `.\gradlew :reconciliation:test --console=plain`
- 결과:
  - Closing batch 테스트 성공.
  - Reconciliation 테스트 성공.
- 남은 리스크:
  - 대사 매칭 성능은 아직 O(n*m) 구조라 대량 처리를 위한 인덱싱/DB 후보 추출이 필요하다.
  - FX/ECL 통화/정책 하드코딩과 자동 승인/전기 통제는 후속 과제로 남아 있다.

## 2026-05-22 (Reconciliation 자동매칭 후보 인덱싱)
- 사용자 요청: "계속 진행해 줘".
- 선확인:
  - `reconciliation/README.md`, `reconciliation/docs/README.md`, `AutomatedMatchingEngine`, `AutomatedMatchingEngineTest` 확인.
- 수정 내용:
  - 전표 라인을 부호 반영 금액 기준 `TreeMap` 인덱스로 구성.
  - 은행 거래 금액의 허용오차 범위에 들어오는 전표 후보만 평가하도록 변경.
  - 후보 내부는 기존 전표 라인 입력 순서를 유지하도록 정렬.
  - 허용오차 내 입력 순서 보존 테스트 추가.
  - `reconciliation/docs/README.md`, `docs/WORKLOG.md`, `GEMINI_REVIEW_PROMPT.md` 갱신.
- 실행 명령:
  - `.\gradlew :reconciliation:test --console=plain`
  - 변경 범위 `git diff --check`
- 결과:
  - Reconciliation 테스트 성공.
  - 변경 범위 diff check 성공(CRLF 경고만 출력).
- 남은 리스크:
  - 전체 `git diff --check`는 이번 범위 밖 프론트엔드 파일의 trailing whitespace로 실패한다.
  - 1억 건 이상 대사에는 인메모리 인덱스 외에 DB/배치 파티셔닝 기반 후보 조회와 청크 상태 저장이 필요하다.
