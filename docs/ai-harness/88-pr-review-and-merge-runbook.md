# PR Review And Merge Runbook

구현 담당이 올린 Draft PR을 검토해 **반려 / 보류 / 승인·병합** 중 하나로 판정하는 절차다.
[`87-spec-driven-delegation.md`](./87-spec-driven-delegation.md)의 Reviewer/Integrator 역할을
실제 실행 단계로 풀어 쓴 것이며, 자동(무인) 실행에도 이 문서를 그대로 적용한다.

초보자 설명: 시공이 끝난 집을 준공 검사하는 절차다. 도면(Issue)대로 지었는지, 시험은 실제로
돌려봤는지 확인하고, 통과면 입주(병합), 미비면 반려, 판단이 어려우면 사람을 부른다.

## 1. 검토 대상과 제외 대상

검토한다:

- open PR이고 base가 `main`이다.
- Draft가 아니다(구현 담당이 `ready for review`로 올렸다).
- `Refs #<issue>` 또는 `Fixes #<issue>`로 Issue가 연결되어 있다.

건드리지 않는다:

- Draft 상태 PR — 아직 작업 중이다.
- 리뷰어 자신이 구현한 PR — 자기 작업을 자기가 승인하지 않는다(87 1절).
- `review:hold` 라벨이 붙은 PR — 사람 판단 대기 중이다.
- Issue 연결이 없고 하네스 흐름 밖에서 열린 오래된 PR — `review:hold`로 돌리고 사람에게 넘긴다.
- conflict가 있는 PR — 병합 대상이 아니다. 반려하고 rebase를 요청한다(60번 문서).

## 2. 검증 게이트

**diff를 읽는 것만으로 승인하지 않는다.** 아래를 실제로 확인한다.

| 게이트 | 확인 방법 | 실패 시 |
| --- | --- | --- |
| Issue 연결 | PR 본문의 `Refs #`/`Fixes #` | 반려 |
| allowlist 준수 | `gh pr diff --name-only`를 Issue의 allowlist와 대조 | **반려** |
| 검증 근거 | PR 본문에 실행 명령과 출력이 있는가 | 반려 |
| CI | `gh pr checks` 통과 | 반려 |
| 테스트 실제 실행 | 변경 모듈의 test task를 리뷰어가 직접 재실행 | 반려 |
| conflict marker | `git diff --check`, `<<<<<<<` 검색 | 반려 |
| 비밀정보 | 자격증명·토큰·운영 URL·개인정보 유입 여부 | **반려** |
| 수용 기준 | Issue의 acceptance criteria 각 항목 충족 | 반려 |

allowlist 위반과 비밀정보 유입은 **다른 항목이 모두 통과해도 병합하지 않는다.**

## 3. 판정

### 3-1. 반려 (Request Changes)

게이트 중 하나라도 실패하면 반려한다. 리뷰어는 **코드를 직접 고치지 않는다**(87 1절).

- 실패한 게이트와 근거(파일:라인, 실패한 명령과 출력)를 적는다.
- 구현 담당이 무엇을 해야 하는지 명확히 적는다.
- Issue를 `status:needs-review` → `status:in-progress`로 되돌린다(86번 문서 전이표).
- PR은 닫지 않는다. 같은 브랜치에서 수정하게 한다.

```bash
gh pr review <번호> --repo skyg547/account --request-changes --body-file <경로>
gh issue edit <이슈> --repo skyg547/account --remove-label "status:needs-review" --add-label "status:in-progress"
```

### 3-2. 심층 검토가 필요한 영역 (병합 가능)

아래 영역은 **병합 금지 영역이 아니라 검증 의무가 추가되는 영역**이다. 리뷰어가 High
Reasoning 티어(Codex, Claude Code)라면 아래 확인을 마친 뒤 **병합할 수 있다.**

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

리뷰어가 Balanced 이하 티어라면 이 영역은 병합하지 않고 3-3 보류로 보낸다.

### 3-3. 보류 (Hold — 사람 판단 필요)

게이트는 통과했지만 **리뷰어가 판단할 수 있는 성질의 문제가 아닌** 경우로 한정한다.
위험도가 높다는 이유만으로는 보류하지 않는다(3-2 참고).

- **검증을 재현할 수 없었다** — 테스트 실행 불가, Docker/DB 부재, 결과 확인 불가.
  근거가 없는 승인은 승인이 아니므로 병합하지 않는다.
- **요구사항 자체가 불명확하다** — Spec과 구현의 의도가 어긋나는데 어느 쪽이 옳은지
  판단하려면 작성자나 업무 담당자의 결정이 필요하다.
- **되돌릴 수 없는 조작이 포함된다** — 실제 데이터 이관·삭제, 외부 시스템에 대한 비가역 호출.
  가역성은 위험 성향의 문제가 아니라 범주의 문제다.
- **저장소 정책 결정이 필요하다** — 의존성 정책, 아키텍처 방향, 플랜/비용이 걸린 변경.

```bash
gh pr comment <번호> --repo skyg547/account --body-file <경로>
gh pr edit <번호> --repo skyg547/account --add-label "review:hold"
```

코멘트에 무엇이 통과했고 **사람이 무엇을 결정해야 하는지**를 적는다.

### 3-4. 승인·병합 (Approve And Merge)

2절 게이트를 모두 통과하고, 3-2에 해당하면 그 추가 확인까지 마쳤으며, 3-3에 해당하지 않을 때
병합한다. High Reasoning 리뷰어에게는 **위험 축 자체가 병합을 막지 않는다.**

```bash
gh pr review <번호> --repo skyg547/account --approve --body-file <경로>
gh pr merge <번호> --repo skyg547/account --squash --delete-branch
```

병합 방식은 저장소 관례에 따라 **squash**를 사용한다. `--admin`이나 force 관련 옵션은 쓰지 않는다.

병합 후:

1. Issue를 닫는다. `Fixes #`로 자동 종료되지 않았으면 직접 닫는다.
2. 진행 중을 뜻하는 `status:*`와 `agent:*` 라벨을 제거한다(86 문서).
3. 브랜치는 `--delete-branch`로 정리한다. 워크트리가 있으면 함께 정리한다.

## 4. 흐름

```
[Implementer]  Draft PR → ready for review
      │
      ▼
[Reviewer]     2절 게이트 검증 (테스트 실제 재실행)
      │
      ├─ 게이트 실패 ──────→ 반려 (request changes) → Issue를 in-progress로
      │
      ├─ 3-2 영역 해당 ────→ 추가 확인 수행
      │                        ├─ 확인 완료 ──→ 병합으로
      │                        └─ 확인 불가 ──→ 보류
      │
      ├─ 확인 불가 / 요구사항 불명확 / 비가역 / 정책 결정 ──→ 보류 (review:hold)
      │
      └─ 통과 ─────────────→ 승인 → squash merge → Issue 종료 → 브랜치 정리
```

보류는 **"위험해서"가 아니라 "확인하지 못해서" 또는 "사람이 결정해야 해서"** 발생한다.

## 5. 자동(무인) 실행 시 추가 제약

무인 리뷰는 사람이 지켜보지 않으므로 아래를 반드시 지킨다.

- **한 번 실행에 PR 1건만** 처리한다. 여러 건을 몰아 처리하지 않는다.
- **리뷰어 티어가 병합 권한을 결정한다.** High Reasoning(Codex, Claude Code)은 3-2의 추가
  확인을 마친 뒤 위험 축을 포함해 병합할 수 있다. Balanced 이하는 3-2 영역을 병합하지 않는다.
- **근거가 없으면 병합하지 않는다.** 검증을 재현하지 못했다면 위험도와 무관하게 보류다.
  "위험해서 보류"가 아니라 "확인하지 못해서 보류"만 남긴다.
- 코드를 수정하지 않는다. 반려만 한다.
- `main`에 직접 push하지 않는다. force push하지 않는다.
- 자기가 만든 PR을 승인·병합하지 않는다.
- conflict를 자동 해결하지 않는다(60번 문서).
- 판정 근거를 PR 코멘트에 남긴다. 근거 없는 승인은 승인이 아니다.

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
