# discovery docs

`discovery`는 Netflix Eureka Server 기반 서비스 레지스트리입니다. 각 서비스가 자신을 등록하고, Gateway와 다른 서비스가 이름으로 현재 위치를 찾게 합니다.

기존 `concept.md`의 유효한 개념은 현재 문서에 통합했고, 인코딩이 깨진 과거 원문은 [archive/concept_legacy_corrupt_2026-06-11.md](./archive/concept_legacy_corrupt_2026-06-11.md)에 보존했습니다.

## 읽기 순서

1. [concept.md](./concept.md) - registry 책임, self-preservation, 인프라 경계.
2. [process-flow.md](./process-flow.md) - register/heartbeat/fetch/cancel/eviction 데이터 흐름.
3. [local-run.md](./local-run.md) - IntelliJ, Gradle, Docker 실행과 확인/종료.
4. [../README.md](../README.md) - 모듈 전체 요약과 운영 TODO.

## 현재 실행 전제

- Spring Boot Eureka Server 애플리케이션입니다.
- Config Client가 실제 의존성으로 포함되며 Config Server import는 optional입니다.
- Config가 없어도 8761, 자기 등록/fetch 비활성 단일 노드로 실행됩니다.
- H2/PostgreSQL은 사용하지 않습니다.
- actuator readiness와 Prometheus endpoint를 제공합니다.
- `.run/Discovery standalone bootRun.run.xml`은 외부 의존성 없는 smoke용입니다.
- `.run/Discovery bootRun.run.xml`은 Config Server와 함께 쓰는 통합 실행용입니다.

## 핵심 파일

| 파일 | 역할 |
|---|---|
| `DiscoveryApplication` | Eureka Server 실행 진입점 |
| `src/main/resources/application.yml` | standalone 기본 계약 |
| `config-repo/discovery-service.yml` | 중앙 설정, 보안/HA TODO |
| `DiscoveryApplicationTests` | readiness와 registry 생명주기 |
| `DiscoveryConfigurationPolicyTest` | YAML/Compose/Docker 정책 |
| `Dockerfile` | JDK 17 bootJar 이미지와 readiness healthcheck |

## 변경 시 함께 확인할 항목

1. 포트/hostname/defaultZone이 local, Config, Docker에서 일치하는가?
2. Discovery 자체의 register/fetch가 계속 `false`인가?
3. readiness가 실제 actuator 의존성과 연결되어 있는가?
4. 모든 Compose 의존 서비스가 `service_healthy`를 기다리는가?
5. 보안 또는 peer 정책 변경이 문서와 TODO에 반영됐는가?
