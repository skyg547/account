# 프로젝트 작업 스킬 메모

이 문서는 특정 모델 전용 지침서가 아니라, `account` 프로젝트에서 공통으로 적용해야 하는 작업 원칙을 정리한 문서다.

## 프로젝트 상태 인식

- 이 저장소는 단순 예제가 아니라 재무회계 도메인별 구현, 테스트, DDL, 운영 문서를 함께 관리한다.
- 따라서 작업은 코드만 수정해서 끝나지 않는다.
- `todo.md`, `WORKLOG.md`, 도메인 문서, 테스트, DDL까지 함께 맞춰야 한다.

## 작업 시작 시 해야 할 일

1. `docs/WORKLOG.md`를 읽고 최근 작업 이력을 확인한다.
2. `docs/todo.md`를 읽고 완료(`o`)와 미완료 항목을 확인한다.
3. 현재 작업 대상 도메인 문서를 읽는다.
4. `git status --short`로 기존 변경사항을 확인한다.

## 작업 중 원칙

- 코드, 문서, 테스트, DDL 간 정합성을 유지한다.
- 회계 기능은 DoD 기준으로 완성 여부를 판단한다.
- 완료되지 않은 기능을 완료 처리하지 않는다.
- 새로운 API나 해결 흐름을 추가하면 테스트와 문서를 같이 갱신한다.

## 작업 종료 시 원칙

- 필요한 경우 `docs/todo.md` 완료 표기를 갱신한다.
- `docs/WORKLOG.md`에 변경 내용을 기록한다.
- 커밋 전 변경 묶음이 일관적인지 확인한다.

## 권장 참조 문서

- `docs/skills.md`
- `docs/WORKLOG.md`
- `docs/todo.md`
- `docs/principles_and_policies.md`
- `docs/domain-catalog.md`
- 도메인별 문서 및 `docs/db/README.md`, `docs/db/current/*.sql`, `docs/db/legacy/*.sql`
