# Watchtower development experiment

Use the [deployment roadmap](../../docs/guides/container-management-and-deployment-roadmap.md)
for prerequisites, rollout and rollback. Upstream `containrrr/watchtower` was archived
on 2025-12-17. This pinned 1.7.1 template is an opt-in feasibility prototype with
monitor-only enabled by default; it is not a production release controller.

Only running containers with **both** `com.centurylinklabs.watchtower.enable=true`
and `com.centurylinklabs.watchtower.scope=account-dev` are selected. No existing
service receives these labels here. The updater itself is excluded. Do not run
alongside an unscoped Watchtower: it can stop scoped updater instances.

The Unix socket grants engine administrator authority even with a read-only bind.
There is no TCP socket, webhook or published updater port. Same-engine rootless
Podman is experimental; Docker API 1.44 is explicit to avoid the old client's 1.24
default. Validate engine compatibility before enabling updates. Compose restart
does not guarantee recovery after a rootless host reboot; configure and verify
the engine's user service separately.

```bash
export WATCHTOWER_SOCKET_PATH=/var/run/docker.sock
test -S "$WATCHTOWER_SOCKET_PATH"
docker compose -f deploy/watchtower/compose.yml --profile watchtower config --quiet
docker compose -f deploy/watchtower/compose.yml --profile watchtower up -d
# Explicitly authorize recreation only after successful monitor/smoke review:
docker compose -f deploy/watchtower/compose.yml -f deploy/watchtower/compose.update.yml \
  --profile watchtower up -d
# Disable updates first during an incident (stop does not depend on the mode).
docker compose -f deploy/watchtower/compose.yml --profile watchtower stop watchtower
```

Version 1.7.1 rejects `--rolling-restart` with global monitor-only. The default
file therefore monitors without rolling; the explicit update override disables
monitor-only and adds rolling restart together. An environment variable alone
cannot enable updates. To return to monitoring, run `up -d` with only the base file.

Tests use a private local registry and synthetic containers, never business services:

```bash
python3 -m unittest discover -s deploy/watchtower/tests -p 'test_*.py' -v
python3 deploy/watchtower/tests/live-smoke.py --engine docker
# Same-user Podman socket must already be active:
python3 deploy/watchtower/tests/live-smoke.py --engine podman \
  --socket "/run/user/$(id -u)/podman/podman.sock"
```

The smoke pulls versioned Watchtower, registry and BusyBox images, builds two tiny
fixtures, pushes only to its random loopback registry, verifies monitor-only,
updates, excluded labels/scope/stopped containers, repeat-run and rollback. It
removes only its randomly named containers and temporary local fixture tags;
downloaded base images remain cached. Its temporary registry has no host volume.
The CI workflow runs the same smoke on an ephemeral Docker runner with read-only
GitHub permissions, without registry credentials or external deployment rights.

Watchtower 1.7.1's `Updated` counter includes stale images in monitor-only mode;
pull errors are skipped and omitted from both `Scanned` and `Failed`. Neither exit
zero nor `Failed=0` proves successful deployment. The smoke checks image IDs,
container IDs, version contents and explicit failure logs as well as counters.
