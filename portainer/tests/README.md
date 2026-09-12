# Portainer offline contract tests

Prerequisites: Python 3.9+ and an already available PyYAML installation. From the repository root:

```sh
python3 -m unittest discover -s portainer/tests -v
```

Expected: 7 tests pass, no failures. The tests read only the Compose and Nginx source files. They do not resolve environment variables, read the administrator password file, contact Docker, or modify runtime resources. Repeated runs have no external side effects.

The deployment policy checks cover host-port isolation, the pinned CE image, persistent named data, mandatory external password-file configuration, safe bind mounts, resource/security constraints, and the shared external network. The proxy checks cover dynamic DNS, prefix stripping, port-preserving headers, the relative redirect, WebSocket settings, and suppression of both access and error logs inside the Portainer location. Upstream errors can include the original request URI and its WebSocket token even when access logging is disabled. In-memory unacceptable variants include removing error-log suppression, enabling file error logging, and adding a second stderr destination. Suppression trades scoped proxy diagnostics for preventing token exposure; verify missing-upstream behavior and synthetic-token absence in runtime logs separately.

The capability policy requires dropping `ALL` and adding exactly `DAC_READ_SEARCH`: rootful container UID 0 needs to read a host-user-owned `0600` bootstrap file. It rejects missing capability restrictions, a missing read capability, and broader `DAC_OVERRIDE`, `SYS_ADMIN`, or `ALL` grants. Verify foreign-owner file readability separately with synthetic data under the target runtime; these checks do not access actual administrator credentials.

The Nginx checks deliberately tokenize only the two simple Portainer location blocks and the upgrade map; they are not a general Nginx parser. If proxy configuration is restructured into includes, nested blocks, or equivalent alternative syntax, update the checks and rerun real Nginx validation. File-mode privacy, container startup, DNS recovery, browser rendering, authentication/CSRF, terminal WebSocket upgrades, and persistence after recreation require the separate runtime smoke described in the Portainer runbook; these static checks do not prove those behaviors.

## Opt-in live smoke

`live-smoke.cjs` requires Node.js 20+, Playwright with its Chromium browser already installed, and an existing dedicated Portainer test instance served at the fixed URL `http://localhost:8080/portainer/`. Its local socket environment must be running and must refer to the same container engine used to create the probe below. The commands assume the existing network is named `account-network`. The implementation was exercised with rootless Podman; use equivalent `docker` commands only when Portainer is connected to that Docker engine.

Use only the dedicated test instance's external private bootstrap password file, via `PORTAINER_ADMIN_PASSWORD_FILE`. Do not run this script with arbitrary existing Portainer administrator credentials. It authenticates as `admin`, restarts the probe, and opens a shell inside it. Before creating the fixture, confirm the name `account-713-probe` is unused in that engine. An existing container with that name is not permission to reuse or replace it.

Create the isolated probe with the exact Issue label. It must keep producing log messages: `follow=true` can stall with a fixture that has only historical output.

```sh
podman run -d --name account-713-probe \
  --label account.issue=713 --network account-network \
  --cpus 0.1 --memory 32m --pids-limit 32 --read-only \
  --cap-drop ALL --security-opt no-new-privileges \
  docker.io/library/alpine:3.20 \
  sh -c 'trap "exit 0" TERM INT; while :; do yes portainer-probe-ready | head -n 500; sleep 2; done'
```

From the repository root, substitute the absolute path of the private test password file. If Playwright is not resolvable as `playwright`, set `PLAYWRIGHT_MODULE` to its already installed absolute module path; no installation is performed by the smoke script.

```sh
export PORTAINER_ADMIN_PASSWORD_FILE=/absolute/path/to/test-admin-password
# Optional: export PLAYWRIGHT_MODULE=/absolute/path/to/playwright
node portainer/tests/live-smoke.cjs
```

Expected successful stages are:

```text
PASS redirect, status, unauthenticated denial, login, local environment
PASS streamed probe logs
PASS probe-only restart
PASS Chromium login and <count> JavaScript/CSS responses
PASS WebSocket HTTP 101 and interactive shell command output
```

The script reads status/environment metadata and only the exact probe's container inspection/logs. It checks `account.issue=713` before restarting that probe or creating its shell. The browser checks the rendered login/Home interface and JavaScript/CSS responses. The terminal check observes an actual HTTP 101 handshake and a shell-produced marker that cannot be satisfied by input echo alone. Password and JWT are kept in memory; the script does not print credentials, response bodies, token-bearing URLs, HAR files, or traces. Application failures print a fixed `FAIL live smoke at ...` stage and exit nonzero; missing Node/Playwright/browser dependencies can fail before stage reporting. Neither environmental failures nor partial PASS output count as a successful smoke run.

After the test, the parent/operator may clean up only the probe created above. Do not apply these commands to a pre-existing or unrelated container. They remove no volumes and do not stop Portainer, Nginx, or other services.

```sh
podman stop account-713-probe
podman rm account-713-probe
```

This smoke does not recreate the Portainer container or prove volume persistence, missing-backend startup/recovery, log-token suppression, or foreign-owner password-file readability. Run those separate checks from the runbook and retain their actual results independently.

The fixture deliberately emits bursts larger than the Portainer HTTP response buffer. Portainer 2.39.7 local Unix-socket proxy uses `io.Copy` without an explicit flush (`api/http/proxy/factory/docker.go` upstream); sparse `follow=true` output may be delayed even with Nginx buffering disabled. Historical log retrieval and browser polling remain available. This smoke verifies transport under ongoing output, not a latency guarantee for a quiet container.
