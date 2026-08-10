# Container Image Contract

이 문서는 Issue `#228`의 Java API/Batch와 frontend image packaging 계약입니다. 실행 target의 단일 진실 원천은 `deploy/image-targets.json`입니다.

## Canonical Images

| Runtime | Build file | Build context | Artifact contract |
| --- | --- | --- | --- |
| Java API/Batch/infra | root `Containerfile` 또는 동일한 root `Dockerfile` | repository root | manifest의 정확한 Gradle `bootJar`와 `build/libs`에서 non-plain JAR 정확히 1개 |
| Frontend | `frontend/Containerfile` | `frontend` | `npm ci` + Next standalone output |

Java builder와 runtime은 모두 Java 17입니다. 이미지에는 environment profile, DB host, credential을 굽지 않습니다. root `.dockerignore`는 모든 하위 디렉터리의 `.env*`, ignored Spring `application-local.*`, secret 디렉터리와 private-key/keystore 형식을 build context에서 제외하며 공개 환경 예제 파일만 허용합니다. API port, healthcheck, `SPRING_PROFILES_ACTIVE`, datasource와 resource/security policy는 #66/#230 Compose가 주입합니다.

library/core/aggregator/phantom `:app`은 service image target이 아닙니다. Manifest는 Java 실행 target 35개와 Frontend 1개를 소유하며 모두 활성입니다. Internal Audit API는 실행 target에 포함되고, 실제 Job/Step이 없는 Internal Audit/Auth Batch는 target에서 제거되었습니다. 정책 테스트는 manifest만 신뢰하지 않고 실제 Gradle Boot API/Batch/infra 진입점과 활성 target을 양방향 비교하므로 새 실행 모듈이 조용히 빠질 수 없습니다.

## Verify Packages Without Building Images

외부 다운로드 없이 모든 enabled Java target의 실제 `bootJar`와 executable artifact 수를 확인합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\container-images.ps1 -Mode VerifyPackages -OutputPath C:\tmp\account-image-packages.json
```

모든 대상을 검사한 JSON 결과를 먼저 기록하며, enabled target 하나라도 실패하면 프로세스도 non-zero로 종료됩니다. 차단 상태로 명시된 target은 보고서에 `BLOCKED`로 남지만 검증 실패로 오인하지 않습니다.

일부 target만 확인할 수 있습니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\container-images.ps1 -Mode VerifyPackages -Target master-data-api,master-data-batch
```

Budget만 확인할 때는 다음 target을 사용합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\container-images.ps1 -Mode VerifyPackages -Target budget-api,budget-batch
```

## Build Local Images

Docker/Podman 사용과 dependency download가 승인된 환경에서만 `Build`를 실행합니다. 이 도구는 local image만 만들고 registry push는 하지 않습니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\container-images.ps1 -Mode Build -Engine docker -Target master-data-api
```

동등한 직접 명령:

```powershell
docker build --file Containerfile --build-arg GRADLE_PROJECT=:master-data:api --build-arg JAR_DIRECTORY=master-data/api --tag account/master-data-api:local .
docker build --file frontend/Containerfile --tag account/frontend:local frontend
```

`GRADLE_PROJECT`와 `JAR_DIRECTORY`는 Containerfile에서 허용 문자와 실제 build file을 검사합니다. Gradle 성공 후 non-plain executable JAR이 정확히 하나가 아니면 image build는 실패합니다.

## Frontend

`frontend/Containerfile`만 canonical production image definition입니다. `next.config.ts`의 `output: "standalone"`과 일치하게 standalone server, static files와 public assets만 runtime image로 복사하고 non-root `nextjs` user로 실행합니다. `NEXT_PUBLIC_API_URL`은 build argument이며 운영 기본은 same-origin `/api`입니다.

`frontend/docker-compose.yml`은 standalone production image를 그대로 실행하며 소스 bind mount로 runtime artifact를 가리지 않습니다. 루트 개발 Compose는 별도 `frontend/Containerfile.dev`, source bind mount, named dependency/cache volume과 `next dev`를 사용합니다. 운영은 계속 canonical `frontend/Containerfile` standalone image를 사용합니다.

기존 module Compose 중 PostgreSQL datasource를 직접 주입하는 Auth, Account Mart, ECL은 URL/user/password를 필수 환경변수로만 받습니다. 추적된 기본 자격증명은 없고 Auth의 Hibernate 정책은 `validate`입니다.

## Legacy Module Dockerfiles

기존 domain별 Dockerfile에는 aggregator build, Java 21, wildcard JAR copy가 섞여 있습니다. 신규 자동화와 현재 활성 module Compose는 이 파일을 image source로 사용하지 않고 canonical root Containerfile + manifest만 사용합니다. 루트 개발/운영 통합 Compose도 같은 Java 계약을 사용하며, 기존 module Dockerfile 성공은 보장하지 않습니다.

## Rollback And Remaining Gate

- Rollback은 root Containerfile/Dockerfile, manifest, verification tool, frontend image selection, docs/policy tests를 revert합니다.
- Registry image나 remote container는 자동 삭제하지 않습니다.
- Docker/Compose가 없는 호스트에서는 actual image build가 미검증입니다.
- Image가 생성돼도 application context/DB migration/health는 별도 runtime/Compose gate입니다.
