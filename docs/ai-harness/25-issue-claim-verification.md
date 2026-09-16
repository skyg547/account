# Issue Claim Verification

## Purpose

Confirm an Issue's diagnosis against the actual code before implementing it. Automatically generated Issues describe a symptom the generator inferred, not one it verified, and several have turned out to be wrong in ways that would have made the codebase worse if implemented as written.

초보자 설명: 이슈에 적힌 "이게 문제다"가 항상 맞지는 않는다. 고치기 전에 그 파일을 직접 열어보고 정말 그런지 확인하라는 절차다.

## When This Applies

Run this before writing code for any Issue you did not diagnose yourself. It matters most for Issues carrying the `agent-loop` label, which are generated rather than hand-written, but the cost is low enough to apply generally.

Skip it only when you produced the diagnosis in the same session and already read the code.

## Evidence Observed

Four Issues in one review session had diagnoses that did not survive contact with the code.

| Issue | Claim | Reality |
| --- | --- | --- |
| #166 | "returns null on token verification failure — security risk" | None of the three `return null` sites is a verification path. Two signal absent configuration, one an absent optional claim. Implementing the requested change would break startup for HS256-only deployments. |
| #89 | "asset-lease:api ApplicationContext failure" | The attached log is a Gradle CLI usage error (`Cannot locate tasks that match ':'`). The jar starts cleanly. |
| #467 | "five modules are missing a local profile" | Zero were. The repository has two valid patterns and only one was treated as correct. One module's startup failure was a deliberate fail-closed design. |
| #182, #165 | `return null` still present | Zero occurrences remain. Already resolved, Issue left open. |

Three failure modes are visible:

- **Pattern matching without meaning.** Counting `return null` occurrences does not distinguish "configuration absent" from "optional value" from "failure swallowed". Only the first two appeared in #166, and both were load-bearing.
- **Misattributed logs.** A pasted log may not be the symptom the title claims.
- **Stale state.** An Issue can be resolved by unrelated work and stay open.

## Procedure

### 1. Read what the Issue points at

Open the exact file and line. If the Issue names a symptom without a location, find it before continuing.

Ask what the code actually does, not whether the described problem could exist somewhere.

### 2. Reproduce the attached evidence

If the Issue includes a log, error, or failing command, run it.

```powershell
.\gradlew :module:test --console=plain
```

If it does not reproduce, say so in the Issue with what you ran and what you got instead. A non-reproducing report is a finding, not a blocker.

### 3. Check whether it is already fixed

Work merged since the Issue was filed may have resolved it. Verify against latest `origin/main`, not against the Issue's filing date.

### 4. Decide before implementing

| Finding | Action |
| --- | --- |
| Diagnosis holds | Claim and implement normally |
| Diagnosis wrong | **Do not implement.** Record the evidence in the Issue and propose closure |
| Already resolved | Record the verification and propose closure |
| Partially right | Narrow the scope in a claim comment, implement only the verified part |

초보자 설명: 진단이 틀렸을 때 "그래도 뭔가 고쳐야 하나" 싶을 수 있지만, 틀린 지시를 그대로 구현하는 것이 가장 나쁘다. 근거를 남기고 멈추는 것이 올바른 결과다.

### 5. Never implement a change you believe is wrong

If the Issue asks for something the code contradicts, the Issue is the thing to correct. #166 is the concrete case: doing what it asked would have turned a working key-selection branch into a startup failure.

## Finding Already-Resolved Issues

An Issue can be fixed by unrelated work and stay open. #182 and #165 both ask to remove `return null` occurrences that no longer exist.

For the `return null` family, first identify every production Java root from the module layout and the Issue's scope. For example, a standalone module may use `<module>/src/main/java`, while a split module may use `<module>/api/src/main/java`, `<module>/core/src/main/java` and `<module>/batch/src/main/java`. List only actual, intended production roots; do not guess both layouts with wildcards or silently drop a missing root. Tests are outside this search.

A count is meaningful only after the declared scope exists, contains Java files, and the entire search succeeds. Save the following as `Test-IssuePattern.ps1` outside the repository. Run it from the verified checkout with PowerShell 7.2+ and `rg` (ripgrep) already available. This example does not install tools.

```powershell
#requires -Version 7.2
param(
    [string[]] $ProductionRoots = @(),
    [string] $Pattern = 'return null'
)

$ErrorActionPreference = 'Stop'
# rg exit 1 means a successful search with no matches, not a PowerShell failure.
$PSNativeCommandUseErrorActionPreference = $false
try {
    if ($ProductionRoots.Count -eq 0 -or [string]::IsNullOrWhiteSpace($Pattern)) {
        throw 'Declare nonempty production roots and a nonempty pattern.'
    }
    $rg = (Get-Command rg -CommandType Application -ErrorAction Stop).Source
    $resolvedRoots = @(foreach ($root in $ProductionRoots) {
        if ([string]::IsNullOrWhiteSpace($root) -or
            -not (Test-Path -LiteralPath $root -PathType Container -ErrorAction Stop)) {
            throw "Missing production directory: $root"
        }
        $directory = Get-Item -LiteralPath $root -Force -ErrorAction Stop
        # Require the production Java boundary; a module/test directory is not a scope.
        if ($directory.FullName -notmatch '[/\\]src[/\\]main[/\\]java$') {
            throw "Expected an explicit src/main/java root: $root"
        }
        # Use the search tool's own filters, including its symlink/case rules.
        $javaFiles = @(& $rg --no-config --no-ignore --hidden --files `
            --glob '*.java' -- $directory.FullName)
        $scopeExit = $LASTEXITCODE
        if ($scopeExit -notin @(0, 1)) {
            throw "Production enumeration failed (rg exit $scopeExit): $root"
        }
        if ($javaFiles.Count -eq 0) {
            throw "Empty production Java scope: $root"
        }
        $directory.FullName
    })
    # Ignore local rg configuration and ignore files so approved roots are fully searched.
    $matchingLines = @(& $rg --no-config --no-ignore --hidden --text --case-sensitive `
        --line-number --with-filename --fixed-strings --glob '*.java' -- $Pattern @resolvedRoots)
    $searchExit = $LASTEXITCODE
    # Capture the native status immediately; never interpret partial output as success.
    switch ($searchExit) {
        0 {
            "MATCHES: $($matchingLines.Count) matching lines; inspect their meaning."
            $matchingLines
        }
        1 { 'ZERO_CANDIDATE: search succeeded; recheck all acceptance criteria.' }
        default { throw "Search failed (rg exit $searchExit); no resolution judgment." }
    }
    exit 0
} catch {
    [Console]::Error.WriteLine("ERROR: $($_.Exception.Message)")
    exit 2
}
```

Invoke the saved script in a separate PowerShell process so its `exit` does not close the calling shell. Supply the complete array in that process, for example:

```powershell
pwsh -NoProfile -Command '& /tmp/Test-IssuePattern.ps1 -ProductionRoots @("module/core/src/main/java", "module/api/src/main/java")'
$checkExit = $LASTEXITCODE
if ($checkExit -ne 0) { throw "Verification failed (exit $checkExit)." }
```

Replace the script path and roots with the reviewed paths for your platform/module. Record the base commit, declared roots, command, stdout, stderr and exit code. A successful result describes that scope at that time; missing required roots must not be omitted just to obtain zero.

| Result | Script exit | Meaning / next action |
| --- | --- | --- |
| `MATCHES` | 0 | Search succeeded and found matching lines. Read their semantics before deciding whether the diagnosis holds. |
| `ZERO_CANDIDATE` | 0 | Search succeeded over nonempty production Java scope with no matches. Candidate for resolution only: recheck every Issue acceptance criterion and related behavior/test evidence. |
| `ERROR` | 2 | Missing/invalid root, empty Java scope, enumeration failure, missing tool or failed search. Verification is incomplete; never classify it as resolved. |

Only the parent Integrator decides whether the complete evidence supports proposing closure. Neither a successful zero count nor this script authorizes label changes or Issue closure. The same approach applies to other concrete patterns, but the scope and semantic/behavioral checks must come from that Issue's acceptance criteria.

초보자 설명: 책을 못 펼쳐서 아무 글자도 못 읽은 것과, 정해진 책을 끝까지 읽었는데 찾는 글자가 없는 것은 다르다. 검색에 성공해 0건이어도 이슈의 다른 완료 조건까지 확인한 뒤 부모 Integrator가 판단한다.

### Reproduce the search boundaries

Use synthetic files only. With the script above saved as `Test-IssuePattern.ps1`, run this PowerShell fixture from its directory. It leaves a uniquely named temporary directory for inspection; it never touches repository production files.

```powershell
#requires -Version 7.2
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$scriptPath = (Resolve-Path -LiteralPath './Test-IssuePattern.ps1').Path
$fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) ('issue-679-' + [guid]::NewGuid())
foreach ($case in @('match', 'zero', 'empty', 'test-only')) {
    $javaRoot = Join-Path $fixtureRoot "$case/src/main/java"
    New-Item -ItemType Directory -Path $javaRoot -Force | Out-Null
}
Set-Content -LiteralPath (Join-Path $fixtureRoot 'match/src/main/java/Sample.java') `
    -Value 'class Sample { Object value() { return null; } }'
Set-Content -LiteralPath (Join-Path $fixtureRoot 'zero/src/main/java/Sample.java') `
    -Value 'class Sample {}'
$testRoot = Join-Path $fixtureRoot 'test-only/src/test/java'
New-Item -ItemType Directory -Path $testRoot -Force | Out-Null
Set-Content -LiteralPath (Join-Path $testRoot 'SampleTest.java') -Value 'return null;'

foreach ($case in @('match', 'zero', 'missing', 'empty', 'test-only')) {
    $root = Join-Path $fixtureRoot "$case/src/main/java"
    $output = @(& pwsh -NoProfile -File $scriptPath -ProductionRoots $root 2>&1)
    $actualExit = $LASTEXITCODE
    $expectedExit = if ($case -in @('match', 'zero')) { 0 } else { 2 }
    $expectedResult = switch ($case) {
        'match' { 'MATCHES: 1 ' }
        'zero' { 'ZERO_CANDIDATE:' }
        default { 'ERROR:' }
    }
    if ($actualExit -ne $expectedExit -or
        ($output -join "`n") -notmatch "(?m)^$expectedResult") {
        throw "Fixture $case failed: exit=$actualExit; output=$output"
    }
    "$case PASS: exit=$actualExit; result=$expectedResult"
}
"Fixture retained at $fixtureRoot"
```

Expected: matching production file → `MATCHES`; nonmatching production file → `ZERO_CANDIDATE`; nonexistent root, empty directory and test-only module → `ERROR`. An absent `rg` or native search error must also terminate with `ERROR`/exit 2, even if a tool emits partial matches first. These fixtures test the diagnostic mechanism; they do not establish that a real business Issue is resolved.

## Recording a Failed Verification

Write it in the Issue so the next reader does not repeat the work. Include:

- what the Issue claimed, quoted
- what the code or command actually shows, with file:line or output
- why implementing as written would be wrong, when that applies
- what should happen instead — closure, a narrowed scope, or a separate Issue

Do not change labels or close the Issue yourself unless you own it. Propose, and let the parent Integrator decide.

## Circuit Breaker & Early Exit on Invalid Diagnosis

### 1. Early Exit Rule
에이전트가 Issue를 Claim한 직후, 진단 가설이 실제 코드베이스와 불일치하거나 이미 해결된 상태임이 판명되면 **절대로 임의로 다른 코드를 수정하거나 억지로 변경사항을 만들어내지 않습니다.**
- **조치**: 검증 명령 및 코드 확인 결과(file:line)를 댓글로 명시하고, 수정 없이 `status:ready` 해제 및 이슈 종결(Close)을 제안합니다.
- **근거**: 틀린 가설을 바탕으로 한 수정은 정상적인 동작을 파괴하는 회귀(Regression) 버그를 유발합니다.

### 2. Circuit Breaker for Agent Loops
자동화 러너(Codex, Gemini, Claude 등) 실행 루프에서:
- **연속 2회 실패 시 자동 차단**: 동일 Issue에 대해 빌드/테스트/런타임 실패가 2회 연속 발생할 경우, 무한 재시도 루프를 중단하고 해당 이슈를 `status:blocked`로 전환합니다.
- **사용량 한도(Usage Limit) 검출 시 안전 중단**: OpenAI/Anthropic/Google API 쿼터 한도에 도달한 경우, 재시도 없이 프로세스를 즉시 종료하여 불필요한 토큰 소모를 방지합니다.

## Relation To Other Documents

- `20-workflow.md` — the Intake step; this procedure sits between reading the Issue and analysing impact
- `86-multi-tool-issue-ownership.md` — claim protocol. Verify before claiming, so a wrong Issue is not marked `status:in-progress`
- `40-test-checklist.md` — verification after implementing; this document covers verification before
