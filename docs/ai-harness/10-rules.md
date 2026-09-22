# AI Harness Rules

## Branch And Review Rules

- All AI work must happen on a separate feature branch, agent branch, AI branch, or fix branch.
- Recommended branch prefixes:
  - `agent/`
  - `ai/`
  - `feature/`
  - `fix/`
- Do not push directly to `main`, `master`, or `develop`.
- Use Draft MR/PR for review. Do not treat direct commits on a base branch as the normal integration path.
- Do not mark merge complete without tests or an explicitly documented reason why tests could not run.
- 테스트 없이 merge 완료로 판단하지 않는다. 테스트를 못 돌렸으면 이유, 영향, 대체 검증을 기록한다.

초보자 설명: 기준 브랜치는 모두가 공유하는 원본이다. AI가 바로 밀어 넣으면 다른 사람 작업까지 깨질 수 있으므로, 먼저 작업 브랜치에서 검증하고 Draft PR로 리뷰를 받는다.


## GitHub Issue Loop Rules

- Use a GitHub Issue as the task contract when work is planned, reviewed, or split across agents.
- Link each AI task branch and Draft PR to its issue with `Refs #issue-number` until merge readiness is confirmed.
- Use `Fixes #issue-number` only when the PR is intended to close the issue after merge.
- Prefer a separate worktree for issue loops when the current checkout is dirty, multiple agents are active, or module-by-module inspection is running.
- The issue must include scope, acceptance criteria, verification commands, rollback plan, and safety notes before implementation starts.
## Security Rules

- Do not read or print production/development DB credentials, passwords, tokens, personal information, private keys, or production URLs.
- 운영/개발 DB 접속정보, 비밀번호, 토큰, 개인정보, 운영 URL은 읽거나 출력하지 않는다.
- Do not paste secrets into worklogs, handoffs, commit messages, PR bodies, or chat.
- If a task needs secret-backed access, ask the user for an approved safe path instead of inspecting local secret files.

## Dangerous Command Rules

User approval is required before running:

- `rm -rf`
- `chmod -R`
- `curl | bash`
- external script execution
- package installation or dependency download
- destructive git operations
- permission changes

## Architecture Rules

- Preserve existing architecture and layer boundaries.
- Keep hexagonal architecture: inbound adapter, application service/use case, domain, outbound port, infrastructure adapter.
- Do not mix responsibilities across Controller, Service, ServiceImpl, DAO, Query XML, Mapper XML, Repository, DTO, VO, and domain model.
- Batch modules are orchestrators. Business rules, formulas, and state changes belong in core/application/domain.
- Large refactors must be split into a separate branch and reviewed independently.

## Code Quality Rules

- 승인된 변경의 구현·완료·독립 리뷰에는 [코드·문서 품질 계약](42-code-documentation-quality.md)의 Q1–Q4와 완료표를 필수 적용한다.
- 명확한 이름·응집된 작은 함수·단일 책임·중복 최소화·불필요한 추상화 방지를 확인하되 MSA·헥사고날·DDD, 성능과 금융 정합성을 우선한다.
- 입력→처리→출력과 설계 이유·예외·경계·해당 시 재실행을 설명하고, 검증된 코드·명령에 맞는 모듈 기능 문서와 초보자 안내를 유지한다. WORKLOG는 기능 문서를 대체하지 않는다.
- 변경된 비자명 로직의 의도 주석과 유익한 기존 주석을 유지하고 오래된 주석을 수정한다. 매줄 주석이나 동일 장문 중복, 범위 밖 전수 정리는 요구하지 않는다.
- 항목별 PASS/FAIL/N/A에 파일·테스트 근거를 붙인다. 독립 리뷰어가 구체적 N/A 사유를 확인하고 FAIL·근거 누락을 원래 작성자에게 반환한다. 테스트 미실행을 PASS로 처리하지 말고 위험과 다음 검증 게이트를 기록한다.

## Completion Rules

Every meaningful change must document:

- changed files
- impact scope
- test method and result
- rollback method
- remaining risks

The parent Integrator updates shared records once after collecting subagent results:

- `docs/ai-harness/worklog.md`
- `docs/ai-harness/agent-status.md`
- `docs/ai-harness/handoff.md`
- `docs/ai-harness/conflict-log.md` when conflict occurs
- the agent-specific worklog when applicable

## Commit Rules

- Do not commit with conflict markers.
- Check only the explicit, approved, non-sensitive changed-file list for conflict markers. Do not scan `.` or secret-bearing paths.
- PowerShell:

```powershell
$approvedFiles = @(
    'docs/ai-harness/10-rules.md'
    'docs/ai-harness/40-test-checklist.md'
)
$markerPattern = '^(<{7}|={7}|>{7})( |$)'

rg -n -- $markerPattern $approvedFiles
$rgExit = $LASTEXITCODE
if ($rgExit -eq 0) { throw 'Conflict marker found.' }
if ($rgExit -ge 2) { throw "Conflict marker scan failed with rg exit code $rgExit." }
Write-Host 'No conflict markers found.'
```

- Linux bash:

```bash
approved_files=(
  docs/ai-harness/10-rules.md
  docs/ai-harness/40-test-checklist.md
)
marker_pattern='^(<{7}|={7}|>{7})( |$)'

if rg -n -- "$marker_pattern" "${approved_files[@]}"; then
  rg_exit=0
else
  rg_exit=$?
fi

case "$rg_exit" in
  0) echo 'Conflict marker found.' >&2; exit 1 ;;
  1) echo 'No conflict markers found.' ;;
  *) echo "Conflict marker scan failed with rg exit code $rg_exit." >&2; exit "$rg_exit" ;;
esac
```

- The raw `rg` exit code has three distinct meanings: `0` means a marker was found and the gate fails, `1` means no marker was found and the gate passes, and `2` or greater means the scan itself failed and must not be treated as a pass. The wrappers above normalize only the gate result: a clean raw exit `1` continues successfully, while a match or scan error stops the script.
- `^` limits a match to the start of a physical line, `{7}` requires exactly seven marker characters, and `( |$)` requires a following space or the end of the line. Therefore, the command text that explains the pattern does not trigger its own alarm.

초보자 설명: 이 검사는 화재 경보기와 같다. 경보기 사용 설명서에 "화재"라는 단어가 적혀 있다는 이유만으로 경보를 울리면 자기 오탐이다. 줄 맨 앞에 놓인 실제 7글자 충돌 마커만 감지하고, `rg`가 파일을 읽지 못한 검사 오류도 "안전"으로 오해하지 않는다.

- Do not commit unrelated generated logs, local settings, secrets, or temporary worktree folders.

## Clean Root Directory Policy

- Keep the repository root directory clean. Only canonical build manifests (`build.gradle`, `package.json`, `Dockerfile`, `docker-compose.yml`), root environment files (`.env`), and tool-required root contracts (`README.md`, `AGENTS.md`, `GEMINI.md`, `CLAUDE.md`, `GEMINI_REVIEW_PROMPT.md`) remain in the root directory.
- All non-essential documentation, legacy pointers, operational task lists, and worklogs must be stored under `docs/` or `docs/ai-harness/` or `docs/history/`.
