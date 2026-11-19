"""Run the built Journal API twice against a NEW schema in the synthetic GH-762 PostgreSQL DB.

Prerequisites: API bootJar, local PostgreSQL test container on 127.0.0.1:17622.
No production configuration or credentials are read. The generated internal credential stays in
the two child environments and HTTP headers; output contains only assertions and log locations.
"""
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
import urllib.error
import urllib.request
import uuid


ROOT = Path(__file__).resolve().parents[2]
JAR = next((ROOT / "api/build/libs").glob("*.jar"))
SCHEMA = "runtime_762_" + uuid.uuid4().hex
TOKEN = secrets.token_urlsafe(48)
SCOPE = {"fiscalPeriodId": 762, "startDate": "2026-05-01", "endDate": "2026-05-31"}
BASE = "http://127.0.0.1:17623/api/internal/journal-periods/"
PARAMETERS = {
    "spring.profiles.active": "local",
    "spring.datasource.url": "jdbc:postgresql://127.0.0.1:17622/account_762?currentSchema=" + SCHEMA,
    "spring.datasource.driver-class-name": "org.postgresql.Driver",
    "spring.datasource.username": "postgres",
    "spring.datasource.password": "",
    "spring.flyway.enabled": True,
    "spring.flyway.schemas": SCHEMA,
    "spring.flyway.locations": "classpath:db/journal-migration",
    "spring.jpa.properties.hibernate.default_schema": SCHEMA,
    "spring.jpa.mapping-resources": "META-INF/journal-ledger-orm.xml",
    "spring.jpa.hibernate.ddl-auto": "validate",
    "spring.sql.init.mode": "never",
    "spring.cloud.config.enabled": False,
    "spring.cloud.discovery.enabled": False,
    "spring.cloud.vault.enabled": False,
    "eureka.client.enabled": False,
    "spring.kafka.listener.auto-startup": False,
    "management.tracing.enabled": False,
    "server.address": "127.0.0.1",
    "server.port": 17623,
    "journal.period-fence.service-token": TOKEN,
}


def post(operation, body):
    request = urllib.request.Request(
        BASE + operation, data=json.dumps(body).encode(), method="POST",
        headers={"Content-Type": "application/json", "X-Service-Identity": "closing",
                 "Authorization": "Bearer " + TOKEN})
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as failure:
        return failure.code, None


def start(number):
    log_path = Path("/tmp") / f"account-762-api-restart-{number}.log"
    environment = os.environ.copy()
    environment["SPRING_APPLICATION_JSON"] = json.dumps(PARAMETERS)
    log = log_path.open("w")
    process = subprocess.Popen(["java", "-jar", str(JAR)], env=environment,
                               stdout=log, stderr=subprocess.STDOUT)
    deadline = time.monotonic() + 300
    try:
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError(f"Synthetic API exited; inspect {log_path}")
            try:
                status, result = post("checkpoint", SCOPE)
                if status == 200:
                    assert result["scope"] == SCOPE
                    return process, log
            except (OSError, urllib.error.URLError):
                pass
            time.sleep(1)
        raise RuntimeError(f"Synthetic API startup timed out; inspect {log_path}")
    except BaseException:
        stop(process, log)
        raise


def stop(process, log, crash=False):
    if crash:
        process.kill()
    else:
        process.terminate()
    try:
        process.wait(timeout=30)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait(timeout=10)
    log.close()


def main():
    process, log = start(1)
    try:
        status, checkpoint = post("checkpoint", SCOPE)
        assert status == 200
        command = {"operationId": str(uuid.uuid4()), "checkpoint": checkpoint,
                   "actor": "synthetic-runtime"}
        status, original = post("fence", command)
        assert status == 200 and original == {"command": command, "state": "ACTIVE"}
        print("First JVM: committed ACTIVE fence", flush=True)
    finally:
        # The acknowledged fence must survive abrupt JVM loss, not only a graceful shutdown.
        stop(process, log, crash=True)

    process, log = start(2)
    try:
        status, recovered = post("fence", command)
        assert status == 200 and recovered == original
        status, released = post("release", command)
        assert status == 200 and released == {"command": command, "state": "RELEASED"}
        status, _ = post("fence", command)
        assert status == 409
        status, current = post("checkpoint", SCOPE)
        assert status == 200 and current["revision"] > checkpoint["revision"]
        print("Second JVM: same durable receipt recovered; release tombstone rejects replay", flush=True)
        print("PASS: actual Journal API process restart with PostgreSQL; schema=" + SCHEMA, flush=True)
    finally:
        stop(process, log)


if __name__ == "__main__":
    main()
