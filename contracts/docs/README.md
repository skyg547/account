# Contracts 문서 인덱스

`contracts`는 DB나 API 서버가 아니라 모듈 간 Java 계약을 제공하는 `java-library`입니다.
계약 객체에 들어 있는 검증은 통신 형식과 불변성을 지키는 역할이며, 전표 균형이나 예산 판단 같은
업무 계산은 제공 모듈의 도메인에 남습니다.

## 읽는 순서

1. [beginner-guide.md](./beginner-guide.md): 계약과 포트가 필요한 이유
2. [process-flow.md](./process-flow.md): 호출 모듈에서 제공 모듈까지의 실제 흐름
3. [schema.md](./schema.md): 현재 Port/Command/Ref 구조와 변경 규칙
4. [local-run.md](./local-run.md): IntelliJ와 Gradle 검증
5. [archive/legacy-runtime-skeleton/README.md](./archive/legacy-runtime-skeleton/README.md): 과거 Docker 뼈대 보존 기록

## 중요한 경계

- 같은 프로세스: Spring Bean으로 구현 어댑터를 주입해 메서드 호출
- 분리된 서비스: REST/Feign/Kafka 어댑터를 별도로 구현
- 금지: 다른 모듈의 Entity/Repository/Controller를 계약에 노출
- 허용: 기술 독립적인 Port, 불변 Command/Ref, 명시적인 enum/값
- 계약 변경: 호출자, 구현자, JSON/이벤트 호환성, 테스트를 함께 확인