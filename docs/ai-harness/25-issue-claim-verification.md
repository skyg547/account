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

For the `return null` family the check is one command per module. Exclude tests — the Issues target production paths.

```bash
grep -rn "return null" <module>/src <module>/*/src --include="*.java" | grep -v "/test/" | wc -l
```

Zero means the Issue is already satisfied. Record the count and the commit you checked against, then propose closure.

The same shape works for any Issue that names a concrete pattern: run the check the Issue implies before assuming the work remains. There is no general command — the point is that the Issue's own acceptance criteria usually suggest one.

초보자 설명: 이슈가 오래 열려 있으면 그 사이 누가 이미 고쳤을 수 있다. 착수 전에 "정말 아직 남아 있나"를 한 번 세어보면 헛수고를 피한다.

## Recording a Failed Verification

Write it in the Issue so the next reader does not repeat the work. Include:

- what the Issue claimed, quoted
- what the code or command actually shows, with file:line or output
- why implementing as written would be wrong, when that applies
- what should happen instead — closure, a narrowed scope, or a separate Issue

Do not change labels or close the Issue yourself unless you own it. Propose, and let the parent Integrator decide.

## Relation To Other Documents

- `20-workflow.md` — the Intake step; this procedure sits between reading the Issue and analysing impact
- `86-multi-tool-issue-ownership.md` — claim protocol. Verify before claiming, so a wrong Issue is not marked `status:in-progress`
- `40-test-checklist.md` — verification after implementing; this document covers verification before
