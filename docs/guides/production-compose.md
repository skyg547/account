# Production Compose Runbook

Issue `#230`은 운영 환경을 source build 없이 immutable image와 외부 PostgreSQL로 실행하는 계약을 정의합니다. 이 문서의 명령은 배포 승인과 검증된 image digest가 있는 운영 호스트에서만 실행합니다.

## Guarantees

- `compose.prod.yml`에는 `build:`가 없으며 모든 image는 필수 `@sha256` digest 변수입니다.
- PostgreSQL, Kafka, Redis를 이 Compose가 생성하거나 host port로 공개하지 않습니다.
- 도메인 API/Batch는 각각 TLS 검증이 강제된 외부 PostgreSQL URL과 최소 권한 runtime user/password를 필수로 받습니다.
- host에 공개되는 application port는 Gateway와 Frontend뿐입니다.
- Java/Frontend service는 read-only filesystem, dropped capabilities, `no-new-privileges`, PID/resource/log limits를 사용합니다.
- API는 `prod`, Batch는 `batch` profile로 분리됩니다. Batch job은 기본적으로 자동 실행되지 않습니다.
- Internal Audit API/Batch는 실행형 packaging이 복구될 때까지 #73, #74, #231에 의해 제외됩니다.

Gateway TLS termination, firewall, registry authentication, secret-manager injection과 host hardening은 이 저장소 밖의 배포 플랫폼 책임입니다. PostgreSQL JDBC URL은 `sslmode=verify-full`을 사용해야 합니다. Gateway/Frontend port를 공용 인터넷에 직접 노출하지 말고 승인된 reverse proxy 또는 load balancer 뒤에 둡니다.

## Prepare An Environment File

예제를 실제 값 파일로 복사합니다. `.env.prod`는 Git ignore 대상입니다.

```powershell
Copy-Item .env.prod.example .env.prod
```

다음을 배포 시스템의 승인된 값으로 채웁니다.

- 36개 image reference: release registry의 immutable `@sha256:<64 hex>` digest
- `PROD_CONFIG_REPO_PATH`: 배포된 Config repository의 절대 경로
- Kafka/Redis endpoint
- Internal Audit과 Budget을 포함한 17개 bounded context별 PostgreSQL JDBC URL, 최소 권한 runtime user, password
- Auth, Gateway, Budget API가 함께 사용하는 32자 이상의 `AUTH_JWT_SECRET`
- 운영 bootstrap용 16자 이상의 `AUTH_DEFAULT_PASSWORD`와 32자 이상의 `AUTH_INTERNAL_API_TOKEN`
- Frontend BFF와 Gateway만 공유하는 별도 32~512-byte `BFF_GATEWAY_SHARED_SECRET`
- 필요하면 Gateway/Frontend host port

예제의 빈 password와 zero digest는 운영 검증에서 거부됩니다. BFF 공유 키는 JWT나 내부 API
token과 별도 생성하고 Frontend/Gateway에 동일하게 주입합니다. 자격증명은 저장소, PR, Issue,
shell history에 넣지 않습니다. 실제 배포에서는 secret manager가 보호된 임시 env 파일을
생성하게 하고, host 접근권한과 보존 정책을 배포 플랫폼에서 통제합니다.

## Validate Without Printing Secrets

예제의 키 구조와 digest 형식만 확인할 수 있습니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\validate-prod-env.ps1 -EnvFile .\.env.prod.example -Template
```

실제 값 파일은 더 엄격하게 검사합니다. 검증기는 값 자체를 출력하지 않으며, 필수 변수가 process environment에도 존재하면 Compose의 shell-over-file 우선순위 우회를 막기 위해 실패합니다. `$` interpolation과 backslash escape가 필요한 값은 이 env-file 계약에서 거부되므로 secret generator는 해당 문자를 사용하지 않아야 합니다. 같은 깨끗한 shell에서 검증과 Compose 명령을 연속 실행합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\validate-prod-env.ps1 -EnvFile .\.env.prod
```

Docker Compose가 있는 승인된 호스트에서는 image pull이나 service start 전에 최종 model을 검사합니다.

```powershell
docker compose --env-file .env.prod -f compose.prod.yml --profile prod config --quiet
docker compose --env-file .env.prod -f compose.prod.yml --profile batch config --quiet
```

렌더링된 전체 config는 환경변수 값을 포함할 수 있으므로 CI artifact나 채팅에 출력하지 않습니다. `--quiet`만 사용합니다.

## Start API Runtime

Release 절차가 digest image를 미리 확보한 뒤 runtime에서 source build와 예상치 못한 pull을 금지합니다. 배포 호스트의 Compose는 `up --wait --wait-timeout`을 지원해야 하며, 지원하지 않는 구버전에서는 배포하지 않습니다.

```powershell
docker compose --env-file .env.prod -f compose.prod.yml --profile prod up -d --wait --wait-timeout 300 --no-build --pull never
docker compose --env-file .env.prod -f compose.prod.yml --profile prod ps
```

`--wait`가 prod profile의 21개 서비스가 running/healthy가 되지 못하면 명령을 실패시킵니다. Config Server readiness 뒤에 Discovery가 시작되고, Gateway와 도메인 API는 두 플랫폼 서비스가 healthy가 된 뒤 병렬로 시작합니다. Frontend만 Gateway readiness를 기다립니다. Gateway가 모든 API를 `depends_on`으로 묶어 한 업무의 장애가 전체 ingress를 막게 하지 않습니다. `--wait` 통과 뒤에도 필수 Gateway route smoke가 성공하기 전에는 traffic을 전환하지 않습니다. Database migration과 PostgreSQL 권한이 유효하지 않으면 `ddl-auto`로 우회하지 않고 배포를 실패시킵니다. #243의 PostgreSQL driver/Actuator classpath와 #244의 PostgreSQL migration gate가 완료되기 전에는 이 stack을 운영 배포하지 않습니다.

## Run A Batch Explicitly

Batch는 기본 `SPRING_BATCH_JOB_ENABLED=false`이며 API stack과 별도 profile입니다. 검증된 Job 이름과 재실행 파라미터를 명시해 한 서비스만 실행합니다.

```powershell
docker compose --env-file .env.prod -f compose.prod.yml --profile batch run --rm `
  -e SPRING_BATCH_JOB_ENABLED=true `
  -e SPRING_BATCH_JOB_NAME=<approved-job-name> `
  closing-batch --spring.batch.job.name=<approved-job-name> run.id=<approved-run-id>
```

Job별 필수 business date, 멱등 key, restart 지점과 결과 검증은 해당 Batch Issue의 승인 gate입니다.

## Database Policy

`config-repo/application-prod.yml`과 Compose는 다음을 강제합니다.

- PostgreSQL driver/dialect와 필수 URL/user/password
- long-running API/Batch의 Flyway disabled와 clean disabled
- JPA `ddl-auto=validate`, Open Session in View disabled
- SQL init와 Spring Batch 자동 schema 생성 disabled
- Kafka/Redis endpoint 필수

Migration은 #244에서 제공하는 별도 release artifact/job으로만 수행하며 application runtime credential과 분리된 migrator role을 사용합니다. 승인된 release 단계는 먼저 `validate`, 그 다음 명시적으로 승인된 `migrate`, 마지막으로 다시 `validate`를 수행해야 합니다. Spring Batch metadata도 같은 release gate에서 사전 provision합니다. long-running service의 runtime role에는 schema 변경 권한을 부여하지 않습니다. 적용된 migration을 삭제하거나 운영 DB를 H2로 대체하지 않습니다.

## Rollback

Application rollback은 DB migration이 이전 application schema와 backward-compatible하고 이전 digest가 host에 미리 확보된 경우에만 허용합니다. 이 조건을 만족하면 직전 승인 digest로 `.env.prod` image reference를 되돌린 뒤 같은 `--no-build --pull never` 절차로 재기동합니다. migration이 rename/drop/type 축소처럼 이전 mapping과 호환되지 않으면 application도 rollback하지 않고 forward-fix합니다. Schema 변경은 expand/contract 순서를 사용해야 합니다. Compose 종료 시 외부 PostgreSQL, Kafka, Redis에는 삭제 명령을 보내지 않습니다.

```powershell
docker compose --env-file .env.prod -f compose.prod.yml --profile prod down
```

Database rollback은 자동화하지 않습니다. 이미 적용된 migration은 별도 review를 거친 forward-fix를 사용합니다.

## Current Verification Boundary

이 작업 호스트에는 Docker CLI와 Compose provider가 없고 Podman에도 필요한 image가 없습니다. 따라서 YAML/policy test, template validator, secret/default scan까지만 실행했습니다. 실제 Compose render, digest pull, non-root runtime, health dependency, PostgreSQL migration 및 rollback rehearsal은 승인된 배포 환경의 잔여 gate입니다. #243과 #244는 이 문서의 production deployment 선행 조건입니다.
