# config-server docs

`config-server`는 Spring Cloud Config Server입니다. 로컬에서는 루트 `config-repo` 폴더를 읽어 각 서비스 설정을 제공합니다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 중앙 설정 서버가 왜 필요한지 설명합니다.
2. [process-flow.md](./process-flow.md) - 서비스가 설정을 받아가는 흐름.
3. [local-run.md](./local-run.md) - IntelliJ와 Gradle 실행 방법.

## 현재 실행 전제

- `config-server`는 Spring Boot 앱입니다.
- 기본 포트는 `8888`입니다.
- `config-server/src/main/resources/application.yml`은 native profile로 `file://${user.dir}/config-repo`를 읽습니다.
- 로컬 실행 설정은 `.run/Config Server bootRun.run.xml`을 사용합니다.

## 같이 볼 문서

- [../README.md](../README.md)
- [../../config-repo/README.md](../../config-repo/README.md)
