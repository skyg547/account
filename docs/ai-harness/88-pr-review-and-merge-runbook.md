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

### 3-2. 보류 (Hold — 사람 판단 필요)

게이트는 통과했지만 리뷰어가 단독으로 병합을 결정하면 안 되는 경우다. **아래에 해당하면
자동 병합하지 않고 `review:hold` 라벨을 붙여 사람에게 넘긴다.**

- 금액 계산, 차변/대변 방향, 반올림·scale 등 **회계 정합성**에 영향
- 동시성, 락, **멱등성**, 배치 재시작 동작 변경
- **DB migration**, 스키마 변경, forward-only 제약
- **보안 경계** — JWT, 권한, actor 신뢰, 인증 우회 가능성
- **공개 계약 변경** — `contracts`, `shared-kernel`, Gateway route
- 여러 모듈에 걸친 blast radius
- 되돌리기 어려운 변경(데이터 이관, 삭제)
- 검증을 리뷰어가 재현할 수 없었던 경우(Docker/DB 부재 등)

이 목록은 87 2절의 Fast 티어 금지 조건과 같은 축이다. **구현을 Fast에 맡기지 않는 영역이라면
병합도 무인으로 하지 않는다.**

```bash
gh pr comment <번호> --repo skyg547/account --body-file <경로>
gh pr edit <번호> --repo skyg547/account --add-label "review:hold"
```

### 3-3. 승인·병합 (Approve And Merge)

게이트를 모두 통과하고 3-2에 해당하지 않을 때만 병합한다. 실질적으로 다음 성격의 변경이다.

- 문서, 주석, 설정 placeholder
- Spec의 작업 지시와 **1:1로 일치하는** 기계적 수정
- 회귀 테스트 추가
- 명확한 버그 수정이면서 위 위험 축에 걸리지 않는 것

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
      ├─ 실패 ─────────→ 반려 (request changes) → Issue를 in-progress로
      │
      ├─ 위험 축 해당 ──→ 보류 (review:hold) → 사람 판단
      │
      └─ 통과 ─────────→ 승인 → squash merge → Issue 종료 → 브랜치 정리
```

## 5. 자동(무인) 실행 시 추가 제약

무인 리뷰는 사람이 지켜보지 않으므로 아래를 반드시 지킨다.

- **한 번 실행에 PR 1건만** 처리한다. 여러 건을 몰아 처리하지 않는다.
- **병합은 3-3 조건을 전부 만족할 때만** 한다. 애매하면 병합이 아니라 `review:hold`다.
  판정이 갈리는 경우의 기본값은 **보류**다.
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
| `review:hold` | 게이트는 통과했으나 사람 판단이 필요해 자동 병합하지 않음 |
| `review:changes-requested` | 반려됨, 구현 담당이 수정 중 |

Issue 쪽 상태(`status:needs-review` 등)는 86번 문서 전이표를 따른다.

## 7. 관련 문서

- [`87-spec-driven-delegation.md`](./87-spec-driven-delegation.md) — 티어, 분할, 병합 권한
- [`86-multi-tool-issue-ownership.md`](./86-multi-tool-issue-ownership.md) — Issue 상태 전이
- [`60-rebase-merge-policy.md`](./60-rebase-merge-policy.md) — rebase/conflict 안전 규칙
- [`40-test-checklist.md`](./40-test-checklist.md) — 검증 체크리스트
- [`.github/workflows/agent-merge-guard.yml`](../../.github/workflows/agent-merge-guard.yml) — PR 규율 CI 검사
