# Config Repo (중앙 설정 저장소)

`config-repo`는 Config Server가 읽는 로컬/native 설정 원본입니다. 단독 Spring Boot 서버나 데이터베이스가 아니며, `config-server`가 HTTP 설정 조회로 전달합니다.

## 초보자를 위한 설명

서비스가 많아지면 포트, Discovery 주소, Gateway route 같은 설정을 각 모듈에서 따로 맞추기 어렵습니다. `config-repo`는 프랜차이즈 본사의 승인된 레시피 서랍이고, Config Server는 요청받은 레시피를 찾아 주는 창구입니다.

## 데이터 흐름

```text
서비스 spring.application.name/profile
-> Config Server /{application}/{profile}
-> EnvironmentRepository
-> config-repo/{application}.yml
-> ordered propertySources
-> 서비스 Environment 바인딩
```

서비스 로컬 설정이 무조건 무시되는 것은 아닙니다. 원격 Config, 환경변수, 명령행 인자, 로컬 파일이 Spring 우선순위에 따라 합쳐집니다.

## 현재 파일

| 파일 | 대상 |
| --- | --- |
| `auth-service.yml` | Auth |
| `discovery-service.yml` | Discovery |
| `gateway-service.yml` | Gateway |
| `internal-audit-service.yml` | Internal Audit API |
| `journal-ledger.yml` | Journal Ledger |
| `master-data.yml` | Master Data |

파일명은 대상 서비스의 `spring.application.name`과 일치해야 합니다.

## 변경 관리

파일을 저장했다고 실행 중인 모든 서비스 값이 즉시 바뀌는 것은 아닙니다. Config Server의 다음 조회에는 반영될 수 있지만, 클라이언트는 보통 재시작 또는 검증된 refresh/Bus 절차가 필요합니다.

```text
변경 요청 -> 리뷰/승인 -> 테스트 -> 제한 배포 -> 전체 배포 -> rollback 가능 상태 유지
```

운영 비밀번호, JWT secret, 내부 token, 개인정보, 운영 URL은 평문으로 확정하거나 문서/로그에 출력하지 않습니다. 환경변수나 승인된 secret 관리 체계로 분리합니다.

`dev` profile의 PostgreSQL database/owner, self-contained Compose와 공유 개발 DB 주입 계약은 [개발 PostgreSQL 가이드](../docs/development-postgresql.md)를 따릅니다. `application-dev.yml`은 datasource 환경변수가 없을 때 H2로 fallback하지 않고 시작을 실패시킵니다.

## Docker와 로컬

설정 원본은 Config Server 이미지 레이어에 포함하지 않습니다. Compose에서만 이 폴더를 `/config-repo`에 읽기 전용으로 마운트합니다.

```text
root compose:   ./config-repo:/config-repo:ro
module compose: ../config-repo:/config-repo:ro
```

Config Server를 저장소 루트에서 실행한 뒤 상태 코드만 확인합니다.

```powershell
(Invoke-WebRequest http://localhost:8888/actuator/health/readiness).StatusCode
(Invoke-WebRequest http://localhost:8888/master-data/default).StatusCode
```

설정 응답 본문은 공유 출력에 남기지 않습니다. 자세한 실행법은 [../config-server/docs/local-run.md](../config-server/docs/local-run.md)를 봅니다.
