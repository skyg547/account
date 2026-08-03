$logPath = "docs/WORKLOG.md"
$existingContent = Get-Content -Path $logPath -Raw

$newEntry = @"
### 📅 2026-07-29 (전사 모듈 전수 조사 및 이슈 생성)
### [전수 조사] 51개 서브모듈 + 프론트엔드 전수 조사 완료 및 중복 없는 GitHub Issue 25건 생성
- **작업 배경**:
  - `account` 저장소 내 전체 모듈(51개 서브모듈 + 프론트엔드)의 헥사고날 구조, 코드 품질, 테스트, 문서화, Application 설정, 빌드/의존성, 보안 상태 전수 검사 수행.
- **검사 결과 요약**:
  - 18개 모듈의 `src/test/java` 테스트 클래스 0건 식별
  - 14개 모듈 70+건의 `return null;` 기술 부채 식별
  - `master-data:api`, `internal-audit:api` 모듈의 Application 클래스 누락 및 `auth:batch` 단일라인 클래스 식별
  - `account-mart` 및 `ecl-batch` 설정 파일 내 평문 비밀번호 노출 식별
- **조치 사항**:
  - `audit_report.md` 작성 및 저장소 내 기존 이슈들과 중복 대조 검증
  - 중복 없이 25건의 독립적인 GitHub Issue 생성 완료 (#157, #160~#182)
- **검증 명령**:
  - `gh issue list --state open --limit 50`

"@

Set-Content -Path $logPath -Value ($newEntry + $existingContent) -Encoding UTF8
Write-Host "Updated docs/WORKLOG.md successfully"
