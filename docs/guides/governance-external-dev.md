# Governance development runtime (GH-696)

This isolated profile adds Budget (8096) and Internal Audit (8083) to the existing
`account-network`, PostgreSQL and `minimal-discovery` development services. APIs
use their own dev configuration, PostgreSQL driver, Hibernate validate and
release-owned migrations. Runtime Flyway, SQL initialization and Batch jobs do
not create schemas. Internal Audit retains the discovery ID
`INTERNAL-AUDIT-SERVICE`; Budget registers as `BUDGET-API`.

All four issue targets have 0.50 CPU / 768 MiB limits. API, Nginx (8080) and Grafana
(13001) host bindings are loopback only. The Internal Audit API trusts existing
Gateway identity headers; keep this development port private. Business mutation
and authorization redesign are outside this runtime verification.

## Build and prepare the two databases

Use JDK 17 and Gradle 8.7 with one worker. The verified container recipe mounts
only this isolated worktree and the existing Gradle cache:

```sh
podman run --name account-696-api-tests --cpus 1 --memory 1536m --pids-limit 512 \
  --user 0 -v "$PWD:/workspace" -v "$HOME/.gradle:/home/gradle/.gradle" \
  -w /workspace docker.io/library/gradle:8.7-jdk17-alpine \
  ./gradlew :budget:core:test :budget:api:test \
  :internal-audit:core:test :internal-audit:api:test \
  :budget:api:bootJar :internal-audit:api:bootJar \
  --no-daemon --max-workers=1 --console=plain
```

Build `:migration-runner:bootJar` from the same checkout/JDK before provisioning.
Use `postgres/runtime/provision-governance-external-dev.py` with the explicit
`--apply` flag only for the approved development cluster:

```sh
python3 -B postgres/runtime/provision-governance-external-dev.py \
  --env-file .env.governance-dev \
  --jar migration-runner/build/libs/account-migration-runner.jar
python3 -B postgres/runtime/provision-governance-external-dev.py \
  --env-file .env.governance-dev \
  --jar migration-runner/build/libs/account-migration-runner.jar --apply
```

The tool is fixed to `account-postgres:5432`, `budget_db`, `internal_audit_db`.
It creates a dedicated ignored mode-600 `.env.governance-dev`, generates distinct
owner/app credentials, sends them over stdin, applies current context migrations,
and validates runtime login, schema/table/sequence privileges and denied DDL.
Migration histories remain inaccessible to app roles. No existing database,
volume, password or business record is replaced. A second apply preserves saved
credentials and validates applied checksums before applying pending migrations.
A partial role/database creation or mismatched ownership stops for reviewed
manual forward recovery; do not delete databases or reset passwords to retry.

## Build images and start only these APIs

Use the installed Docker Compose client against the rootless Podman socket;
its multiple `--env-file` support merges both inputs. The following uses an already approved Auth env file for the Budget JWT key and
the newly generated database env. Never print Compose config or source either
file as shell code. Keep the shared Auth/Gateway key and issuer unchanged.

```sh
export DOCKER_HOST="unix://$XDG_RUNTIME_DIR/podman/podman.sock"
docker-compose --env-file /home/ho/dev/account/.env.external-dev \
  --env-file .env.governance-dev -f tools/compose.governance-external-dev.yml config >/dev/null
docker-compose --env-file /home/ho/dev/account/.env.external-dev \
  --env-file .env.governance-dev -f tools/compose.governance-external-dev.yml build budget-api
docker-compose --env-file /home/ho/dev/account/.env.external-dev \
  --env-file .env.governance-dev -f tools/compose.governance-external-dev.yml build internal-audit-api
docker-compose --env-file /home/ho/dev/account/.env.external-dev \
  --env-file .env.governance-dev -f tools/compose.governance-external-dev.yml up -d --no-build
```

The build uses `tools/Containerfile.minimal-auth-java` with the exact Gradle
project/JAR directory per API. For the GH-696 live verification,
the already-tested bootJars were assembled offline with that file's unchanged
runtime stage (only the builder-stage COPY was replaced with the staged JAR).
This avoids compiling the same source twice under host I/O pressure; the image
JAR checksum must match the tested JAR. The full Compose build above remains
the source-based reproduction path. The JVM retains the case-sensitive Eureka map key
through `-Deureka.client.serviceUrl.defaultZone=http://minimal-discovery:8761/eureka/`.
Budget also explicitly enables its module-specific discovery flags.

## Verify and recover

```sh
python3 -B -m unittest discover -s tools -p test_governance_runtime.py -v
python3 tools/verify-governance-runtime.py
curl --fail http://127.0.0.1:8096/actuator/health
curl --fail http://127.0.0.1:8083/actuator/health
podman exec account-frontend-nginx nginx -t
curl --fail http://127.0.0.1:8080/healthz
curl --fail http://127.0.0.1:8080/grafana/api/health
```

Require both health responses to contain `status: UP`, and both Eureka instances
to be `UP` with the correct ports. Verify actual container cgroup limits,
restart counts and OOM state, not merely Compose declarations. Check a runtime
PostgreSQL session for each app role. Do not include credentials, raw logs or
business responses in evidence.

[Grafana recovery](../../grafana/README.md) documents exact-volume preservation,
retained-image recreation, direct/proxy probes and datasource connectivity.
The dedicated Nginx template dynamically resolves the current container DNS and
routes `/api` through the `minimal-frontend` API boundary; Host retains the public port for
same-origin checks. `/healthz` proves Nginx only; `/login` and the frontend API validation
request must separately prove the upstream. Allow development frontend cold
compilation to complete before treating its initial timeout as proxy failure.

Rollback stops only the two new API containers and the dedicated proxy/Grafana
services. Keep generated env, the two databases, all volumes and retained images
for forward recovery. Recreate Nginx with the previously recorded image/mount
configuration and Grafana with its retained image plus the same external
`grafana-storage` volume. Never use volume removal, prune, whole-stack down, or
schema rollback. The primary checkout and unrelated containers remain owned by
their existing tasks.

## Authority separation

The user authorized implementation, related build/config fixes, development
migration/start/recreation, verification, branch push and Draft PR creation.
Implementers own changes; the separate Reviewer is read-only. The parent
Integrator owns shared harness records and GitHub mutations. Human review and
separate authorization remain the gates for Ready, merge, Issue close and
branch/worktree removal. The Draft PR uses `Refs #696`.

## GH-696 recovery observations

The initial Nginx and Grafana containers had no enforced CPU/memory limits.
Grafana was already running when inspected, but had no published port and used
the old Prometheus address. Recovery retained its exact image and external
volume, then applied the corrected datasource and loopback publication.

The existing minimal frontend did not answer even its in-container `/next.svg`
probe. SIGTERM/SIGKILL left stale container state and a `crun start` failure.
After confirming its old PIDs were gone, its configuration was cloned to retain
settings/volumes, and the stale container was replaced. Because Podman clone did
not retain the Compose healthcheck, the canonical existing minimal Compose was
then used with `--force-recreate --no-deps --no-build --pull never minimal-frontend`.
The original image, two named cache/dependency volumes and 0.75-CPU/1-GiB limits
were retained. These existing frontend limits are outside the four GH-696
0.50-CPU/768-MiB targets. Its source, credentials and other minimal services were
not modified. Nginx `/login` subsequently returned HTTP 200.

The deployed retained frontend image predates the current source BFF and uses
its `GATEWAY_INTERNAL_URL` Next.js rewrite. This issue preserves that image and
routes `/api` via the frontend, which also supports the current source BFF when
that separate frontend rollout occurs. The negative API request smoke proves
origin/input validation and proxy reachability; it does not claim a new HttpOnly
BFF deployment. Initial requests while services warmed up returned socket resets;
only final settled probe results are acceptance evidence.

## Final acceptance evidence (2026-09-11 KST)

| Check | Result |
| --- | --- |
| Budget core/API tests | 42 + 13 passed |
| Internal Audit core/API tests | 26 + 31 passed |
| Python policy/replay tests | 12 passed |
| Both bootJars/current migration runner | Built with JDK17; image JAR checksums match tested JARs |
| PostgreSQL migration, schema, runtime ACL and denied DDL | 2/2 passed; 2 app sessions per DB observed |
| API health / exact current-IP Eureka registration | 2/2 UP, ports 8096/8083 |
| Nginx health/login; Grafana direct/proxy | HTTP200 |
| Frontend API negative input/origin | HTTP400 / HTTP403 |
| Persisted Grafana default datasource / network query | Correct target / success with 6 results |
| Four issue containers | healthy, restart0, OOMfalse, CPU0.50, RAM805306368 |
| Independent read-only live gate | PASS, exit0 |

Commands and limitations above distinguish the tested-JAR image assembly from a
fresh full multi-stage source build and the retained frontend from a new BFF
rollout. Authenticated Grafana Save & test and unrelated business operations
were not executed. Keep the Draft PR and worktree for human review.
