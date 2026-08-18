# Claude 검수 워크로그

## 2026-08-19 — 개발 컨테이너 기동 점검 및 프로파일링 검수

### 검수 대상
개발 서버 컨테이너 11개 (podman rootless, `account-network`)

### 수행한 명령과 결과

| 단계 | 명령 | 결과 |
| --- | --- | --- |
| 상태 확인 | `podman ps -a` | 11개 전부 `Created` (미실행) 상태 발견 |
| Phase 1 | `podman start account-postgres account-redis zipkin logstash account-pgadmin` | 정상, postgres `pg_isready` 4초 |
| Phase 2 | `podman start config-server discovery` | config-server health 200 (14초), discovery health 200 |
| Phase 3 | `podman start auth master-data` | auth 200 / **master-data 503 (DOWN)** |
| Phase 4 | `podman start gateway account-frontend` | gateway 200, frontend 200 |

최종 엔드포인트: config-server(8888) UP, discovery(8761) UP, auth(8084) UP,
gateway(8000) UP, frontend(3000) 200, zipkin(9411) healthy, pgadmin(5050) 302,
**master-data(8082) DOWN**.

### 발견된 위반/문제 항목

- **[심각도: 높음] `config-repo/master-data.yml:5-7` — 개발 컨테이너가 H2 인메모리 DB 사용**
  프로파일과 무관하게 `jdbc:h2:mem:masterdb`가 하드코딩되어 있다.
  `SPRING_PROFILES_ACTIVE=docker`인데 `config-repo`에 `application-docker.yml`이 없어
  PostgreSQL 설정이 담긴 `application-dev.yml`이 전혀 적용되지 않는다.
  컨테이너 재시작 시 기준정보가 전부 소실된다.
  → 프로파일을 `dev`로 정렬하거나 `application-docker.yml`을 추가해 datasource를 PostgreSQL로 전환.

- **[심각도: 높음] `master-data/docker-compose.yml:14-27` — Redis를 `localhost:6379`로 접속하여 health DOWN**
  위와 동일 원인. `config-repo/application-dev.yml:30-32`의 `host: ${REDIS_HOST:redis}`가
  미적용되어 Spring 기본값 `localhost`로 떨어진다. 컨테이너 내부에서 `redis` DNS는
  정상 해석(10.89.4.3)되므로 네트워크 문제는 아니다.
  → `SPRING_DATA_REDIS_HOST=redis` 추가 또는 프로파일 정렬.

- **[심각도: 중간] 서비스별 Spring 프로파일 불일치**
  auth=`postgres`, master-data=`docker`, discovery=`docker`, config-server=`native`.
  루트 `docker-compose.yml:12`는 `SPRING_PROFILES_ACTIVE: dev`로 통일되어 있으나
  실제 기동에 쓰이는 모듈별 compose는 아무도 `dev`를 쓰지 않는다.
  `docs/guides/development-compose.md`의 "컨테이너 환경은 `dev` 프로파일 + PostgreSQL"
  계약과 실제 런타임이 어긋난다.

- **[심각도: 중간] Eureka 등록 서비스가 MASTER-DATA 하나뿐**
  auth는 `auth/docker-compose.yml:26`에서 `EUREKA_CLIENT_ENABLED=false`로 명시 비활성.
  gateway는 `EUREKA_DEFAULT_ZONE`이 설정되어 있으나 registry에 미등록이며,
  `/api/master-data/actuator/health` 라우팅이 404를 반환한다.

- **[심각도: 낮음] frontend가 백엔드를 `127.0.0.1:8000`으로 호출**
  로그에 `ECONNREFUSED 127.0.0.1:8000`. 커밋 `6fcc1126`의 backend-network overlay
  (`frontend/compose.dev.backend.yml`)를 적용하지 않고 기동한 것으로 보인다.
  페이지 자체는 200이라 SSR fetch 경로만 실패.

- **[심각도: 낮음] `podman logs gateway`가 무한 대기**
  journald 드라이버 사용 컨테이너 중 gateway만 로그 조회가 타임아웃된다.
  다른 컨테이너는 동일 드라이버로 정상. 운영 관측성 저해.

### 문서화 검수

기동 절차 문서는 **존재한다**.

| 문서 | 내용 |
| --- | --- |
| `docs/guides/development-compose.md` | 루트 compose 기준 상세 절차 — profile 표, self-contained/external-dev, migration/grant, batch, 종료·롤백 |
| `docs/guides/infrastructure_runbook.md` | Phase 1~4 기동 순서, 대시보드 목록, 트러블슈팅 |
| `docs/guides/local-development.md` §7 | 인프라 실행 순서와 `.env.dev` 검증 게이트 |
| `README.md` 234·250·253·265행 | 위 문서로의 진입점 |

**갭:**

1. 실제 운영 중인 방식(모듈별 `<module>/docker-compose.yml` 개별 기동)의 순서·의존성이
   어느 문서에도 없다. `infrastructure_runbook.md:83`은 미들웨어에만 "각 폴더로 이동하여
   `docker-compose up -d`"라 하고, config-server/discovery/auth/gateway/master-data는
   Gradle `bootRun` 경로로만 설명한다.
2. 모든 절차가 PowerShell/Windows + `docker compose` 전제다. 현재 개발 머신은
   Linux + podman/podman-compose이며 리눅스 실행 경로가 없다. `validate-dev-env.ps1`도
   `.ps1`만 제공된다.
3. `.env.dev`가 없다(`.env.dev.example`만 존재). 문서가 규정한 루트 compose 경로는
   현 상태로는 실행 불가다.

### 남은 리스크

- master-data가 H2로 기동되는 한 개발 환경의 기준정보 영속성이 없다. 다른 서비스가
  master-data를 참조하는 통합 시나리오는 신뢰할 수 없다.
- 문서화된 경로(루트 compose + `dev` 프로파일 + PostgreSQL)와 실제 경로(모듈별 compose +
  `docker`/`postgres` 프로파일 + 혼재 DB)가 이원화되어 있어, 문서를 따라도 현재 환경이
  재현되지 않는다. 둘 중 하나를 정본으로 정하고 나머지를 폐기하거나 문서화해야 한다.
