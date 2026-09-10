# PR Review And Merge Runbook

승인된 부모 Integrator가 올린 PR을 검토해 **반려 / 보류 / 통과** 증거를 반환하는 절차다.
통과 판정 자체는 병합 승인이 아니다. [`87-spec-driven-delegation.md`](./87-spec-driven-delegation.md)의 분리된 역할을
실제 실행 단계로 풀어 쓴 것이며, 자동(무인) 실행에도 이 문서를 그대로 적용한다.

초보자 설명: 시공이 끝난 집을 준공 검사하는 절차다. 도면(Issue)대로 지었는지, 시험은 실제로
돌려봤는지 확인한다. 검사 통과와 입주 허가는 다르다. 검토자는 결과를 전달하고, 승인된 부모만 다음 문을 연다.

`AGENTS.md`와 30/80/85/86에 따라 부모 Integrator만 승인 범위 내 Git/GitHub 변경과 공유 기록을 맡는다.
Implementer는 구현·검증·PR 본문 초안만 반환한다. Reviewer와 advisory Integrator는 읽기 전용이며,
PR 코멘트·리뷰·라벨·Issue 상태도 직접 변경하지 않는다. 아래 mutation 예제는 모두 승인된 부모 전용이다.

사람 리뷰는 기본 게이트다. 기존 명시 승인 workflow 예외는 등록된 승인 범위에서만 적용하며,
high 모델 티어는 검토 역량이지 병합 권한이 아니다. Draft PR 요청은 Ready/merge/close/cleanup 승인이 아니다.

## 1. 검토 대상과 제외 대상

검토한다:

- open PR이고 base가 `main`이다.
- 부모가 명시 요청한 동결 Draft는 사전리뷰 대상이다. 구현 동결, PR 번호, head/base SHA, 검증 증거를 먼저 고정한다.
- 최종 병합 검토는 부모 Ready 전환 후 비작성자 독립 세션에서 수행한다. 사전리뷰 통과만으로 최종 검토를 대신하지 않는다.
- `Refs #<issue>` 또는 `Fixes #<issue>`로 Issue가 연결되어 있다.

건드리지 않는다:

- 작업 중 Draft의 전역 자동 인수·자동 병합은 금지한다. 명시 요청된 동결 Draft 사전리뷰와 구분한다.
- 리뷰어 자신이 구현한 PR — 자기 작업을 자기가 승인하지 않는다(87 1절).
- `review:hold` 라벨이 붙은 PR — 사람 판단 대기 중이다.
- Issue 연결이 없고 하네스 흐름 밖에서 열린 오래된 PR — 부모에게 보류 근거를 반환한다.
- 충돌이 있는 PR — 병합 대상이 아니며 보류한다. 부모의 별도 판단 없이 자동 rebase·force push·reset·충돌 해결을 하지 않는다(60번 문서).

## 2. 검증 게이트

**diff를 읽는 것만으로 승인하지 않는다.** 아래를 실제로 확인한다.

검사 실행 시점을 구분한다. Draft 사전리뷰는 동결 diff와 그 단계에서 실행 가능한 검사를 확인한다.
Ready-only 검사는 미도래/pending으로 기록하며 PASS가 아니지만 Draft 사전리뷰를 막지 않는다.
단, 실제 Draft 단계 필수 검사의 오류·실패는 보류 또는 확인된 코드 결함 반려이며 이 예외로 우회하지 않는다.
부모 Ready 전환 후 최종 독립 검토에서는 Ready-only 포함 모든 필수 CI 통과를 확인한다.

| 게이트 | 확인 방법 | 실패 시 |
| --- | --- | --- |
| Issue 연결 | PR 본문의 `Refs #`/`Fixes #`, 현재 owner와 동결 상태 | 확인 불가 시 보류 |
| allowlist 준수 | `gh pr diff --name-only`를 Issue의 allowlist와 대조 | **반려** |
| 검증 근거 | 실행 명령·결과·미실행 사유가 현재 변경에 대응하는가 | 근거 부재/불명확 시 보류 |
| Draft CI | 동결 head/base 기준 실행 가능한 Draft 단계 필수 검사 통과; Ready-only는 미도래/pending 기록 | 이 단계 필수 CI 대기·미실행·확인 불가·환경 실패는 보류, 확인된 코드 결함은 반려 |
| 최종 CI (Ready 후) | 최신 head/base 기준 Ready-only 포함 모든 필수 CI 통과 | CI 대기·미실행·확인 불가·환경 실패는 보류, 확인된 코드 결함은 반려 |
| 테스트 실제 실행 | 변경 범위의 test를 안전한 환경에서 리뷰어가 재실행 | 환경/의존성 부재는 보류, 재현된 코드 결함은 반려 |
| SHA 및 상태 | 리뷰 시작·종료와 병합 직전 head/base SHA, CI 결과, Draft/mergeability 확인 | head/base 또는 검증 결과 변경 시 재검토, 충돌은 보류 |
| conflict marker | `git diff --check`, `<<<<<<<` 검색 | 반려 |
| 비밀정보 | 자격증명·토큰·운영 URL·개인정보 유입 여부 | **반려** |
| 수용 기준 | Issue의 acceptance criteria 각 항목 충족 | 반려 |

allowlist 위반과 비밀정보 유입은 **다른 항목이 모두 통과해도 병합하지 않는다.**

최신 head/base·CI는 리뷰 증거에 함께 기록한다. read-only `gh pr view --json headRefOid,baseRefOid,isDraft,mergeable,statusCheckRollup`
및 `gh pr checks`로 다시 확인한다. 현재 단계의 필수 CI가 생략되었거나 성공 여부를 확인할 수 없으면 통과가 아니다.
Ready-only 미도래는 Draft 사전리뷰에서 명시한 예외이며, 최종 검토에서는 pending인 채로 통과시킬 수 없다.
민감 값은 출력·복사하지 않고 위치와 위험만 보고한다. 설치·DB·외부 서버 접근은 검증을 이유로 자동 승인되지 않는다.

## 3. 판정

### 3-1. 반려 (Request Changes)

검증으로 확인된 코드 결함, allowlist 위반, conflict marker 등 수정 가능한 결함은 반려한다.
현재 단계 필수 검사의 환경·CI 미실행·충돌·승인 부재는 코드 결함으로 단정하지 않고 보류한다. 리뷰어는 **코드를 직접 고치지 않는다**(87 1절).

- 실패한 게이트와 근거(파일:라인, 실패한 명령과 출력)를 적는다.
- 구현 담당이 무엇을 해야 하는지 명확히 적는다.
- 부모가 근거를 확인해 Issue를 `status:needs-review` → `status:in-progress`로 되돌린다(86번 문서 전이표).
- PR은 닫지 않는다. 같은 브랜치에서 수정하게 한다.

부모 전용 예제 (게시 승인 범위 확인 후):

```bash
gh pr review <번호> --repo skyg547/account --request-changes --body-file <경로>
gh issue edit <이슈> --repo skyg547/account --remove-label "status:needs-review" --add-label "status:in-progress"
```

### 3-2. 심층 검토가 필요한 영역

아래 영역은 **검증 의무가 추가되는 영역**이다. High Reasoning 검토자는 아래 확인 결과를
반환하지만, 모델 선택이나 검토 통과로 외부 변경 승인을 얻지는 않는다.

| 영역 | 추가로 확인할 것 |
| --- | --- |
| 회계 정합성 | 차변/대변 합계 일치, `BigDecimal` 사용, scale·rounding 모드, 경계값(0·음수·null) |
| 멱등성·재실행 | 같은 입력 재실행 시 중복 생성이 없는지, unique 제약이나 source reference가 있는지 |
| 동시성·락 | 조회 후 갱신 race, 낙관적/비관적 락 적용 범위 |
| 배치 재시작 | Job parameter identity, checkpoint, 실패 후 재시작 경로 |
| DB migration | forward-only 여부, JPA 컬럼 정의와 migration 일치, precision/scale/nullability |
| 보안 경계 | JWT 검증 경로, 권한 정책, actor를 body/header에서 신뢰하지 않는지, 우회 경로 |
| 공개 계약 | `contracts`·`shared-kernel`·Gateway route 변경 시 직접 소비자 compile/test |
| 다중 모듈 | 영향받는 모듈의 test를 함께 실행 |

**이 확인을 실제로 수행하지 못했다면 병합하지 않고 3-3 보류로 보낸다.** 영역에 해당한다는
사실만으로 보류하지는 않지만, 확인 없이 통과시키지도 않는다.

리뷰 역량이 부족하면 부모에게 상위 역량의 독립 재검토를 요청하고 그 전까지 보류한다.

### 3-3. 보류 (Hold — 사람 판단 필요)

게이트를 확인하지 못했거나 승인·요구사항 판단이 필요한 경우다. 다른 게이트가 통과해야만
보류할 수 있는 것은 아니다. 위험도가 높다는 이유만으로는 보류하지 않는다(3-2 참고).

- **검증을 재현할 수 없었다** — 테스트 실행 불가, Docker/DB 부재, 결과 확인 불가.
  근거가 없는 승인은 승인이 아니므로 병합하지 않는다.
- **현재 상태를 확정할 수 없다** — 현재 단계 필수 CI 대기·미실행·환경 실패, head/base 변경, 충돌, 필수 검증 증거 부재. Draft에서 Ready-only 미도래만 있는 경우는 2절의 예외다.
- **승인 범위가 없다** — Ready/게시/merge/close/cleanup 중 요청받지 않은 변경은 수행하지 않는다.
- **요구사항 자체가 불명확하다** — Spec과 구현의 의도가 어긋나는데 어느 쪽이 옳은지
  판단하려면 작성자나 업무 담당자의 결정이 필요하다.
- **되돌릴 수 없는 조작이 포함된다** — 실제 데이터 이관·삭제, 외부 시스템에 대한 비가역 호출.
  가역성은 위험 성향의 문제가 아니라 범주의 문제다.
- **저장소 정책 결정이 필요하다** — 의존성 정책, 아키텍처 방향, 플랜/비용이 걸린 변경.

부모 전용 예제 (보류 게시 승인 범위 확인 후):

```bash
gh pr comment <번호> --repo skyg547/account --body-file <경로>
gh pr edit <번호> --repo skyg547/account --add-label "review:hold"
```

Reviewer는 무엇이 통과했고 무엇을 확인/결정해야 하는지 부모에게 반환하며, 부모가 승인 범위 내에서 게시한다.

### 3-4. 최종 검토와 별도 승인 병합

2절과 해당 심층 검토를 모두 통과한 비작성자 독립 세션의 증거를 부모가 확인한다.
부모는 승인된 범위에서 리뷰 결과를 게시한다. 동일 GitHub 계정의 자기 approve가 불가능하면
이를 우회하거나 독립 승인을 위조하지 않고 한계를 기록한다. 필요한 게이트가 남으면 보류한다.

별도 merge 승인이 있을 때만 부모가 최신 head/base·CI·Ready 상태를 재확인하고 병합한다.
head/base 또는 검증 결과가 변경되면 재검토한다. `--match-head-commit`은 head 변경만 방어하므로
base도 직전에 다시 확인해야 하며 확인할 수 없으면 보류한다. 승인된 부모 전용 예제:

```bash
gh pr review <번호> --repo skyg547/account --approve --body-file <경로>
gh pr merge <번호> --repo skyg547/account --merge --match-head-commit <검토된-head-SHA>
```

병합 방식은 [85번 문서](./85-github-issue-agent-loop.md)의 **merge commit**을 따른다.
`--delete-branch`를 병합에 결합하지 않는다. `--admin`·force·자동 rebase는 금지한다.

병합 후:

1. 부모가 실제 merge 결과와 종료 조건을 확인한다. 별도 Issue close 승인 없이 닫지 않는다.
   `Fixes #`도 종료 승인이 있을 때만 사용한다. Draft/미완료는 `Refs #`를 유지한다.
2. 승인된 종료 후 부모가 진행 중을 뜻하는 `status:*`와 `agent:*` 라벨을 정리한다(86 문서).
3. cleanup은 별도 승인 게이트다. 부모가 정확한 branch/worktree 소유·현재 상태·미커밋 변경·활성 사용자/후속 사용 여부를
   확인하고 기존 사용자 작업을 보존한 뒤 승인된 대상만 정리한다. merge 성공 자체는 삭제 승인이 아니다.

## 4. 흐름

| 단계 | 담당 | 다음 단계 조건 |
| --- | --- | --- |
| 구현·검증 | Implementer | allowlist diff, 검증 결과, PR 본문 초안을 부모에게 반환 |
| Draft 생성·동결 | 부모 Integrator | 승인된 commit/push/PR 생성, `Refs #`, head/base 기록 |
| 동결 Draft 사전리뷰 | 독립 Reviewer | 부모 명시 요청, 동결 diff·Draft 단계 필수 검사 통과 증거 반환; Ready-only 미도래/pending 기록; 사람 리뷰 기본 |
| 부모 Ready | 부모 Integrator | Draft 단계 필수 검사 통과·사전리뷰 완료 및 Ready 승인 확인 |
| 최종 독립 검토 | 비작성자 Reviewer | 최신 head/base·Ready-only 포함 모든 필수 CI 통과·수용 기준 검증, 변경 시 재검토 |
| 별도 승인 merge commit | 부모 Integrator | 최종 증거와 merge 승인, 병합 직전 상태 재확인 |
| Issue close / cleanup | 부모 Integrator | 병합 결과·종료 조건·각각의 승인 및 보존/소유 확인 |

검토 완료를 기다리는 Draft도 사전리뷰할 수 있으므로 Ready와 순환 대기하지 않는다.
확인된 결함은 구현자에게 반려하고, 수정 후 동결·검증·리뷰 단계를 다시 거친다.

보류는 **"위험해서"가 아니라 "확인하지 못해서" 또는 "사람이 결정해야 해서"** 발생한다.

## 5. 자동(무인) 실행 시 추가 제약

무인 리뷰는 사람이 지켜보지 않으므로 아래를 반드시 지킨다.

- **한 번 실행에 PR 1건만** 처리한다. 여러 건을 몰아 처리하지 않는다.
- 전용 독립 리뷰 자동화의 부모도 등록된 승인 범위 안에서만 게시·Ready·merge·close·cleanup을 수행한다.
  자동화에 명시된 대상/조건/허용 조작을 먼저 확인한다. generic Reviewer는 읽기 전용이며 이 예외를 상속하지 않는다.
- 모델 티어는 검토 역량이지 병합 권한이 아니다. 등록된 merge 승인 없는 자동화는 증거 반환에서 멈춘다.
- **근거가 없으면 병합하지 않는다.** 검증을 재현하지 못했다면 위험도와 무관하게 보류다.
  "위험해서 보류"가 아니라 "확인하지 못해서 보류"만 남긴다.
- 코드를 수정하지 않는다. 통과·반려·보류 증거를 부모에게 반환한다.
- `main`에 직접 push하지 않는다. force push하지 않는다.
- 자기 구현·설계를 독립 최종 검토한 것으로 취급하지 않는다. PR 생성자인 부모도 별도 Reviewer 증거 없이 병합하지 않는다.
- conflict를 자동 해결하지 않는다. 자동 rebase·force·reset·권한 확대도 금지한다(60번 문서).
- 부모가 승인 범위 내에서 판정 근거를 PR에 게시한다. 근거 없는 승인은 승인이 아니다.

> **강제력 한계**: 이 저장소는 Free 플랜 private이라 branch protection이 없다(87 6절).
> 위 제약은 규약이며 GitHub이 막아주지 않는다. 무인 병합을 허용한다는 것은 **규약을 신뢰한다는
> 결정**이므로, 처음 몇 건은 사람이 결과를 확인하는 것을 권장한다.

## 6. 라벨

| 라벨 | 의미 |
| --- | --- |
| `review:hold` | 검증 재현 불가·요구사항 불명확·비가역 조작·정책 결정 등 리뷰어가 판단할 성질이 아니라 병합하지 않음 |
| `review:changes-requested` | 반려됨, 구현 담당이 수정 중 |

Issue 쪽 상태(`status:needs-review` 등)는 86번 문서 전이표를 따른다.

## 7. 관련 문서

- [`87-spec-driven-delegation.md`](./87-spec-driven-delegation.md) — 티어, 분할, 병합 권한
- [`86-multi-tool-issue-ownership.md`](./86-multi-tool-issue-ownership.md) — Issue 상태 전이
- [`60-rebase-merge-policy.md`](./60-rebase-merge-policy.md) — rebase/conflict 안전 규칙
- [`40-test-checklist.md`](./40-test-checklist.md) — 검증 체크리스트
- [`.github/workflows/agent-merge-guard.yml`](../../.github/workflows/agent-merge-guard.yml) — PR 규율 CI 검사
