# Spec-Driven Delegation

상위 추론 모델이 **원인 분석·설계·작업 지시**까지 끝낸 Issue를 만들고, 저비용/고속 모델이 그
지시를 **실행만** 하며, 검토·승인·병합은 다시 상위 모델이나 사람이 맡는 운영 방식이다.

초보자 설명: 설계도를 잘 그리는 사람이 도면을 끝까지 그리고, 시공은 빠른 작업자가 하고,
준공 검사는 다시 설계자가 한다. 시공자가 스스로 "준공 승인"을 내지 못하게 하는 것이 핵심이다.

이 문서는 [`86-multi-tool-issue-ownership.md`](./86-multi-tool-issue-ownership.md)의 Issue
소유 규약과 [`70-model-assignment-policy.md`](./70-model-assignment-policy.md)의 capability
기준을 전제로 하며, 그 위에 **티어 분리와 병합 권한**만 추가한다.

## 1. 세 가지 역할

역할은 벤더 이름이 아니라 **capability tier**로 정의한다(70번 문서 원칙과 동일).

| 역할 | 티어 | 하는 일 | 하지 않는 일 |
| --- | --- | --- | --- |
| **Spec Author** | High Reasoning | 재현·근본원인·설계·대안기각·작업지시·allowlist·검증계획 작성 | 구현하지 않는다 |
| **Implementer** | Fast/Low-Cost 또는 Balanced | 지시된 파일만 수정, 테스트 실행, Draft PR 생성 | 설계 판단, 범위 확장, 승인, 병합 |
| **Reviewer / Integrator** | High Reasoning | 독립 검토, 승인, 병합, Issue 상태·라벨 갱신 | 구현 owner를 대신해 코드를 고치지 않는다 |

Spec Author와 Reviewer는 같은 티어이나 **같은 세션이어서는 안 된다.** 자기 설계를 자기가
검토하면 설계 전제의 오류가 그대로 통과한다.

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

## 3. 흐름

```
[Spec Author]  Issue 작성 (status:draft)
      │        원인·설계·작업지시·allowlist·검증
      ▼
[Integrator]   Ready 체크리스트 검증 → status:ready
      │
      ▼
[Integrator]   claim 승인 → status:in-progress + agent:<tool>
      │
      ▼
[Implementer]  브랜치/워크트리에서 지시 실행 → 검증 → Draft PR (Refs #N)
      │        ※ 여기서 멈춘다. 병합하지 않는다.
      ▼
[Integrator]   status:needs-review
      │
      ▼
[Reviewer]     독립 검토 (read-only) → 승인 또는 findings 반려
      │
      ▼
[Integrator]   병합 → Issue 종료 → 브랜치/워크트리 정리
```

`status:draft`는 이 문서에서 추가하는 상태다. 설계가 아직 완성되지 않은 Issue를 구현
모델이 집어가는 것을 막는다. 나머지 상태 전이는 86번 문서 표를 따른다.

## 4. 병합 권한 (Merge Authority)

**구현 담당은 PR 생성까지만 한다.**

구현 담당이 하지 않는 것:

- `main`으로의 병합 (`gh pr merge`, GitHub UI Merge 버튼)
- 자신의 PR 승인 (`gh pr review --approve`)
- `main`에 직접 push
- Issue 라벨·assignee·상태 변경 (86번 문서에 따라 Integrator 전용)
- 다른 Issue의 브랜치나 PR 조작

구현 담당이 하는 것:

- 배정된 브랜치/워크트리에서 allowlist 내 파일 수정
- 검증 명령 실행과 결과 기록
- **Draft** PR 생성, 본문에 검증 출력 첨부, `Refs #<issue>` 사용
- 막히면 진행하지 말고 Integrator에게 보고

PR 본문에는 다음 줄을 넣어 권한을 명시한다.

```text
Implementer tier: Fast/Low-Cost
Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.
```

## 5. 강제 수준 — 솔직한 한계

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

계약 파일이 이 문서보다 **더 엄격한** 제약을 선언하면 엄격한 쪽을 따른다. 예를 들어
`GEMINI.md`는 Gemini의 PR을 사람이 직접 승인·병합하도록 요구하므로, Gemini 구현분에
대해서는 상위 모델 Integrator 병합이 아니라 사람 병합이 적용된다.

## 6. CI 가드

[`.github/workflows/agent-merge-guard.yml`](../../.github/workflows/agent-merge-guard.yml)이
PR에서 다음을 검사한다.

- 구현 owner 라벨(`agent:*`)이 붙은 PR에 리뷰 승인 흔적이 있는가
- PR이 Draft를 벗어났는데 `status:needs-review` 이상으로 진행되었는가
- PR 본문에 검증 출력과 merge authority 문구가 있는가

branch protection이 없으므로 이 검사는 **실패를 보여줄 뿐 병합을 막지 못한다.**
required status check로 승격하려면 5절의 1번이 필요하다.

## 7. 관련 문서

- [`70-model-assignment-policy.md`](./70-model-assignment-policy.md) — capability 기준
- [`86-multi-tool-issue-ownership.md`](./86-multi-tool-issue-ownership.md) — claim/상태/handoff
- [`60-rebase-merge-policy.md`](./60-rebase-merge-policy.md) — rebase/merge 안전 규칙
- [`80-file-ownership.md`](./80-file-ownership.md) — 역할별 파일 소유
- `.github/ISSUE_TEMPLATE/agent-implementation-spec.yml` — 설계·작업지시 Issue 폼
