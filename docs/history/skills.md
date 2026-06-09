# 프로젝트 수행 스킬 가이드

이 문서는 `account` 프로젝트를 지속적으로 수행할 때 필요한 "프로젝트 전용 스킬"을 정의한다.  
기존 문서는 일반적인 역할 설명에 머물러 있었고, 실제 저장소에서 바로 재사용할 수 있는 작업 규칙과 진입 순서가 부족했다.  
이 문서는 다음 두 가지 목적을 가진다.

1. 새로운 세션이나 다른 에이전트가 바로 작업을 이어갈 수 있도록 공통 작업 규칙을 제공한다.
2. 문서형 가이드에 그치지 않고 실제 `SKILL.md`가 어떻게 동작해야 하는지 기준을 제공한다.

## 현재 문제점

- `docs/skills.md`가 실제 프로젝트 작업 절차보다 추상적이었다.
- `docs/GEMINI_SKILL.md`는 과거 특정 에이전트/도구 중심 설명이 많아 현재 저장소 운영 기준과 어긋나는 부분이 있었다.
- 저장소 루트에 실제 참조 가능한 `SKILL.md`가 없어 "스킬처럼" 일관되게 동작하기 어려웠다.
- `todo.md`, `WORKLOG.md`, 도메인 문서, DB 스키마 문서를 어떤 순서로 읽고 어떤 기준으로 갱신해야 하는지 명확하지 않았다.

## 이 프로젝트에 필요한 핵심 스킬

### 1. 세션 복구 스킬
- 작업 시작 전 반드시 `WORKLOG.md`를 먼저 읽는다.
- 다음으로 `docs/todo.md`를 읽고 완료(`o`) 여부와 남은 DoD를 확인한다.
- 현재 작업 도메인이 있으면 해당 도메인 문서를 추가로 읽는다.
  - 대사: `docs/reconciliation.md`
  - 대출회계: `docs/loan_accounting.md`
  - 결산: `docs/closing.md`
  - 재무보고: `docs/domain-catalog.md`, `docs/db/README.md`, `docs/db/legacy/report_schema.sql`
- 코드 변경 전 `git status --short`로 기존 변경사항을 확인한다.

### 2. 정합성 유지 스킬
- 문서, 코드, 테스트, DDL 중 하나를 바꾸면 관련 산출물도 같이 맞춘다.
- `todo.md` 완료 표시는 실제 코드/테스트/문서가 모두 맞을 때만 `o`로 바꾼다.
- `WORKLOG.md`에는 기능 완료, DoD 보강, 정합성 수정 이력을 남긴다.
- 새 API나 엔티티를 추가하면 가능한 경우 관련 문서와 `docs/db/*.sql`도 같이 갱신한다.
- DB 작업 전에는 `docs/db/README.md`를 먼저 보고 해당 DDL이 현행인지, 부분 일치인지, 레거시인지 확인한다.

### 3. 회계 도메인 구현 스킬
- 금액 계산은 반드시 `BigDecimal` 기준으로 본다.
- 회계/대사/결산 기능은 DoD 기준으로 본다.
- 단순 CRUD보다 다음을 우선 확인한다.
  - 차대 일치
  - 상태 전이
  - 마감 잠금
  - SOD/승인 분리
  - 라인리지
  - 조정 전표 연결

### 4. 테스트 보강 스킬
- 서비스 로직을 추가하면 최소한 서비스 테스트 또는 컨트롤러 테스트를 같이 보강한다.
- 통합 시나리오가 있는 도메인은 E2E 성격의 Integration Test 유무를 확인한다.
- 테스트를 실행하지 못하면 이유를 `JAVA_HOME`, 외부 의존성, 환경 제한 단위로 명시한다.

### 5. 커밋 정리 스킬
- 커밋 전 `git diff --stat`로 변경 묶음을 확인한다.
- unrelated 변경이 섞였으면 그대로 커밋하지 말고 범위를 분리할지 먼저 판단한다.
- 커밋 메시지는 "문서 정합성", "DoD 보강", "도메인 구현" 같은 실제 묶음을 드러내야 한다.

## 권장 작업 순서

1. `WORKLOG.md` 확인
2. `docs/todo.md` 확인
3. 관련 도메인 문서 확인
4. 현재 코드/테스트/DDL 상태 확인
5. 구현 또는 수정
6. 테스트 또는 검증
7. `todo.md`/`WORKLOG.md` 정합성 반영
8. 커밋

## 실제 스킬 파일 기준

이 프로젝트에서 "스킬처럼 동작"하려면 최소한 아래가 있어야 한다.

- 저장소 루트 `SKILL.md`
- 세션 시작 시 읽을 문서 우선순위
- 문서/코드/DDL/테스트 동시 갱신 규칙
- 완료 처리 기준
- 커밋 전 점검 규칙

즉, `docs/skills.md`는 설명서이고, 실제 동작 기준은 루트 `SKILL.md`에 있어야 한다.

## 관련 문서

- `WORKLOG.md`
- `docs/todo.md`
- `docs/principles_and_policies.md`
- `docs/domain-catalog.md`
- `docs/reconciliation.md`
- `docs/loan_accounting.md`
- `docs/closing.md`
- `docs/db/README.md`
- `docs/db/legacy/table_spec.md`
