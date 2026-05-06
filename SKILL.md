# Account Project Skill

이 저장소에서 작업할 때는 아래 순서를 따른다.

## 시작 순서

1. `WORKLOG.md`를 읽는다.
2. `docs/todo.md`를 읽는다.
3. 현재 작업 도메인 문서를 읽는다.
4. `docs/db/README.md`로 현행/레거시 DDL을 구분한다.
5. 구조 변경이면 `docs/msa-modularization.md`를 읽는다.
6. `git status --short`로 기존 변경을 확인한다.

## 핵심 규칙

- 문서, 코드, 테스트, DDL 정합성을 같이 맞춘다.
- `docs/db/*.sql`는 직접 신뢰하지 말고 먼저 `docs/db/README.md` 분류를 본다.
- 완료된 항목만 `docs/todo.md`에 `o` 표시한다.
- 중요한 변경은 `WORKLOG.md`에 남긴다.
- 회계 기능은 CRUD보다 DoD와 상태 전이, 마감, 승인, 라인리지, 조정 전표 연결을 우선 확인한다.
- 금액 계산은 `BigDecimal` 기준으로 본다.

## 종료 순서

1. 변경 검증
2. `todo.md` / `WORKLOG.md` 반영
3. 커밋 범위 확인
4. 커밋
