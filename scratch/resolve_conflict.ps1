$logPath = "docs/WORKLOG.md"
$content = Get-Content -Path $logPath -Raw

# Replace conflict block with merged entries
$mergedText = @"
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

### 📅 2026-07-29 (Codex 수정)
### [수정] Issue #40 Master Data API/Core/Batch 물리 모듈 분리
- **작업 배경**:
  - `settings.gradle`에는 `master-data:core`, `:api`, `:batch`가 등록돼 있었지만 Controller/DTO와 API 실행점, Batch orchestrator가 모두 core 소스에 남아 실제 배포 경계가 분리되지 않았다.
  - API `bootJar`가 비활성화되고 문서·Docker·Run Configuration이 존재하지 않는 `:master-data` 실행 task를 가리켜 독립 실행이 불가능했다.
- **수정 범위**:
  - REST Controller/DTO와 `MasterDataApplication`을 `master-data:api`로, Batch orchestrator/report를 `master-data:batch`로 이동하고 core에는 application/domain/infrastructure만 남겼다.
  - `masterDataValidityJob`/Tasklet Step을 추가해 필수 `asOfDate`를 검증한 뒤 core `MasterDataValidityReportPipeline`에 위임하도록 했다.
  - API/Batch를 각각 실행 가능한 `bootJar`로 구성하고, Flyway migration은 core의 표준 `src/main/resources`에 두어 두 실행 모듈이 같은 스키마 계약을 사용하게 했다.
  - Dockerfile, IntelliJ Run Configuration, Master Data README/docs와 공용 로컬 개발 문서를 실제 Gradle 경로로 갱신했다.
  - 빈 aggregator `project(':master-data')`를 참조하던 Journal Ledger core 의존을 제거하고 이미 선언된 `contracts` 경계를 유지했다.
- **검증 결과**:
  - JDK 17 컨테이너에서 Master Data core 1, API 1, Batch 4, Journal Ledger core 23개로 총 29 tests가 실패·오류·skip 없이 통과했다.
  - `:master-data:api:bootJar`, `:master-data:batch:bootJar`가 성공했다.
  - populated H2에 네 기준정보의 활성/만료 행을 각각 저장하고 실제 `masterDataValidityJob`을 실행해 Step execution context의 활성 건수 4종이 각각 1임을 확인했다.
  - Config Server가 없으면 기본 Batch가 `ConfigClientFailFastException`으로 실패하고, 명시적인 로컬 H2 예외 플래그에서는 같은 Job이 `COMPLETED`로 종료됨을 런타임으로 확인했다.
  - core의 Web/Validation/API/Batch 역참조 검색, conflict marker 검색, `git diff --check`가 통과했다.
- **남은 리스크**:
  - PostgreSQL/Flyway 실DB 기동과 대량 기준정보 성능은 별도 통합 환경에서 검증해야 한다.
  - Batch 보고 결과는 현재 Step execution context에만 남으므로 운영 장기 보관·메트릭·알림용 출력 포트가 필요하다.
  - typed applier, `requestedVersion`, 요청 잠금/`lockVersion` production 경로의 회귀 테스트는 현재 test source에 없어 별도 복원이 필요하다.
- **롤백 범위**:
  - Issue #40 커밋을 revert하면 소스·리소스·실행 설정·문서 이동이 함께 복구된다. migration 내용이나 운영 DB는 변경하지 않았다.
"@

# Regex replace from <<<<<<< HEAD to >>>>>>> ...
$pattern = "(?s)<<<<<<< HEAD.*?>>>>>>> [a-f0-9]+ \(.*?\)"
$newContent = [regex]::Replace($content, $pattern, $mergedText)

Set-Content -Path $logPath -Value $newContent -Encoding UTF8
Write-Host "Resolved conflict in docs/WORKLOG.md"
