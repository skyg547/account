# Shared-Kernel 문서 인덱스

`shared-kernel`은 직접 업무를 실행하지 않고 여러 모듈이 합의한 최소 타입과 기술 독립 규칙을
제공하는 라이브러리입니다. 현재는 역사적으로 인프라 의존성과 ECL/account-mart 타입도 함께
들어 있어 축소 TODO가 있습니다.

## 읽는 순서

1. [beginner-guide.md](./beginner-guide.md): 공유 커널에 넣을 것과 넣지 않을 것
2. [process-flow.md](./process-flow.md): 로컬 capability 검색과 JSON 마스킹 흐름
3. [schema.md](./schema.md): 현재 타입과 소유권/이동 계획
4. [local-run.md](./local-run.md): IntelliJ와 Gradle 검증
5. [archive/legacy-runtime-skeleton/README.md](./archive/legacy-runtime-skeleton/README.md): 빈 Docker 이력

## 판단 기준

공유 후보는 다음 질문을 모두 통과해야 합니다.

- 둘 이상의 바운디드 컨텍스트가 같은 의미로 사용하는가?
- 변경 승인자를 명확히 정할 수 있는가?
- 특정 DB, 메시지 브로커, 웹 프레임워크가 없어도 의미가 있는가?
- 특정 업무 계산이나 상태 전이를 포함하지 않는가?
- 직렬화/DB 값 호환 정책이 문서화되어 있는가?