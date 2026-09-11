"""Offline replay tests for GH-696; fake credentials and mocked DB/container IO.

Run: python3 -B -m unittest discover -s tools -p test_governance_provision.py -v
"""

import contextlib
import importlib.util
import io
from pathlib import Path
import secrets
import subprocess
import tempfile
import unittest
from unittest.mock import patch


SPEC = importlib.util.spec_from_file_location(
    "governance_provision",
    Path(__file__).resolve().parents[1]
    / "postgres/runtime/provision-governance-external-dev.py",
)
provisioner = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(provisioner)


class ProvisionReplayTests(unittest.TestCase):
    def setUp(self):
        self.root = Path(self.enterContext(tempfile.TemporaryDirectory()))
        self.env = self.root / ".env.governance-dev"
        self.jar = self.root / "fake-runner.jar"
        self.jar.touch()
        self.credentials = {}
        lines = ["# Synthetic test credentials; no live environment access.", ""]
        for context, database in provisioner.TARGETS:
            key = context.replace("-", "_").upper()
            owner, app = secrets.token_hex(32), secrets.token_hex(32)
            self.credentials[database] = (owner, app)
            lines.extend((f"{key}_DB_URL=jdbc:postgresql://account-postgres:5432/{database}",
                          f"{key}_DB_USER={database}_app", f"{key}_DB_PASSWORD={app}",
                          f"{key}_DB_OWNER_PASSWORD={owner}"))
        # Preserve comments, blank lines, and CRLF byte-for-byte on replay.
        self.original = ("\r\n".join(lines) + "\r\n").encode()
        self.env.write_bytes(self.original)
        self.env.chmod(0o600)
        self.inode = self.env.stat().st_ino
        self.history = {database: "pending" for _, database in provisioner.TARGETS}
        self.events = []
        self.bad_owner = None
        self.enterContext(patch.object(provisioner, "ROOT", self.root))
        self.enterContext(contextlib.redirect_stdout(io.StringIO()))
        self.transport = self.enterContext(patch.object(provisioner, "run", side_effect=self.run_transport))
        self.enterContext(patch.object(provisioner, "admin", side_effect=self.catalog))
        self.enterContext(patch.object(provisioner, "verify_owner_login", side_effect=self.owner_login))
        self.enterContext(patch.object(provisioner, "runtime_sql", side_effect=self.runtime_login))
        self.create = self.enterContext(patch.object(provisioner, "provision"))
        self.acl = self.enterContext(patch.object(
            provisioner, "restrict_acl", side_effect=lambda db: self.events.append((db, "acl"))))
        self.migrations = self.enterContext(patch.object(provisioner, "migrate", side_effect=self.migrate))
        self.schema = self.enterContext(patch.object(provisioner, "schema_gate"))
        self.verify = self.enterContext(patch.object(provisioner, "verify", return_value=8))

    def run_transport(self, command, *args, **kwargs):
        if command[:1] == ["git"] and command[3] in ("check-ignore", "ls-files"):
            return subprocess.CompletedProcess(command, 0, "", "")
        if command == ["podman", "image", "exists", provisioner.JRE]:
            return subprocess.CompletedProcess(command, 0, "", "")
        if command[:2] == ["podman", "inspect"]:
            return subprocess.CompletedProcess(command, 0, "fake-container image mount", "")
        self.fail(f"Unexpected transport command: {command[0]}")

    def catalog(self, sql, database=None):
        if sql == "SHOW server_version_num;":
            return "160000"
        if sql.startswith("SELECT oid, datname, datdba, datacl FROM pg_database"):
            return "unchanged-unrelated-catalog"
        if sql.startswith("SELECT count(*) FROM pg_database WHERE datname="):
            return "1"
        if sql.startswith("SELECT count(*) FROM pg_roles WHERE rolname IN (") and " AND " not in sql:
            return "2"
        if sql.startswith("SELECT count(*) FROM ") and any(
            marker in sql for marker in ("pg_extension", "rolsuper", "pg_auth_members",
                                        "pg_database d JOIN", "pg_namespace n JOIN")
        ):
            return "0"
        self.fail("Unexpected catalog query")

    def owner_login(self, database, password, host, port):
        self.assertEqual(password, self.credentials[database][0])
        self.events.append((database, "owner-login"))
        if database == self.bad_owner:
            raise provisioner.GateError("Existing owner credentials do not authenticate")

    def runtime_login(self, database, password, host, port, sql):
        self.assertEqual(password, self.credentials[database][1])
        self.assertEqual(sql, "SELECT 1;")
        self.events.append((database, "app-login"))
        return subprocess.CompletedProcess([], 0, "1\n", "")

    def migrate(self, jar, context, database, password, prefix, action):
        self.assertEqual(password, self.credentials[database][0])
        self.events.append((database, action))
        if action == "migrate":
            self.history[database] = "current"
        elif action == "validate":
            if self.history[database] != "current":
                raise provisioner.GateError("Pending migrations")
        else:
            self.fail("Unexpected migration action")

    def execute(self, apply):
        provisioner.execute(self.env, self.jar, apply, "account-postgres", 5432)

    def assert_env_preserved(self):
        self.assertEqual(self.env.read_bytes(), self.original)
        self.assertEqual(self.env.stat().st_ino, self.inode)
        self.assertEqual(self.env.stat().st_mode & 0o777, 0o600)

    def test_read_only_pending_history_rejected_without_acl_or_provisioning(self):
        with self.assertRaisesRegex(provisioner.GateError, "Pending migrations"):
            self.execute(False)
        self.acl.assert_not_called()
        self.create.assert_not_called()
        self.schema.assert_not_called()
        self.verify.assert_not_called()
        self.assertEqual([action for _, action in self.events if action in ("migrate", "validate")],
                         ["validate"])
        self.assert_env_preserved()

    def test_apply_resumes_empty_and_pending_history_and_preserves_credentials_on_replay(self):
        self.history["budget_db"] = "empty"
        for replay in range(2):
            with self.subTest(replay=replay):
                self.events.clear()
                self.execute(True)
                # Every target authenticates before the first ACL or migration change.
                self.assertEqual(self.events[:4], [
                    ("budget_db", "owner-login"), ("budget_db", "app-login"),
                    ("internal_audit_db", "owner-login"), ("internal_audit_db", "app-login")])
                for _, database in provisioner.TARGETS:
                    self.assertEqual([action for db, action in self.events
                                      if db == database and action in ("acl", "migrate", "validate")],
                                     ["acl", "migrate", "acl", "validate"])
                    self.assertEqual(self.history[database], "current")
                    self.verify.assert_any_call(database, self.credentials[database][1],
                                                "account-postgres", "5432")
                self.create.assert_not_called()
                self.assert_env_preserved()

    def test_bad_second_owner_authentication_blocks_all_mutations(self):
        self.bad_owner = "internal_audit_db"
        with self.assertRaisesRegex(provisioner.GateError, "owner credentials"):
            self.execute(True)
        self.create.assert_not_called()
        self.acl.assert_not_called()
        self.migrations.assert_not_called()
        self.schema.assert_not_called()
        self.verify.assert_not_called()
        self.assert_env_preserved()


if __name__ == "__main__":
    unittest.main()
