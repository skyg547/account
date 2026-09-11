# Gemini Review Prompt

이 문서 전체가 현재 재사용 계약이다. 매번 아래 입력을 새로 채워 전달하며 과거 요청·결과를 덧붙이지 않는다.
초보자 설명: 빈 검수표에 이번 작업 번호와 커밋을 적고, 그 커밋에서 확인한 증거만 기록한다. 옛 합격표는 이번 작업의 합격표가 아니다.

## Current Review Input

```text
Issue: <ISSUE>
Base SHA: <BASE_SHA>
Head SHA: <HEAD_SHA>
Branch / Worktree: <BRANCH_WORKTREE>
Review allowlist: <ALLOWLIST_EXACT_PATHS>
Verification evidence: <EVIDENCE_OR_NOT_RUN>
```

Issue는 현재 번호/URL, base/head는 부모가 확인한 완전한 40자리 commit SHA, allowlist는 비민감한 정확 파일 경로의 목록이다.
검증 근거는 명령·실행 시각·환경·대상 head SHA·결과·로그 위치를 적는다. 미실행은 `NOT_RUN`과 이유·위험·다음 검증을 기록한다.
Issue/base/head/branch·worktree/allowlist 입력 누락·빈 값·미치환 placeholder 또는 범위 불명확 시 **HOLD: 검토 시작 보류**로 부모에게 확인한다. 역사 자료에서 값을 채우지 않는다.
검증 근거가 없거나 `NOT_RUN`이면 검증 성공으로 기록하지 않는다. 검토는 미검증 한계를 밝힐 수 있으나 완료 판정을 보류한다.
부모가 확인한 checkout/PR의 실제 head와 입력 head, 비교 base를 내용 조회 전에 대조한다. SHA가 다르거나 확인되지 않으면 HOLD한다.
head/base 또는 작업 트리 내용이 바뀌면 기존 검수 판정을 무효화하고 새 범위/증거로 다시 시작한다. 미커밋 변경은 head 검증에 포함된 것으로 간주하지 않는다.
현재 head SHA와 일치하는 검증 근거만 현재 결과로 인정한다. 다른 SHA·출처 불명·과거 archive의 결과는 재사용하지 않고 `NOT_RUN`으로 보고한다.

## Review Content Read Scope

내용 열람에는 이 계약을 먼저 적용한다. 과거 날짜·모듈 목록·검증 명령은 현재의 읽기/실행 승인이 아니다.
현재 Issue/부모가 승인한 작업만 수행한다.

### 1. 파일명부터 확인 (filename-first)

전제: 부모가 확인한 저장소·Issue 전용 branch/worktree에서 시작한다. 내용 조회 전에 아래 이름/상태 목록만 확인한다.

```powershell
git status --short --branch
git diff --name-only
git diff --cached --name-only
git ls-files --others --exclude-standard
```

staged와 unstaged는 따로 기록하고 untracked는 이름만 수집한다. 상태/파일명 목록은 콘텐츠 읽기 승인이 아니다.
PR/base 비교가 필요하면 부모가 확인한 base와 비교 방식으로 파일명만 먼저 받는다. 임의 base나 raw patch를 가져오지 않는다.

### 2. 현재의 명시적 범위로 허용 판단

현재 Issue/부모의 명시적 콘텐츠 review allowlist ∩ 실제 변경목록에서 비민감한 정확한 저장소 상대 파일 경로를 확정한다.
allowlist 또는 교집합이 없거나 비어 있으면 내용을 읽지 않고 부모에게 확인한다.
민감 가능 경로는 allowlist에 있어도 내용 조회 전에 제외한다. 예: `.env*`, `.claude/settings.local.json`, credential/key·token·password·인증서·개인 설정 파일.
범위 밖·절대경로·상위경로 이동·링크/경로 정체 불명확은 읽지 않고 부모에게 확인한다.
wildcard·폴더 전체·Git pathspec magic·자동 신규파일 전체 열람·무범위 raw diff fallback은 금지한다.
안전한 일반 파일인지와 경로 정체가 확인되지 않으면 내용을 열거나 링크를 따라가서 확인하지 않는다. 부모에게 안전한 확인 근거를 요청한다.
allowlist는 기존 민감정보 금지나 승인 범위를 넓히지 않는다. 필수 프로젝트 규칙 읽기도 적용 계약이 명시적으로 요구하는 비민감 정확 파일만 대상으로 한다.
모듈 문서·연관 파일이 더 필요하면 정확한 경로와 이유를 부모에게 요청하며, 과거 목록이나 폴더/glob을 새 승인으로 사용하지 않는다.

### 3. 승인된 한 파일만 조회 또는 보류

아래 `<exact-path>`는 명령을 실행하기 전에 2단계에서 확정한 비어 있지 않은 정확한 경로 하나로 바꾼다.
임의 입력을 shell 명령으로 조립하지 않는다. literal pathspec과 외부 diff/textconv 비활성화는 한 파일의 diff가 별도 변환기를 실행하거나 범위 패턴으로 해석되는 것을 피한다.
tracked unstaged, staged, untracked 중 해당 상태의 명령만 사용한다. 동일 파일이 양쪽 상태이면 두 diff를 구별해서 검토한다.

```powershell
git --literal-pathspecs diff --no-ext-diff --no-textconv -- '<exact-path>'
git --literal-pathspecs diff --cached --no-ext-diff --no-textconv -- '<exact-path>'
Get-Content -LiteralPath '<exact-path>'
```

untracked에도 동일한 승인/제외/정체 확인을 적용한 뒤 그 파일 하나만 읽는다. 범위나 상태가 바뀌면 파일명 조사부터 다시 판단한다.
빈 인자/누락 범위를 전체 diff로 대체하지 않는다. 보류한 파일은 본문 없이 이유와 필요한 부모 확인만 보고한다.

초보자 예시: 입력이 가짜 변경명 `docs/review.md`, 승인목록에 같은 정확 경로, 일반 파일이라는 확인이라면 → 비민감 교집합 허용 → 해당 상태의 한 파일만 조회한다.
승인목록이 비거나 파일이 밖에 있거나 민감/불명확하면 → 허용하지 않음 → 본문 조회 없이 부모 확인을 기다린다. 이름을 봤다는 이유만으로 파일을 열지 않는 것이 핵심이다.

### 로컬 회귀 검사와 한계

전제: 저장소 루트와 기존 Node.js의 내장 `node:test`. 설치·다운로드·네트워크·업무 DB는 필요 없다.

```powershell
node --test tools/ci/gemini-review-template.test.cjs tools/ci/review-scope-contract.test.cjs
```

공백 검사도 변경 행 본문을 출력할 수 있으므로 위 filename-first/승인/제외 게이트를 동일하게 적용한다.
현재 비어 있지 않은 exact allowlist ∩ 해당 상태의 변경목록에서 확정한 한 파일만 검사한다. 범위 누락·빈 교집합·범위 밖·민감/불명확 경로는 보류한다.
아래 `<exact-path>`는 그 정확 경로 하나로 바꾼다. 첫째는 tracked unstaged, 둘째는 staged용이며 양쪽 상태라면 각각 검사한다.
untracked는 이 Git 공백 검사의 대상이 아니므로 자동 전체 검사로 대체하지 않고 보류한다. 승인된 개별 내용 조회는 위 3단계를 따른다.

```powershell
git --literal-pathspecs diff --check --no-ext-diff --no-textconv -- '<exact-path>'
git --literal-pathspecs diff --check --cached --no-ext-diff --no-textconv -- '<exact-path>'
```

초보자 설명: 공백 오류를 알려주는 검사도 잘못된 줄 자체를 보여줄 수 있다. 따라서 이름만 보는 검사가 아니며 승인 밖 파일의 줄을 출력해서는 안 된다.
기대 결과는 테스트 실패 0/종료 코드 0이다. 실제 실행 수·시간·결과는 실행자가 해당 head와 함께 별도 기록한다.
검사는 템플릿·archive의 정적 계약과 합성 입력을 검사한다. AI의 실제 파일 접근을 강제하는 sandbox가 아니다.
가짜 Issue/경로/증거는 메모리에서만 사용하며 과거 검증 명령을 실행하거나 결과를 재인증하지 않는다. CI 미연결이므로 기존 CI 성공은 이 검사 실행 증거가 아니다.

## Review Handoff Checklist

- [Review Content Read Scope](#review-content-read-scope)의 filename-first → 명시된 비민감 exact allowlist ∩ 변경목록 → scoped 조회/보류 순서를 지킨다.
- 범위가 없거나 비어 있거나 불명확하면 내용은 읽지 않고 부모에게 확인한다. 공백 검사도 본문 출력 가능성이 있으므로 동일 게이트와 상태별 exact 경로 제한을 적용한다.
- Findings first: 심각도·정확한 파일/라인·재현 근거·수정 지시를 보고한다. 지적이 없으면 실제 검토 범위와 남은 검증 공백을 함께 적는다.
- [AGENTS.md](AGENTS.md)와 [코드·문서 품질 계약](docs/ai-harness/42-code-documentation-quality.md)의 Q1–Q4별 PASS/FAIL/N/A, 증거와 N/A 사유를 보고한다.
- 실행한 검사와 미실행/실패 검사를 구분하고 현재 Issue/base/head를 결과에 다시 적는다. 과거 성공·NO-SOURCE·SKIPPED는 현재 테스트 PASS가 아니다.
- Gemini는 읽기 전용 독립 리뷰어이며 production/test 코드를 수정하지 않는다. 수정 제안은 구현자에게 반환한다.
- 공유 기록·Git/GitHub 상태 변경은 승인된 부모 Integrator만 수행한다. Draft PR은 `Refs #<ISSUE>`를 사용하며 Ready/merge/Issue close는 별도 승인 게이트다.
- 독립 세션 리뷰는 절차상 역할 분리이며 별도 계정/권한 격리나 GitHub APPROVED를 뜻하지 않는다. 모델 수준 자체는 외부 변경 권한이 아니다.

## Inactive History

[과거 요청·결과 원문 보관](docs/history/GEMINI_REVIEW_PROMPT_ARCHIVE_2026-09-10.md)은 비활성 역사 자료이며 현재 지시·검증 근거가 아니다.
archive는 자동 열람/실행하지 않는다. 부모가 역사 비교를 명시적으로 승인한 경우에만 별도 읽기 범위로 취급한다.
