# Repository Docs Hub

이 디렉터리는 저장소 전체 문서 허브입니다.
업무/개발 초보자가 모듈 구조와 흐름을 이해할 수 있도록 문서를 분류해 제공합니다.

## 1. 먼저 읽기 (입문 순서)

1. [../BEGINNER_GUIDE.md](/C:/Users/skyg547/IdeaProjects/account/BEGINNER_GUIDE.md)
2. [business_workflow.md](./business_workflow.md)
3. [domain-catalog.md](./domain-catalog.md)
4. [service-discovery-model.md](./service-discovery-model.md)

## 2. 아키텍처/원칙 문서

- [principles_and_policies.md](./principles_and_policies.md)
- [msa-modularization.md](./msa-modularization.md)
- [msa-execution-and-work-plan.md](./msa-execution-and-work-plan.md): 실행 순서, 작업 우선순위, 실제 운영 적용 순서
- [infrastructure-guide.md](./infrastructure-guide.md)

## 3. 모듈 공통 읽기 규칙

각 업무 모듈은 아래 3종 문서를 기본으로 유지합니다.

- `docs/beginner-guide.md`: 업무/도메인 개념 입문
- `docs/process-flow.md`: API/유즈케이스 처리 흐름
- `docs/schema.md`: 데이터 구조와 핵심 컬럼

## 4. 초보자 권장 학습 경로

### 4.1 업무 흐름 먼저

- `expenditure-resolution/docs/*`
- `tax/docs/*`
- `governance/docs/*`

### 4.2 회계 엔진 흐름 확장

- `journal-ledger/docs/*`
- `closing/docs/*`
- `reporting/docs/*`

### 4.3 기준정보/연계 이해

- `master-data/docs/*`
- `contracts/docs/*`
- `shared-kernel/docs/*`

## 5. 문서 최신화 원칙

- 코드 경로가 바뀌면 문서 링크도 같은 커밋에서 수정합니다.
- 비즈니스 규칙(검증, 상태전이, 배치 재실행 정책)이 바뀌면 흐름 문서를 먼저 갱신합니다.
- 테스트를 실행하지 못했으면 문서/작업로그에 이유를 남깁니다.

## 6. 문서 깨짐 점검 팁

- UTF-8 인코딩 확인
- 상대/절대 링크 존재 여부 확인
- 오래된 이력 문서의 경로는 "당시 기준" 표시를 추가해 오해를 줄입니다.

필요하면 이 허브에 모듈별 온보딩 링크를 계속 추가해 확장합니다.
