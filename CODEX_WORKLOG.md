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
