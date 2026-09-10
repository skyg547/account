# Spec-Driven Delegation

상위 추론 모델이 **원인 분석·설계·작업 지시**까지 끝낸 Issue를 만들고, 저비용/고속 모델이 그
지시를 **실행만** 하며, 별도 Reviewer가 검토 증거를 반환하고 승인된 부모 Integrator가 외부 변경을 맡는 운영 방식이다.

초보자 설명: 설계도를 잘 그리는 사람이 도면을 끝까지 그리고, 시공은 빠른 작업자가 하고,
준공 검사는 별도 검사자가 한다. 시공자나 설계자가 자기 작업을 독립 검토한 것으로 취급하지 않는 것이 핵심이다.

이 문서는 [`86-multi-tool-issue-ownership.md`](./86-multi-tool-issue-ownership.md)의 Issue
소유 규약과 [`70-model-assignment-policy.md`](./70-model-assignment-policy.md)의 capability
기준을 전제로 하며, **티어 분리와 작업 단위 분할**을 설명한다. Git/GitHub 권한은 30/80/85/86의 부모 단일 writer 계약을 따른다.

## 1. 역할과 권한 분리

역할은 벤더 이름이 아니라 **capability tier**로 정의한다(70번 문서 원칙과 동일).

| 역할 | 티어 | 대응 난이도 | 하는 일 | 하지 않는 일 |
| --- | --- | --- | --- | --- |
| **Spec Author** | High Reasoning (`high` / `xhigh`) | 상(`difficulty:high`), 최상(`difficulty:very-high`) | 재현·근본원인·설계·대안기각·작업지시·allowlist·검증계획 작성 | 구현하지 않는다 |
| **Implementer** | Fast/Low-Cost 또는 Balanced | 하(`difficulty:low`), 중(`difficulty:medium`) | 지시된 파일만 수정, 테스트 실행, PR 본문 초안·증거 반환 | 설계 판단, 범위 확장, Git/GitHub 변경, 승인, 병합 |
| **Reviewer / advisory Integrator** | High Reasoning (`high` / `xhigh`) | 전 난이도 독립 검증 | 읽기 전용 검토 후 통과·반려·보류 의견과 증거 반환 | 코드·로그 수정, Git/GitHub 변경 |
| **부모 Integrator** | 작업 위험도에 맞게 선택 | 승인 범위 내 조정 | 증거 수집, 공유 기록, 승인된 Git/GitHub 변경 | 승인 범위 확대, 독립 리뷰 생략 |

모델 티어는 검토 역량이며 외부 변경 권한이 아니다. 부모 Integrator만 사용자 요청 또는 명시 workflow 승인 범위 안에서 Git/GitHub 상태를 변경한다.

Spec Author와 Reviewer는 같은 티어이나 **같은 세션이어서는 안 된다.** 자기 설계를 자기가
검토하면 설계 전제의 오류가 그대로 통과한다. 상세 난이도 체계와 추론 강도 매핑은 [`70-model-assignment-policy.md`](./70-model-assignment-policy.md)를 따른다.

## 2. Fast 티어에 넘겨도 되는 조건

Issue가 아래를 **모두** 만족할 때만 `Fast/Low-Cost`로 구현을 지정한다.

- 근본 원인이 이미 특정되어 있고, 구현자가 원인을 다시 조사할 필요가 없다.
- 작업 지시가 파일 단위로 순서화되어 있다.
- allowlist가 명시되어 있고 다른 진행 중 Issue와 겹치지 않는다.
- 검증 명령이 그대로 실행 가능하고 기대 결과가 적혀 있다.
- 실패해도 되돌리기 쉽다(롤백이 revert 수준).

아래 중 **하나라도** 해당하면 Fast 티어를 쓰지 않는다. 70번 문서의 escalation 규칙과 같다.

- 금액 계산, 차변/대변 방향, 반올림·scale 등 회계 정합성
- 동시성, 락, 멱등성, 배치 재시작
- DB migration, 스키마 변경, forward-only 제약
- 보안 경계(JWT, 권한, actor 신뢰), 자격증명 취급
- 공개 계약 변경(`contracts`, `shared-kernel`, Gateway route)
- 여러 모듈에 걸친 blast radius

> Fast 티어의 실패 양상은 "못 한다"가 아니라 **"지시를 넘어서 그럴듯하게 고친다"** 이다.
> 그래서 allowlist와 비목표(Non-Goals)가 품질 장치의 핵심이다.

## 3. 작업 단위 분할 (Work Unit Sizing)

**Issue는 최대한 잘게 쪼개서 발행한다.** 하나의 Issue는 하나의 근본 원인, 하나의 브랜치,
하나의 PR로 끝나야 한다. Fast 티어는 큰 작업을 스스로 쪼개지 못하고, 쪼개지 않은 Issue를
받으면 일부만 고치고 완료로 보고하거나 범위를 넘어 손을 댄다.

초보자 설명: 큰 상자 하나를 통째로 옮기라고 하면 떨어뜨리지만, 작은 상자 여러 개로 나눠서
하나씩 옮기라고 하면 안 떨어뜨린다. 그리고 하나를 떨어뜨려도 나머지는 무사하다.

### 3-1. 크기 상한

executable Issue 하나는 아래를 **모두** 만족해야 한다. 하나라도 넘으면 쪼갠다.

| 항목 | 상한 |
| --- | --- |
| 근본 원인 | **1개** |
| 대상 모듈 | **1개** (`contracts`/`shared-kernel` 동반 변경은 별도 Issue) |
| 변경 파일 | 실질 변경 **5개 이하** (테스트 파일 포함) |
| 작업 지시 단계 | **7단계 이하** |
| 검증 | 명령 **한 묶음**으로 통과 판정 가능 |
| 리뷰 | 한 번에 읽고 판단 가능한 diff |

숫자는 절대 기준이 아니라 **쪼갤지 판단하는 트리거**다. 5개 파일을 넘더라도 같은 치환을
기계적으로 반복하는 경우(예: 같은 오타를 8개 파일에서 고침)는 하나로 둔다. 반대로 파일이
2개여도 서로 다른 판단이 필요하면 쪼갠다. **기준은 파일 수가 아니라 "판단의 개수"다.**

### 3-2. 반드시 쪼개야 하는 신호

- 제목에 "및", "그리고", "·"로 서로 다른 문제가 나열된다.
- 근본 원인이 2개 이상이거나 "A 때문이거나 B 때문"으로 미확정이다.
- 일부는 설정 변경, 일부는 코드 변경처럼 **검증 방법이 다르다**.
- 일부만 먼저 병합해도 의미가 있다 → 그 일부가 독립 Issue다.
- 조사(원인 규명)와 수정이 한 Issue에 섞여 있다 → 조사는 Spec Author의 일이지 Issue가 아니다.
- 어떤 항목은 이미 해결됐고 어떤 항목은 남아 있다.

> 실제 사례: #474는 개발서버 점검에서 나온 **5개 문제를 한 Issue에 묶어** 발행됐다.
> 2026-08-19 재검증 결과 4개는 이미 해소되어 있었고 실제 남은 작업은 설정 파일 2줄뿐이었다.
> 쪼개져 있었다면 4개는 진작 닫히고 1개만 남았을 것이다. 묶인 Issue는 **무엇이 끝났는지
> 아무도 모르는 상태**로 오래 열려 있게 된다.

### 3-3. 쪼개는 방법

큰 문제는 **부모 추적 Issue 1개 + 실행 가능한 자식 Issue N개**로 나눈다.

- **부모 Issue**: 전체 맥락, 배경, 자식 목록과 순서를 담는다. 라벨은 `spec-driven`만 붙이고
  `status:ready`를 붙이지 않는다. 부모는 구현 대상이 아니다.
- **자식 Issue**: 각각 3-1의 상한을 만족하고, 각각 독립적으로 병합 가능해야 한다.
  본문 첫 줄에 `Parent: #<부모번호>`를 적는다.

자식끼리 순서 의존이 있으면 `Depends on: #<번호>`를 명시하고, 선행 Issue가 병합되기 전에는
`status:ready`로 올리지 않는다. **동시에 진행할 자식들은 allowlist가 서로 겹치지 않아야 한다**
(86번 문서의 병렬 작업 규칙).

쪼개는 축은 보통 이 중 하나다.

1. **모듈별** — 같은 성격의 수정이 여러 모듈에 필요할 때
2. **레이어별** — domain 규칙 / adapter 연동 / 테스트 보강
3. **원인별** — 증상이 하나여도 원인이 여럿일 때
4. **선행-후행** — 계약 변경 먼저, 소비자 반영 나중

### 3-4. 쪼갤 수 없을 때

정말 나눌 수 없는 변경(예: 하나의 계약 변경과 그 소비자를 동시에 고쳐야 컴파일되는 경우)은
쪼개지 말고 **Fast 티어에 주지 않는다.** Balanced 이상으로 올린다. 억지로 쪼개서 중간 상태가
빌드되지 않게 만드는 것이 더 나쁘다.

## 4. 흐름

```
[Spec Author]  Issue 작성 (status:draft)
      │        원인·설계·작업지시·allowlist·검증
      ▼
[부모]         Issue Ready 체크리스트 검증 → status:ready
      │
      ▼
[부모]         claim 승인 → status:in-progress + agent:<tool>
      │
      ▼
[Implementer]  브랜치/워크트리에서 지시 실행 → 검증 → PR 본문 초안·증거 반환
      │        ※ Git/GitHub 상태를 변경하지 않는다.
      ▼
[부모]         승인 범위 내 commit/push/Draft PR (Refs #N), status:needs-review
      │
      ▼
[Reviewer]     명시 요청된 동결 Draft 사전리뷰 (읽기 전용), 실행 가능한 Draft 단계 필수 검사 통과
      │
      ▼
[부모]         승인된 Ready 전환
      ▼
[Reviewer]     최신 head/base 기준 최종 독립 검토, Ready-only 포함 모든 필수 CI 통과 → 통과·반려·보류 증거
      ▼
[부모]         별도 승인된 merge commit → 종료 승인·조건 확인 → 별도 승인된 cleanup
```

`status:draft`는 이 문서에서 추가하는 상태다. 설계가 아직 완성되지 않은 Issue를 구현
모델이 집어가는 것을 막는다. 나머지 상태 전이는 86번 문서 표를 따른다.

Draft 사전리뷰는 동결 diff와 그 단계에서 실행 가능한 검사를 대상으로 한다.
Ready-only 검사는 미도래/pending으로 기록하며 PASS가 아니지만 Draft 사전리뷰를 막지 않는다.
Draft 단계 필수 검사의 오류·실패는 여전히 보류 또는 확인된 코드 결함 반려다.
Draft 검사와 사전리뷰 완료 후 부모의 승인된 Ready 전환을 거쳐 최종 독립 검토를 진행한다.

## 5. 병합 권한 (Merge Authority)

**구현 담당은 구현·검증·PR 본문 초안 반환까지만 한다.** PR 생성과 Ready 전환도 부모 소유다.

구현 담당이 하지 않는 것:

- `main`으로의 병합 (`gh pr merge`, GitHub UI Merge 버튼)
- 자신의 PR 승인 (`gh pr review --approve`)
- `main`에 직접 push
- Issue 라벨·assignee·상태 변경 (86번 문서에 따라 Integrator 전용)
- 다른 Issue의 브랜치나 PR 조작
- commit/stage/push, Draft PR 생성, Ready 전환, 리뷰 게시 등 Git/GitHub 변경

구현 담당이 하는 것:

- 배정된 브랜치/워크트리에서 allowlist 내 파일 수정
- 검증 명령 실행과 결과 기록
- PR 본문 초안에 검증 결과·미실행 사유·롤백과 `Refs #<issue>`를 넣어 부모에게 반환
- 막히면 진행하지 말고 Integrator에게 보고

PR 본문에는 다음 줄을 넣어 권한을 명시한다.

```text
Implementer tier: Fast/Low-Cost
Merge authority: 독립 Reviewer 증거와 최신 head/base·CI 확인 후, 별도 승인된 부모 Integrator만 merge commit 수행. 구현자/Reviewer는 Git/GitHub 변경 없음.
```

사람 리뷰가 기본 게이트다. 기존 명시 승인 workflow 예외는 등록된 승인 범위에서만 적용한다.
Draft PR 제출 요청은 Ready/merge/Issue close/cleanup 승인으로 확대하지 않는다. head/base 또는
검증 결과가 바뀌면 재검토하며, 확인 불가·충돌은 보류한다. 단계별 기준은 [88번 문서](./88-pr-review-and-merge-runbook.md)를 따른다.

## 6. 강제 수준 — 솔직한 한계

**이 저장소에서는 위 규칙을 GitHub이 강제하지 못한다.**

`skyg547/account`는 **Free 플랜의 private 저장소**이므로 branch protection과 ruleset API가
막혀 있다(확인: `GET /repos/.../branches/main/protection` → `403 Upgrade to GitHub Pro or
make this repository public`). 즉 "required review", "required status check",
"restrict who can push"를 켤 수 없다.

그래서 현재 강제력은 이렇게 나뉜다.

| 통제 | 지금 가능한가 | 비고 |
| --- | --- | --- |
| 규약 문서 + Issue/PR 템플릿 | ✅ | 모델이 지시를 따를 때만 유효 (soft) |
| CI 경고 (`agent-merge-guard`) | ⚠️ | 검사 실패는 보이지만 **병합을 막지는 못한다** |
| 병합 차단 (required review) | ❌ | branch protection 필요 |
| main 직접 push 차단 | ❌ | branch protection 필요 |

실제로 막고 싶다면 아래 중 하나가 필요하다.

1. **GitHub Pro로 업그레이드** — private 저장소에서 branch protection/ruleset 사용.
   가장 단순하며, `main`에 "PR 필수 + 승인 1개 필수 + 직접 push 금지"를 켜면 이 문서의
   규칙이 그대로 강제된다.
2. **구현 모델에 별도 GitHub 신원 부여** — 구현 전용 계정이 저장소에 write 권한 없이 read만
   갖고, fork에서 작업해 upstream으로 PR을 연다. 권한이 없으므로 병합 자체가 불가능하다.
   Free 플랜에서도 동작하는 유일한 하드 통제다.
3. **현상 유지(soft)** — 모든 도구가 소유자 계정 하나로 동작하므로 기술적 구분이 없다.
   규약과 CI 경고에만 의존한다.

> **현재 선택: 3번(soft). GitHub 통제는 도입하지 않기로 결정했다.**
>
> 모든 도구가 같은 계정 토큰을 쓰기 때문에, GitHub 입장에서 Spec Author와 Implementer와
> Integrator는 **같은 사용자**다. 이 문서의 분리는 운영 규율이지 접근 통제가 아니다.
> "Gemini는 병합 못 하게 막아 뒀다"가 아니라 **"Gemini는 병합하지 않기로 선언되어 있다"**
> 가 정확한 표현이며, 규칙 위반은 사후에만 발견된다.
>
> 이는 누락이 아니라 의도된 선택이다. 강제가 필요해지는 시점(외부 기여자 합류, 여러 사람이
> 같은 저장소에 쓰기, 실제 사고 발생)에 1번 또는 2번으로 전환한다.

도구별 선언은 각 도구의 계약 파일에도 중복해서 적는다. 도구가 자기 계약 파일만 읽고 이
문서까지 읽지 않을 수 있기 때문이다.

- [`GEMINI.md`](../../GEMINI.md) — Gemini의 병합 금지 선언
- [`CLAUDE.md`](../../CLAUDE.md), [`AGENTS.md`](../../AGENTS.md) — Claude/Codex 역할과 권한

계약 파일이 이 문서보다 **더 엄격한** 제약을 선언하면 엄격한 쪽을 따른다.

도구별 보조 문서에 push/PR 생성 또는 티어 기반 병합을 허용하는 오래된 문구가 남아 있어도
부모 단일 writer 계약을 넓히지 않는다. 도구 이름과 관계없이 구현자/Reviewer는 증거를
반환하고, 승인된 부모가 게시·상태 변경을 맡는다. 보조 문서 전체 정비는 별도 범위다.

## 7. CI 가드

[`.github/workflows/agent-merge-guard.yml`](../../.github/workflows/agent-merge-guard.yml)이
PR에서 다음을 검사한다.

- 구현 owner 라벨(`agent:*`)이 붙은 PR에 리뷰 승인 흔적이 있는가
- PR이 Draft를 벗어났는데 `status:needs-review` 이상으로 진행되었는가
- PR 본문에 검증 출력과 merge authority 문구가 있는가

branch protection이 없으므로 이 검사는 **실패를 보여줄 뿐 병합을 막지 못한다.**
required status check로 승격하려면 6절의 1번이 필요하다.

## 8. 관련 문서

- [`70-model-assignment-policy.md`](./70-model-assignment-policy.md) — capability 기준
- [`86-multi-tool-issue-ownership.md`](./86-multi-tool-issue-ownership.md) — claim/상태/handoff
- [`60-rebase-merge-policy.md`](./60-rebase-merge-policy.md) — rebase/merge 안전 규칙
- [`80-file-ownership.md`](./80-file-ownership.md) — 역할별 파일 소유
- `.github/ISSUE_TEMPLATE/agent-implementation-spec.yml` — 설계·작업지시 Issue 폼
