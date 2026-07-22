# Config Server (중앙 설정 서버) - "MSA 레시피 본사"

`config-server`는 각 Spring Boot 서비스가 시작할 때 필요한 포트, 인프라 주소, 기능 설정을 `config-repo`에서 찾아 HTTP로 제공합니다. 기본 포트는 `8888`입니다.

## 초보자를 위한 한 문장

여러 지점이 각자 레시피를 복사해 관리하지 않도록, 지점 이름과 운영 환경에 맞는 레시피를 나눠 주는 프랜차이즈 본사 서버입니다.

## 현재 책임과 아키텍처 경계

```text
Config Client
  -> GET /{application}/{profile}[/{label}]
  -> Spring Cloud Config inbound adapter
  -> EnvironmentRepository
  -> native config-repo adapter
  -> ordered propertySources 응답
```

- `ConfigServerApplication`은 Config Server와 repository probe를 조립하는 실행 진입점입니다.
- Spring Cloud Config가 파일 탐색, profile 병합, `Environment` HTTP 응답 생성을 담당합니다.
- `ConfigRepositoryHealthIndicator`는 대표 설정(`master-data/default`)의 property source가 실제로 존재할 때만 readiness를 UP으로 만듭니다.
- `config-repo`는 로컬/native 설정 원본입니다. Docker에서는 `/config-repo`에 읽기 전용으로 마운트합니다.
- 회계 금액, 승인, 마감 같은 업무 규칙은 이 모듈에 두지 않고 각 업무 모듈의 core/domain에 둡니다.

애플리케이션 클래스가 작은 것은 빈 스켈레톤이기 때문이 아닙니다. 검증된 Config 프레임워크를 어댑터로 사용하고, 이 모듈이 소유해야 할 저장소 가용성·실행·보안·배포 정책과 계약 테스트를 명시적으로 관리합니다.

## 설정 조회 업무 흐름

1. 서비스의 `spring.application.name`과 활성 profile이 조회 키가 됩니다.
2. Config Client가 Config Server에 설정을 요청합니다.
3. Config Server가 `config-repo/{application}.yml` 및 profile 파일을 조회합니다.
4. 우선순위가 있는 `propertySources`를 반환합니다.
5. 클라이언트가 원격 설정과 자신의 로컬/환경변수 설정을 Spring 규칙에 따라 바인딩합니다.
6. 대표 설정을 읽지 못하면 Config Server readiness가 DOWN이 되고, Compose의 15개 의존 서비스는 시작을 기다립니다.

`optional:configserver:`를 사용하는 클라이언트는 Config Server가 없어도 로컬 기본값으로 시작할 수 있습니다. 이는 장애 허용 방식이지 중앙 설정과 동일하게 실행된다는 뜻은 아닙니다.

## 실행과 검증

IntelliJ에서는 `Config Server bootRun`을 실행합니다. 루트 폴더를 기준으로 native 저장소를 읽도록 실행 인자가 포함되어 있습니다.

```powershell
.\gradlew :config-server:test :config-server:bootJar --console=plain --max-workers=1 --no-daemon
.\gradlew :config-server:bootRun --args="--spring.profiles.active=native --server.port=8888 --spring.cloud.config.server.native.search-locations=file:./config-repo" --console=plain --no-daemon
```

서버가 준비됐는지 확인합니다.

```powershell
(Invoke-WebRequest http://localhost:8888/actuator/health/readiness).StatusCode
(Invoke-WebRequest http://localhost:8888/master-data/default).StatusCode
```

설정 조회 응답에는 내부 설정이 포함될 수 있으므로 공유 로그나 이슈에 응답 본문을 붙이지 않습니다. 이 모듈은 DB를 사용하지 않으므로 H2/PostgreSQL 설정이 필요하지 않습니다.

## 문서 읽기 순서

1. [docs/beginner-guide.md](./docs/beginner-guide.md)
2. [docs/process-flow.md](./docs/process-flow.md)
3. [docs/local-run.md](./docs/local-run.md)
4. [docs/README.md](./docs/README.md)
5. [../config-repo/README.md](../config-repo/README.md)

## 남은 운영 과제

코드의 `@todo` 두 건이 운영 전 필수 결정 사항입니다.

- Config 조회 API를 private network와 mTLS 또는 서비스 인증으로 보호합니다.
- native 폴더를 승인된 Git backend/고정 label로 전환하고 refresh, rollback, 다중 노드 동기화를 검증합니다.

현재 Docker CLI가 없는 개발 환경에서는 이미지와 전체 Compose 실행을 별도로 검증해야 합니다.