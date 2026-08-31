#!/usr/bin/env python3
"""Operate the bounded Issue #520 external-dev authentication stack."""

from __future__ import annotations

import argparse
import os
import shutil
import stat
import subprocess
import sys
import time
from pathlib import Path


PROJECT_NAME = "account-minimal-auth-external-dev"
PROJECT_LABEL = f"com.docker.compose.project={PROJECT_NAME}"
BUILD_SERVICES = (
    "minimal-config-server",
    "minimal-discovery",
    "minimal-auth",
    "minimal-master-data",
    "minimal-gateway",
    "minimal-frontend",
)
ENV_ACTIONS = frozenset(("preflight", "build", "up", "smoke", "all"))
LEGACY_TARGET_CONTAINERS = frozenset(
    ("config-server", "discovery", "auth", "master-data", "gateway", "account-frontend")
)
LEGACY_TARGET_SERVICES = frozenset(
    (
        "config-server",
        "discovery",
        "auth",
        "auth-api",
        "master-data",
        "master-data-api",
        "gateway",
        "frontend",
        "frontend-dev",
    )
)
LOGIN_PATH_ATTEMPTS = 30
LOGIN_PATH_DELAY_SECONDS = 5


class RunnerError(Exception):
    pass


def repository_root() -> Path:
    return Path(__file__).resolve().parent.parent


def detect_engine(requested: str | None) -> str:
    candidates = (requested,) if requested else ("podman", "docker")
    for candidate in candidates:
        if candidate and shutil.which(candidate):
            return candidate
    raise RunnerError("neither podman nor docker is available")


def env_file_identity(env_file: Path) -> tuple[int, int, int, int, int]:
    try:
        metadata = env_file.lstat()
    except OSError:
        raise RunnerError("approved env file could not be inspected") from None
    if stat.S_ISLNK(metadata.st_mode) or not stat.S_ISREG(metadata.st_mode):
        raise RunnerError("approved env file must be a regular non-symlink file")
    return (
        metadata.st_dev,
        metadata.st_ino,
        metadata.st_size,
        metadata.st_mtime_ns,
        metadata.st_ctime_ns,
    )


def ensure_env_file_identity(
        env_file: Path, expected: tuple[int, int, int, int, int]) -> None:
    if env_file_identity(env_file) != expected:
        raise RunnerError("approved env file identity changed during execution")


def run(
        command: list[str],
        *,
        env: dict[str, str] | None = None,
        timeout_seconds: int | None = None) -> None:
    try:
        completed = subprocess.run(
            command,
            cwd=repository_root(),
            env=env,
            check=False,
            timeout=timeout_seconds,
        )
    except subprocess.TimeoutExpired:
        raise RunnerError("command timed out") from None
    except OSError:
        raise RunnerError("command could not be started") from None
    if completed.returncode != 0:
        raise RunnerError(f"command failed with exit code {completed.returncode}")


def compose_command(engine: str, env_file: Path) -> list[str]:
    return [
        engine,
        "compose",
        "--project-name",
        PROJECT_NAME,
        "--env-file",
        str(env_file),
        "-f",
        str(repository_root() / "tools" / "compose.minimal-auth-external-dev.yml"),
        "--profile",
        "external-dev",
    ]


def validate_env(env_file: Path) -> None:
    run(
        [
            sys.executable,
            str(repository_root() / "tools" / "validate-minimal-auth-env.py"),
            "--env-file",
            str(env_file),
        ]
    )


def preflight(
        engine: str, env_file: Path) -> tuple[list[str], tuple[int, int, int, int, int]]:
    identity = env_file_identity(env_file)
    validate_env(env_file)
    ensure_env_file_identity(env_file, identity)
    base = compose_command(engine, env_file)
    run([engine, "compose", "version"])
    ensure_env_file_identity(env_file, identity)
    run([*base, "config", "--quiet"])
    ensure_env_file_identity(env_file, identity)
    print("PASS: minimal external-dev compose preflight")
    return base, identity


def build(engine: str, env_file: Path) -> None:
    base, identity = preflight(engine, env_file)
    build_env = os.environ.copy()
    build_env["COMPOSE_PARALLEL_LIMIT"] = "1"
    for service in BUILD_SERVICES:
        print(f"BUILD: {service}")
        ensure_env_file_identity(env_file, identity)
        run([*base, "build", service], env=build_env)
        ensure_env_file_identity(env_file, identity)


def up(engine: str, env_file: Path) -> None:
    base, identity = preflight(engine, env_file)
    ensure_legacy_targets_are_stopped(engine)
    try:
        ensure_env_file_identity(env_file, identity)
        run([*base, "up", "-d", "--no-build", "--wait", "--wait-timeout", "360"])
        ensure_env_file_identity(env_file, identity)
    except RunnerError as error:
        try:
            stop(engine)
        except RunnerError as stop_error:
            raise RunnerError(
                "compose up failed and project-scoped stop also failed"
            ) from stop_error
        raise RunnerError("compose up failed; project-scoped stop completed") from error


def compose_exec(
        base: list[str],
        env_file: Path,
        identity: tuple[int, int, int, int, int],
        service: str,
        command: list[str]) -> None:
    ensure_env_file_identity(env_file, identity)
    run([*base, "exec", "-T", service, *command], timeout_seconds=15)
    ensure_env_file_identity(env_file, identity)


def smoke(engine: str, env_file: Path) -> None:
    base, identity = preflight(engine, env_file)
    probes = (
        ("minimal-config-server", "http://127.0.0.1:8888/actuator/health/readiness"),
        ("minimal-discovery", "http://127.0.0.1:8761/actuator/health/readiness"),
        ("minimal-auth", "http://127.0.0.1:8084/actuator/health/readiness"),
        ("minimal-master-data", "http://127.0.0.1:8082/actuator/health/readiness"),
        ("minimal-gateway", "http://127.0.0.1:8000/actuator/health/readiness"),
        ("minimal-frontend", "http://127.0.0.1:3000/next.svg"),
    )
    for service, url in probes:
        compose_exec(
            base,
            env_file,
            identity,
            service,
            ["wget", "-q", "-T", "4", "-t", "1", "-O", "/dev/null", url],
        )
        print(f"PASS: {service} readiness")

    frontend_path_probe = (
        "fetch('http://127.0.0.1:3000/api/auth/login',"
        "{method:'POST',headers:{'content-type':'application/json',"
        "'origin':'http://127.0.0.1:3000','sec-fetch-site':'same-origin'},body:'{}',"
        "signal:AbortSignal.timeout(4000)})"
        ".then(response=>{if(response.status!==400)process.exit(1)})"
        ".catch(()=>process.exit(1))"
    )
    for attempt in range(1, LOGIN_PATH_ATTEMPTS + 1):
        try:
            compose_exec(
                base,
                env_file,
                identity,
                "minimal-frontend",
                ["node", "-e", frontend_path_probe],
            )
            print("PASS: frontend -> gateway -> auth validation path returned HTTP 400")
            break
        except RunnerError:
            if attempt == LOGIN_PATH_ATTEMPTS:
                raise RunnerError(
                    "frontend -> gateway -> auth path did not return HTTP 400 "
                    f"after {LOGIN_PATH_ATTEMPTS} attempts"
                ) from None
            print(
                "WAIT: frontend -> gateway -> auth registration "
                f"attempt {attempt}/{LOGIN_PATH_ATTEMPTS}"
            )
            time.sleep(LOGIN_PATH_DELAY_SECONDS)


def project_container_ids(engine: str, include_stopped: bool) -> list[str]:
    command = [engine, "ps"]
    if include_stopped:
        command.append("-a")
    command.extend(("-q", "--filter", f"label={PROJECT_LABEL}"))
    completed = subprocess.run(
        command,
        cwd=repository_root(),
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        raise RunnerError("could not identify project containers")
    return [line for line in completed.stdout.splitlines() if line]


def ensure_legacy_targets_are_stopped(engine: str) -> None:
    completed = subprocess.run(
        [engine, "ps", "-q"],
        cwd=repository_root(),
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        raise RunnerError("could not inspect existing target containers")
    container_ids = [line for line in completed.stdout.splitlines() if line]
    if not container_ids:
        return
    inspected = subprocess.run(
        [
            engine,
            "inspect",
            "--format",
            '{{ index .Config.Labels "com.docker.compose.service" }}|{{.Name}}',
            *container_ids,
        ],
        cwd=repository_root(),
        check=False,
        capture_output=True,
        text=True,
    )
    if inspected.returncode != 0:
        raise RunnerError("could not inspect existing target container labels")
    conflicts = []
    for line in inspected.stdout.splitlines():
        compose_service, _, raw_name = line.partition("|")
        name = raw_name.lstrip("/")
        if name in LEGACY_TARGET_CONTAINERS or compose_service in LEGACY_TARGET_SERVICES:
            conflicts.append(name or compose_service)
    conflicts = sorted(set(conflicts))
    if conflicts:
        names = ", ".join(conflicts)
        raise RunnerError(
            "existing target containers must be stopped before low-resource up: " + names
        )


def status(engine: str) -> None:
    run(
        [
            engine,
            "ps",
            "-a",
            "--filter",
            f"label={PROJECT_LABEL}",
            "--format",
            "{{.Names}}|{{.Status}}|{{.Ports}}",
        ]
    )
    container_ids = project_container_ids(engine, include_stopped=False)
    if container_ids:
        cpu_field = "{{.CPUPerc}}" if engine == "docker" else "{{.CPU}}"
        run(
            [
                engine,
                "stats",
                "--no-stream",
                "--format",
                "{{.Name}}|" + cpu_field + "|{{.MemUsage}}",
                *container_ids,
            ]
        )


def stop(engine: str) -> None:
    container_ids = project_container_ids(engine, include_stopped=False)
    if not container_ids:
        print("PASS: no running minimal external-dev containers")
        return
    run([engine, "stop", *container_ids])
    print(f"PASS: stopped {len(container_ids)} project containers; volumes were preserved")


def run_all(engine: str, env_file: Path) -> None:
    build(engine, env_file)
    up(engine, env_file)
    try:
        smoke(engine, env_file)
        status(engine)
    except RunnerError as error:
        try:
            stop(engine)
        except RunnerError as stop_error:
            raise RunnerError(
                "smoke/status failed and project-scoped stop also failed"
            ) from stop_error
        raise RunnerError("smoke/status failed; project-scoped stop completed") from error


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run the low-resource Issue #520 external-dev authentication stack."
    )
    parser.add_argument(
        "action",
        choices=("preflight", "build", "up", "smoke", "status", "stop", "all"),
    )
    parser.add_argument("--env-file", type=Path)
    parser.add_argument("--engine", choices=("podman", "docker"))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        engine = detect_engine(args.engine)
        env_file = args.env_file.absolute() if args.env_file else None
        if args.action in ENV_ACTIONS and env_file is None:
            raise RunnerError(f"--env-file is required for {args.action}")
        if args.action in ENV_ACTIONS and not env_file.is_file():
            raise RunnerError("approved env file is not a regular file")

        if args.action == "preflight":
            preflight(engine, env_file)
        elif args.action == "build":
            build(engine, env_file)
        elif args.action == "up":
            up(engine, env_file)
        elif args.action == "smoke":
            smoke(engine, env_file)
        elif args.action == "status":
            status(engine)
        elif args.action == "stop":
            stop(engine)
        else:
            run_all(engine, env_file)
    except RunnerError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
