# Test Checklist

## Before Testing

- Confirm current branch is not `main`, `master`, or `develop`.
- Confirm no conflict markers in the explicit, approved, non-sensitive changed-file list. Do not scan `.` or secret-bearing paths.
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

- Read the raw `rg` result before applying the wrapper: exit `0` means a marker was found and the check failed, exit `1` means no marker was found and the check passed, and exit `2` or greater means a scan error. A missing file must therefore fail as an error, never pass as "no marker".
- The anchored pattern checks only physical lines that begin with exactly seven marker characters and then a space or line end. Pattern examples embedded later in a command line do not match themselves.

초보자 설명: 화재 경보기 사용 설명서에 "화재"라는 단어가 있어도 실제 화재는 아니다. 같은 방식으로 명령 설명 속 글자 조합은 무시하고, 줄 맨 앞의 실제 충돌 마커만 경보 대상으로 삼는다. 경보기 자체가 고장 난 상태인 exit `2` 이상도 안전 판정으로 바꾸지 않는다.

### Conflict Marker Reproduction

Use the same pattern for all three cases. The expected values below are raw `rg` exit codes, before the pass/fail wrapper changes the script exit code.

PowerShell:

```powershell
$markerPattern = '^(<{7}|={7}|>{7})( |$)'

"rg -n -- '^(<{7}|={7}|>{7})( |$)' approved-file.md" | rg -n -- $markerPattern
$exampleExit = $LASTEXITCODE # expected: 1 (the command example does not match itself)

@('<<<<<<< HEAD', '=======', '>>>>>>> branch') | rg -n -- $markerPattern
$markerExit = $LASTEXITCODE # expected: 0 (real markers are found)

rg -n -- $markerPattern 'path/that-does-not-exist'
$errorExit = $LASTEXITCODE # expected: 2 or greater (scan error)
```

Linux bash:

```bash
marker_pattern='^(<{7}|={7}|>{7})( |$)'

printf '%s\n' "rg -n -- '^(<{7}|={7}|>{7})( |$)' approved-file.md" |
  rg -n -- "$marker_pattern"
example_exit=$? # expected: 1 (the command example does not match itself)

printf '%s\n' '<<<<<<< HEAD' '=======' '>>>>>>> branch' |
  rg -n -- "$marker_pattern"
marker_exit=$? # expected: 0 (real markers are found)

rg -n -- "$marker_pattern" path/that-does-not-exist
error_exit=$? # expected: 2 or greater (scan error)
```

- Confirm changed modules and docs are known:

```powershell
git status --short
git diff --stat
```

## Java And Gradle

- Narrow compile for changed modules:

```powershell
.\gradlew :module:compileJava --console=plain --max-workers=1
```

- Narrow tests:

```powershell
.\gradlew :module:test --console=plain --max-workers=1
```

- Full build when contracts, build scripts, shared modules, API contracts, or batch behavior changed:

```powershell
.\gradlew build --console=plain --max-workers=1
```

## Continuous Integration

- CI runs `:module:test` only for the modules touched by the diff. See `45-ci-module-validation.md`.
- Changing `shared-kernel`, `contracts`, or the build scripts makes CI run every module.
- CI does not run `bootJar`, container policy checks, or migration verification. Those remain local gates in this checklist.

초보자 설명: CI가 통과했다고 모든 검증이 끝난 것은 아니다. CI는 바뀐 모듈의 테스트만 돌리므로, 아래 배치/API 항목은 여전히 직접 확인해야 한다.

## Batch

- Confirm Spring Batch Job names and parameters.
- Run context smoke with job disabled when checking wiring only.
- Run actual Job smoke with explicit `spring.batch.job.name` when verifying execution.
- Record restartability, idempotency, and data prerequisites.

## API

- Confirm controller mappings.
- Confirm DTO validation and error response behavior.
- Run bootRun smoke only when needed and with local infrastructure disabled.

## Documentation

- Update module README or docs when behavior, commands, parameters, or architecture changed.
- The parent Integrator updates harness logs (not a substitute for module documentation):
  - `worklog.md`
  - `agent-status.md`
  - `handoff.md`
  - `conflict-log.md` if applicable

## Quality Completion Gate

- 구현 완료와 독립 리뷰에서 [코드·문서 품질 계약](42-code-documentation-quality.md)의 Q1–Q4 완료표 확인은 필수다. 변경 파일·테스트와 설명을 대조해 코드 품질, 흐름, 모듈/초보자 문서, 비자명 로직의 의도 주석을 검토한다.
- 각 항목은 PASS/FAIL/N/A 및 파일·테스트 근거로 보고한다. 문서 전용 변경의 코드 주석은 구체적 사유가 있을 때 N/A 가능하며 독립 리뷰어가 이를 확인한다.
- 테스트 미실행은 PASS가 아니다. 미실행 사유·위험·다음 검증 게이트를 남기고, FAIL 또는 근거 누락이 있으면 완료 처리하지 않는다. 읽기 전용 리뷰어는 원래 작성자에게 수정 요청을 반환한다.
- 이 품질 확인은 기존 검증·Ready·merge 승인 게이트를 대체하지 않는다. 아래 로컬 구조 검사는 CI 미연결 상태이며 연결은 #672에서 별도 추적한다.

```powershell
node --test tools/ci/harness-quality-contract.test.cjs
```

## Rollback Note

Each change summary must state which files to revert or which commit to revert. 초보자 설명: rollback은 "문제가 생겼을 때 되돌리는 방법"이다.
