#!/usr/bin/env python3
"""Sequential, value-suppressing live gates for the three Issue #653 packages."""
import argparse
import json
import os
from pathlib import Path
import re
import signal
import stat
import subprocess
import time

ROOT = Path(__file__).resolve().parent.parent
PACKAGES = {
    "accounting": {
        "journal-ledger-api": "JOURNAL-LEDGER-API",
        "closing-api": "CLOSING-SERVICE",
        "payable-api": "PAYABLE-API",
        "receivable-api": "RECEIVABLE-API",
        "expenditure-resolution-api": "EXPENDITURE-RESOLUTION-API",
        "tax-api": "TAX-API",
        "reporting-api": "REPORTING-API",
    },
    "products": {"deposit-api": "DEPOSIT-SERVICE", "loan-api": "LOAN-API",
                 "asset-lease-api": "ASSET-LEASE-SERVICE"},
    "risk": {"account-mart-api": "ACCOUNT-MART-API", "ecl-api": "IFRS9-ALLOWANCE-API",
             "reconciliation-api": "RECONCILIATION-API"},
}


class GateError(Exception):
    """Messages must contain only fixed labels, never subprocess output."""


def run(command, timeout=300, check=True):
    process = None
    try:
        process = subprocess.Popen(command, cwd=ROOT, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE, text=True, start_new_session=True,
                                   env={**os.environ, "COMPOSE_PARALLEL_LIMIT": "1"})
        stdout, stderr = process.communicate(timeout=timeout)
        result = subprocess.CompletedProcess(command, process.returncode, stdout, stderr)
    except (OSError, subprocess.TimeoutExpired):
        if process is not None:
            # Compose can leave child clients running after a parent-only timeout.
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.communicate()
        raise GateError("command unavailable or timed out; output suppressed") from None
    if check and result.returncode:
        raise GateError("command failed; output suppressed")
    return result


def identity(path):
    try:
        s = path.lstat()
    except OSError:
        raise GateError("approved env input unavailable") from None
    if not stat.S_ISREG(s.st_mode) or s.st_mode & 0o077:
        raise GateError("approved env must be a private regular non-symlink file")
    return s.st_dev, s.st_ino, s.st_size, s.st_mtime_ns, s.st_ctime_ns


def container(engine, package, service):
    result = run([engine, "ps", "-aq", "--filter",
                  f"label=com.docker.compose.project=account-{package}-external-dev",
                  "--filter", f"label=com.docker.compose.service={service}"])
    ids = result.stdout.split()
    if len(ids) != 1 or not re.fullmatch(r"[a-f0-9]{12,64}", ids[0]):
        raise GateError("expected exactly one package service container")
    return ids[0]


def inspect(engine, cid, template):
    return run([engine, "inspect", "--format", template, cid]).stdout.strip()


def state(engine, cid):
    # Never inspect Config.Env or print Healthcheck.Log (which can contain secrets).
    raw = inspect(engine, cid, '{{.State.Running}} {{.RestartCount}} {{.State.OOMKilled}}')
    return raw == "true 0 false"


def health(engine, cid):
    r = run([engine, "exec", cid, "wget", "-q", "-T", "5", "-O", "-",
             "http://127.0.0.1:8080/actuator/health"], check=False)
    try:
        return r.returncode == 0 and json.loads(r.stdout).get("status") == "UP"
    except (ValueError, AttributeError):
        return False


def registered(engine, cid, application):
    addresses = inspect(engine, cid,
                        '{{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}').split()
    r = run([engine, "exec", cid, "wget", "-q", "-T", "5", "-O", "-",
             "--header=Accept: application/json",
             f"http://minimal-discovery:8761/eureka/apps/{application}"], check=False)
    try:
        instances = json.loads(r.stdout)["application"]["instance"]
        if isinstance(instances, dict):
            instances = [instances]
        return r.returncode == 0 and any(
            i.get("status") == "UP" and i.get("ipAddr") in addresses
            and str(i.get("port", {}).get("$")) == "8080" for i in instances)
    except (ValueError, KeyError, TypeError, AttributeError):
        return False


def log_counts(engine, cid):
    output = run([engine, "logs", "--tail", "10000", cid])
    raw = output.stdout + output.stderr
    return {
        "error": len(re.findall(r"\bERROR\b", raw)),
        "startup_failure": len(re.findall(r"APPLICATION FAILED TO START", raw)),
        "database_failure": len(re.findall(
            r"(?i)SQLState:|SQLSTATE\[|password authentication failed|"
            r"unable to acquire JDBC|could not obtain.*connection|schema-validation:|"
            r"org\.postgresql\.util\.PSQLException|"
            r"HikariPool[^\n]*(?:Exception|Error|failed|timed out)", raw)),
    }


def limits(engine, cid):
    raw = inspect(engine, cid,
                  '{{.HostConfig.Memory}} {{.HostConfig.NanoCpus}} '
                  '{{.HostConfig.CpuQuota}} {{.HostConfig.CpuPeriod}}')
    memory, nano, quota, period = map(int, raw.split())
    cpu = nano / 1e9 if nano else (quota / period if period > 0 else 0)
    return memory == 768 * 1024 * 1024 and cpu == 0.5


def verify(engine, package, service, timeout=600):
    cid = container(engine, package, service)
    deadline = time.monotonic() + timeout
    while True:
        if not state(engine, cid):
            raise GateError(f"{service}: stopped, restarted or OOM; inspect with redaction")
        if health(engine, cid) and registered(engine, cid, PACKAGES[package][service]):
            break
        if time.monotonic() >= deadline:
            raise GateError(f"{service}: health/Eureka deadline exceeded")
        time.sleep(5)
    counts = log_counts(engine, cid)
    if not limits(engine, cid) or any(counts.values()):
        raise GateError(f"{service}: resource/log gate failed; counts={counts}")
    print(f"PASS {service}: health=UP eureka=UP restart=0 OOM=false CPU=0.50 RAM=768MiB logs={counts}",
          flush=True)


def memory_gate():
    # Leave 2 GiB headroom in addition to the next API's maximum allocation.
    try:
        raw = Path('/proc/meminfo').read_text()
        available = int(re.search(r'MemAvailable:\s+(\d+)', raw)[1])
    except (OSError, TypeError, ValueError):
        raise GateError("host memory availability could not be verified") from None
    if available < (2048 + 768) * 1024:
        raise GateError("host memory headroom gate failed")


def execute(args):
    if args.timeout <= 0:
        raise GateError("timeout must be positive")
    services = PACKAGES[args.package]
    if args.service:
        if args.service not in services:
            raise GateError("service does not belong to selected package")
        services = {args.service: services[args.service]}
    if args.action == "verify":
        for service in services:
            verify(args.engine, args.package, service, args.timeout)
        return
    if args.env_file is None:
        raise GateError("--env-file is required")
    path = args.env_file.absolute()
    expected = identity(path)
    base = [args.engine, "compose", "--project-name", f"account-{args.package}-external-dev",
            "--env-file", str(path), "-f", str(ROOT / "tools" /
            f"compose.{args.package}-external-dev.yml"), "--profile", "external-dev"]

    def compose(*command, timeout=300):
        if identity(path) != expected:
            raise GateError("env input identity changed")
        result = run([*base, *command], timeout=timeout)
        if identity(path) != expected:
            raise GateError("env input identity changed")
        return result

    compose("config", "--quiet")
    print(f"PASS {args.package}: quiet Compose preflight", flush=True)
    if args.action == "preflight":
        return
    if args.action == "up":
        memory_gate()
        compose("up", "-d", "--no-build", "--pull", "never", "--wait", "--wait-timeout",
                "180", f"{args.package}-db-check", timeout=240)
        print(f"PASS {args.package}: DB prerequisite", flush=True)
    for service in services:
        if args.action == "build":
            memory_gate()
            print(f"BUILD {service}: one Gradle worker", flush=True)
            compose("build", service, timeout=2400)
            print(f"PASS {service}: image build", flush=True)
        elif args.action == "up":
            memory_gate()
            print(f"START {service}", flush=True)
            compose("up", "-d", "--no-build", "--pull", "never", "--no-deps",
                    service, timeout=300)
            verify(args.engine, args.package, service, args.timeout)
    if args.action == "up" and not args.service:
        # The first API must still be healthy after the last API has started.
        for service in services:
            verify(args.engine, args.package, service, args.timeout)
        print(f"PASS {args.package}: package checkpoint ({len(services)} APIs)", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("preflight", "build", "up", "verify"))
    parser.add_argument("--package", choices=PACKAGES, required=True)
    parser.add_argument("--service")
    parser.add_argument("--env-file", type=Path)
    parser.add_argument("--engine", choices=("podman", "docker"), default="podman")
    parser.add_argument("--timeout", type=int, default=600)
    args = parser.parse_args()
    try:
        execute(args)
    except (GateError, ValueError):
        # Only GateError messages are safe; malformed engine responses stay suppressed.
        import sys
        error = sys.exc_info()[1]
        print(f"FAIL: {error if isinstance(error, GateError) else 'invalid engine response'}", flush=True)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
