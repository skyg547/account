# App Module Docs

`app` 모듈은 모든 업무 모듈을 묶어 실행하는 Spring Boot 진입점입니다.

## 문서 목록

- [process-flow.md](./process-flow.md): 애플리케이션 부팅과 프로파일 적용 흐름을 설명합니다.
- [schema.md](./schema.md): 설정 파일과 엔트리포인트 구조를 정리합니다.
- [beginner-guide.md](./beginner-guide.md): 초보자가 이 모듈의 역할을 쉽게 이해할 수 있도록 설명합니다.

## 이 모듈이 하는 일

1. 전체 모듈을 하나의 실행 애플리케이션으로 묶습니다.
2. Spring Boot 시작점을 제공합니다.
3. 환경별 설정 파일을 로드합니다.
4. Actuator, JPA, Web, Validation 등 공통 런타임 구성을 활성화합니다.

## 핵심 파일

- `AccountApplication`
- `application.yml`
- `application.yaml`
- `application-dev.yml`
- `application-prod.yml`

## 현재 구현 기준에서 먼저 알아둘 점

- 실제 비즈니스 로직은 없고 조립/실행 역할만 합니다.
- 기본 활성 프로파일이 `local`로 지정되어 있지만, 현재 리포지토리에는 `application-local.yml`이 없습니다.
- 대신 `application-dev.yml`과 `application-prod.yml`이 존재합니다.
- `application.yml`과 `application.yaml`이 동시에 있어 기본 설정 파일이 이중 존재합니다.
