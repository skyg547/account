"""Shared, value-suppressing external-dev execution for Issue #690.

Only known development databases and runtime roles are accepted. Credentials
travel in the child environment, never command arguments or diagnostic output.
The lock covers both seed writes and batch runs across processes/worktrees.
"""
from __future__ import annotations

import contextlib
import fcntl
import importlib.util
import os
from pathlib import Path
import re
import stat
import subprocess
from urllib.parse import parse_qs, urlsplit

ROOT = Path(__file__).resolve().parent.parent
DATE = "2090-01-15"
PACKAGES = {
    "accounting": ("master-data", "journal-ledger", "payable", "receivable", "expenditure", "closing"),
    "products": ("deposit", "loan", "asset-lease"),
    "risk": ("ecl", "account-mart", "reconciliation"),
}
CONTEXTS = {
    "master-data": ("MASTER_DATA", "master_data"),
    "journal-ledger": ("JOURNAL_LEDGER", "journal_ledger"),
    "payable": ("PAYABLE", "payable"),
    "receivable": ("RECEIVABLE", "receivable"),
    "expenditure": ("EXPENDITURE_RESOLUTION", "expenditure_resolution"),
    "closing": ("CLOSING", "closing"),
    "deposit": ("DEPOSIT", "deposit"),
    "loan": ("LOAN", "loan"),
    "asset-lease": ("ASSET_LEASE", "asset_lease"),
    "ecl": ("ECL", "ecl"),
    "account-mart": ("ACCOUNT_MART", "account_mart"),
    "reconciliation": ("RECONCILIATION", "reconciliation"),
}
PODMAN_SOCKET = None
PENDING = Path(f"/tmp/account-business-batch-{os.getuid()}.pending")


def configure_transport(socket: str | None):
    global PODMAN_SOCKET
    expected = f"/run/user/{os.getuid()}/podman/podman.sock"
    if socket is not None and socket != expected:
        raise BatchError("only the existing current-user Podman socket is supported")
    PODMAN_SOCKET = socket


class BatchError(Exception):
    """Messages must contain only static labels, never subprocess output."""


def assert_engine_idle(engine):
    if PENDING.exists():
        raise BatchError("previous launch/stop is unresolved; inspect the issue-owned container before retry")
    active = command([engine, "ps", "--filter", "label=account.issue=690",
                      "--filter", "status=running", "--format", "{{.Names}}"])
    if active:
        raise BatchError("an issue-owned helper or batch is still running")


def mark_pending(name):
    fd = os.open(PENDING, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    with os.fdopen(fd, "w") as stream:
        stream.write(name + "\n")


def clear_pending():
    PENDING.unlink(missing_ok=True)


def recover_pending(engine):
    """Stop only the recorded issue-owned batch; never alter Batch metadata."""
    if not PENDING.exists():
        raise BatchError("no pending launch is recorded")
    fd = os.open(PENDING, os.O_RDONLY | os.O_NOFOLLOW)
    with os.fdopen(fd) as stream:
        name = stream.read(128).strip()
    if not re.fullmatch(r"account-690-[a-z-]+-[a-f0-9]{8}", name):
        raise BatchError("invalid pending container record")
    # A launch can fail before the engine creates a container. Only a successful
    # engine inventory (including stopped containers) proves that exact absence.
    names = command([engine, "ps", "--all", "--format", "{{.Names}}"])
    if name not in names.splitlines():
        clear_pending()
        return
    label = command([engine, "inspect", "--format", '{{index .Config.Labels "account.issue"}}', name])
    if label != "690":
        raise BatchError("pending container ownership could not be verified")
    running = command([engine, "inspect", "--format", "{{.State.Running}}", name])
    if running == "true":
        command([engine, "stop", "--time", "10", name], timeout=300)
        running = command([engine, "inspect", "--format", "{{.State.Running}}", name])
    if running != "false":
        raise BatchError("pending container is not confirmed stopped")
    clear_pending()


def load_inputs(path: Path) -> dict[str, str]:
    spec = importlib.util.spec_from_file_location("safe_env", ROOT / "tools/validate-minimal-auth-env.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    try:
        with module.open_env_file(path) as stream:
            return module.parse_env(stream)
    except (OSError, ValueError, UnicodeError, module.ValidationError):
        raise BatchError("approved input file is absent, insecure, or invalid") from None


def database_inputs(values: dict[str, str], context: str) -> dict[str, str]:
    prefix, database = CONTEXTS[context]
    # Master Data historically uses the DEV_DB_* contract in the minimal stack.
    if context == "master-data" and not values.get("MASTER_DATA_DB_URL"):
        url = "jdbc:postgresql://{}:{}/{}".format(
            values.get("DEV_DB_HOST", ""), values.get("DEV_DB_PORT", ""), values.get("DEV_DB_NAME", ""))
        user, password = values.get("DEV_DB_USER", ""), values.get("DEV_DB_PASSWORD", "")
    else:
        url = values.get(f"{prefix}_DB_URL", "")
        user, password = values.get(f"{prefix}_DB_USER", ""), values.get(f"{prefix}_DB_PASSWORD", "")
    try:
        if not url.startswith("jdbc:postgresql://"):
            raise ValueError()
        parsed = urlsplit(url[5:])
        query = parse_qs(parsed.query, strict_parsing=True, keep_blank_values=True)
        sslmode = query.get("sslmode", ["prefer"])
        if (parsed.scheme != "postgresql" or not parsed.hostname or parsed.username or parsed.password
                or parsed.fragment or parsed.path != f"/{database}_dev"
                or user != f"{database}_dev_app" or not password
                or set(query) - {"sslmode"} or len(sslmode) != 1
                or sslmode[0] not in {"disable", "allow", "prefer", "require", "verify-ca", "verify-full"}
                or any(ord(c) < 32 for c in url + user + password)):
            raise ValueError()
        port = parsed.port or 5432
        if not 1 <= port <= 65535:
            raise ValueError()
    except (ValueError, TypeError):
        raise BatchError(f"{context}: development database input contract failed") from None
    return {"PGHOST": parsed.hostname, "PGPORT": str(port), "PGDATABASE": f"{database}_dev",
            "PGUSER": user, "PGPASSWORD": password, "PGSSLMODE": sslmode[0], "PGCONNECT_TIMEOUT": "15"}


@contextlib.contextmanager
def execution_lock():
    path = Path(f"/tmp/account-business-batch-{os.getuid()}.lock")
    descriptor = os.open(path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
    try:
        metadata = os.fstat(descriptor)
        if not stat.S_ISREG(metadata.st_mode) or metadata.st_uid != os.getuid():
            raise BatchError("invalid execution lock")
        try:
            fcntl.flock(descriptor, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise BatchError("another seed or batch execution is active") from None
        yield
    finally:
        os.close(descriptor)


def command(args: list[str], *, env=None, stdin=None, timeout=300) -> str:
    if args[0] == "podman" and PODMAN_SOCKET:
        args = ["podman", "--remote", "--url", f"unix://{PODMAN_SOCKET}", *args[1:]]
    try:
        result = subprocess.run(args, input=stdin, text=True, capture_output=True,
                                env=env, timeout=timeout, cwd=ROOT)
    except subprocess.TimeoutExpired:
        raise BatchError("child command timed out; inspect issue-owned container state before retry") from None
    except OSError:
        raise BatchError("child command could not start") from None
    if result.returncode:
        # Do not expose raw errors: a JDBC/psql/build failure can contain inputs.
        categories = (
            ("permission denied", "database privilege"),
            ("does not exist", "missing schema/object"),
            ("violates", "database constraint"),
            ("syntax error", "SQL syntax"),
            ("synthetic seed assertion failed", "seed assertion"),
            ("value too long", "column length"),
            ("timeout", "timeout"),
        )
        category = next((label for token, label in categories if token in result.stderr.lower()), "command")
        raise BatchError(f"child command failed (exit {result.returncode}; {category}; output suppressed)")
    return result.stdout.strip()


def query(engine: str, values: dict[str, str], context: str, sql: str, *, readonly=True) -> str:
    env = os.environ.copy()
    inputs = database_inputs(values, context)
    inputs["PGOPTIONS"] = "-c statement_timeout=60000 -c lock_timeout=10000 -c idle_in_transaction_session_timeout=60000" + (
        " -c default_transaction_read_only=on" if readonly else "")
    env.update(inputs)
    # Reuse the already-running, resource-bounded PostgreSQL client gate. This
    # avoids repeatedly creating helper containers on I/O-constrained hosts.
    package = next(key for key, contexts in PACKAGES.items() if context in contexts)
    name = f"account-{package}-external-dev-{package}-db-check-1"
    args = [engine, "exec", "-i"]
    for key in inputs:
        args += ["--env", key]
    args += [name, "psql", "-X", "-qAt", "-v", "ON_ERROR_STOP=1"]
    return command(args, env=env, stdin=sql)


def seed_path(context: str, filename: str) -> Path:
    package = next(key for key, contexts in PACKAGES.items() if context in contexts)
    return ROOT / "tools/seeds" / package / context / filename


def assert_query(engine, values, context, path):
    if not path.is_file():
        raise BatchError(f"{context}: required assertion file is missing")
    if query(engine, values, context, path.read_text(encoding="utf-8")) != "t":
        raise BatchError(f"{context}: synthetic data assertion failed")
