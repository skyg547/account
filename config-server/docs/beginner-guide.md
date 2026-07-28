# Config Server beginner guide

## 한 문장 요약

`config-server`는 여러 서비스가 사용할 설정을 한 곳에서 찾아 나눠 주는 중앙 설정 서버입니다.

초보자 관점에서는 프랜차이즈 본사의 레시피 창구입니다. 지점 서비스는 자기 이름과 환경을 알려 주고 그 조건에 맞는 레시피를 받습니다.

## 등장인물

- `config-server`: 레시피를 찾아 전달하는 본사 창구.
- `config-repo`: 승인된 레시피 원본이 있는 서랍.
- Config Client: 레시피를 요청하는 각 지점 서비스.
- `application`: 지점 이름. 보통 `spring.application.name`입니다.
- `profile`: `local`, `docker`, `prod`처럼 실행 환경을 구분하는 이름입니다.
- `label`: Git backend에서 특정 branch/tag/commit 계열을 고르는 값입니다. 현재 native 로컬 모드에서는 주로 사용하지 않습니다.

## 요청 URL 읽는 법

```text
GET /{application}/{profile}
GET /{application}/{profile}/{label}
```

예를 들어 `GET /master-data/default`는 `master-data` 서비스의 기본 profile 설정을 요청합니다. Config Server는 응답을 `propertySources` 목록으로 반환하며, 앞쪽 source가 더 높은 우선순위를 가집니다.

## 로컬 설정은 무시되는가

무조건 무시되지 않습니다. Spring Boot는 환경변수, 명령행 인자, 원격 Config, 로컬 `application.yml` 등을 정해진 우선순위로 합칩니다. 같은 키가 여러 곳에 있으면 우선순위가 높은 값이 최종값이 됩니다.

따라서 문제를 조사할 때는 다음을 같이 봅니다.

1. 서비스의 `spring.application.name`
2. 활성 profile
3. Config Server 응답의 property source 순서
4. 환경변수와 명령행 인자
5. 서비스 로컬 기본 설정

## 설정 파일을 바꾸면 즉시 모든 서비스가 바뀌는가

아닙니다. native Config Server는 다음 조회에서 변경된 파일을 읽을 수 있지만, 이미 실행 중인 클라이언트는 보통 시작 시 값을 바인딩한 상태입니다. 클라이언트 재시작 또는 검증된 refresh/Bus 전략이 있어야 런타임 값이 바뀝니다.

회계 시스템에서는 설정을 즉시 퍼뜨리는 것보다 아래 순서가 중요합니다.

```text
변경 요청 -> 사람 리뷰/승인 -> 버전 고정 -> 검증 -> 제한 배포 -> 전체 배포 -> 문제 시 rollback
```

## readiness가 필요한 이유

프로세스가 8888 포트를 열었다고 해서 레시피를 읽을 수 있다는 뜻은 아닙니다. 이 프로젝트는 `master-data/default` 대표 설정에서 property source가 하나 이상 조회되어야 readiness를 UP으로 판단합니다.

- 정상 조회: 뒤쪽 서비스 시작 가능.
- 빈 결과: DOWN.
- 저장소 조회 예외: DOWN.
- Config Server DOWN: Compose 의존 서비스는 `service_healthy`를 기다림.

## 보안 주의

Config 응답에는 내부 주소나 민감 설정이 포함될 수 있습니다. 현재 로컬 native 모드는 개발 편의를 위한 것이며 인터넷에 직접 노출하면 안 됩니다. 운영 전 private network와 mTLS/서비스 인증, Git 승인/감사/rollback 정책을 적용해야 합니다.

H2나 PostgreSQL은 필요하지 않습니다. 이 모듈은 데이터베이스 대신 설정 저장소를 읽습니다.