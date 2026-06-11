# discovery docs

`discovery`는 Netflix Eureka Server 기반 서비스 레지스트리입니다. 각 마이크로서비스가 자신을 등록하고, Gateway나 다른 서비스가 이름으로 위치를 찾을 수 있게 합니다.

기존 `concept.md`는 앞부분은 유효했지만 뒤쪽에 인코딩이 깨진 과거 내용이 붙어 있어 [archive/concept_legacy_corrupt_2026-06-11.md](./archive/concept_legacy_corrupt_2026-06-11.md)로 이동해 보존했습니다.

## 읽기 순서

1. [concept.md](./concept.md) - Eureka와 서비스 등록 개념.
2. [local-run.md](./local-run.md) - IntelliJ와 Gradle 로컬 실행 방법.

## 현재 실행 전제

- `discovery`는 Spring Boot Eureka Server 앱입니다.
- Config Server import는 optional입니다.
- 실제 포트는 `config-repo/discovery-service.yml` 기준으로 `8761`입니다.
- 로컬 실행 설정은 `.run/Discovery bootRun.run.xml`을 사용합니다.
