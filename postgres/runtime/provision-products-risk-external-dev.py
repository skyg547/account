#!/usr/bin/env python3
"""Issue #649: bounded, secret-redacting Products/Risk development provisioning."""
import argparse
import fcntl
import importlib.util
import os
from pathlib import Path
import re
import secrets
import stat
import subprocess
import tempfile

# Reuse the reviewed stdin-only transport and safe env parser without changing
# Accounting's context list, approval ticket or executable entry point.
_spec = importlib.util.spec_from_file_location(
    'accounting_provision_helpers',
    Path(__file__).with_name('provision-accounting-external-dev.py'))
_helpers = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_helpers)
GateError = _helpers.GateError
require = _helpers.require
run = _helpers.run
assignments = _helpers.assignments
container_shell = _helpers.container_shell
admin = _helpers.admin
env_value = _helpers.env_value
endpoint = _helpers.endpoint
runtime_sql = _helpers.runtime_sql
provision = _helpers.provision
ROOT = _helpers.ROOT
CONTAINER = _helpers.CONTAINER
JRE = _helpers.JRE
CONTEXTS = ('deposit', 'loan', 'asset-lease', 'account-mart', 'ecl', 'reconciliation')


def migrate(jar, context, database, password, prefix, action):
    values = {'MIGRATION_DB_URL': prefix.format(database=database),
              'MIGRATION_DB_USER': database + '_owner',
              'MIGRATION_DB_PASSWORD': password, 'MIGRATION_TARGET_ENV': 'development',
              'MIGRATION_EXPECTED_DATABASE': database,
              'MIGRATION_ALLOW_MIGRATE': 'true', 'MIGRATION_CHANGE_TICKET': 'GH-649'}
    # Only the jar is mounted. Secrets travel on stdin, never container config/argv.
    run(['podman', 'run', '--rm', '-i', '--pull=never', '--network=account-network', '--cpus=1',
         '--memory=512m', '--pids-limit=128', '--read-only', '--cap-drop=ALL',
         '--security-opt=no-new-privileges', '-v', str(jar) + ':/runner.jar:ro',
         '--entrypoint', 'sh', JRE, '-s'],
        'set -eu\n' + assignments(values) + 'exec java -Xmx256m -jar /runner.jar '
        + '--context=' + context + ' --action=' + action + '\n')


def restrict_acl(database):
    """Rebuild only the target's runtime ACL; new objects remain denied by default.

    PostgreSQL catalogs include ordinary, partitioned and partition child tables,
    plus SERIAL/identity sequences. No rows, schema ownership or role passwords
    are changed here. One transaction prevents partial ACL publication.
    """
    admin(f"""
BEGIN;
REVOKE ALL ON DATABASE {database} FROM PUBLIC, {database}_app;
GRANT CONNECT ON DATABASE {database} TO {database}_app;
REVOKE ALL ON SCHEMA public FROM PUBLIC, {database}_app;
GRANT USAGE ON SCHEMA public TO {database}_app;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC, {database}_app;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM PUBLIC, {database}_app;
ALTER DEFAULT PRIVILEGES FOR ROLE {database}_owner
    REVOKE ALL ON TABLES FROM PUBLIC, {database}_app;
ALTER DEFAULT PRIVILEGES FOR ROLE {database}_owner IN SCHEMA public
    REVOKE ALL ON TABLES FROM PUBLIC, {database}_app;
ALTER DEFAULT PRIVILEGES FOR ROLE {database}_owner
    REVOKE ALL ON SEQUENCES FROM PUBLIC, {database}_app;
ALTER DEFAULT PRIVILEGES FOR ROLE {database}_owner IN SCHEMA public
    REVOKE ALL ON SEQUENCES FROM PUBLIC, {database}_app;
SELECT format('GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE %I.%I TO %I',
              schemaname, tablename, '{database}_app')
FROM pg_tables WHERE schemaname='public'
AND tablename NOT LIKE 'flyway\\_schema\\_history%' ESCAPE '\\'
\\gexec
SELECT format('GRANT USAGE,SELECT ON SEQUENCE %I.%I TO %I',
              n.nspname, c.relname, '{database}_app')
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname='public' AND c.relkind='S'
\\gexec
COMMIT;
""", database)


def verify(database, password, host, port):
    negative = runtime_sql(database, secrets.token_hex(32), host, port,
                           'SELECT 1;', check=False)
    require(negative.returncode != 0
            and 'password authentication failed for user' in negative.stderr,
            'Password-negative login did not prove authentication rejection')
    result = runtime_sql(database, password, host, port, f"""
SELECT current_database() = '{database}' AND current_user = '{database}_app';
SELECT count(*) FROM pg_tables WHERE schemaname = 'public';
SELECT NOT has_schema_privilege(current_user, 'public', 'CREATE')
AND NOT has_database_privilege(current_user, current_database(), 'CREATE')
AND NOT has_database_privilege(current_user, current_database(), 'TEMP');
SELECT count(*) = 0 FROM pg_roles WHERE rolname = current_user
AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls);
SELECT count(*) = 0 FROM pg_auth_members WHERE member = (SELECT oid FROM pg_roles WHERE rolname = current_user);
SELECT count(*) = 0 FROM pg_tables WHERE schemaname = 'public'
AND tablename LIKE 'flyway\\_schema\\_history%' ESCAPE '\\'
AND has_table_privilege(current_user, format('%I.%I', schemaname, tablename),
'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER');
SELECT count(*) = 0 FROM pg_tables WHERE schemaname = 'public'
AND has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'TRUNCATE,REFERENCES,TRIGGER');
""").stdout.strip().splitlines()
    require(len(result) == 7 and result[0] == 't' and int(result[1]) > 8
            and all(item == 't' for item in result[2:]), 'Runtime isolation gate failed')
    denied = runtime_sql(database, password, host, port,
                         'BEGIN; CREATE TABLE public.account_649_ddl_probe(id integer); ROLLBACK;', False)
    require(denied.returncode != 0 and 'permission denied for schema public' in denied.stderr,
            'Runtime DDL denial gate failed')
    return int(result[1])


def execute(env_path, jar, apply):
    require(jar.is_file(), 'Build the migration-runner jar before execution')
    run(['podman', 'image', 'exists', JRE])
    lock_fd = os.open(env_path.parent, os.O_RDONLY | os.O_DIRECTORY)
    fd = None
    try:
        # A directory inode survives atomic env replacement and excludes other runs.
        fcntl.flock(lock_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        fd = os.open(env_path, os.O_RDWR | os.O_NOFOLLOW)
        fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        info = os.fstat(fd)
        require(stat.S_ISREG(info.st_mode) and stat.S_IMODE(info.st_mode) == 0o600
                and info.st_uid == os.getuid() and info.st_nlink == 1,
                'Environment file must be an owned mode-600 regular file')
        original = os.read(fd, info.st_size + 1).decode('utf-8')
        lines = original.splitlines()
        prefix, host, port = endpoint(lines)
        require(int(admin('SHOW server_version_num;')) >= 150000, 'PostgreSQL 15+ required')
        require(admin("SELECT count(*) FROM pg_extension WHERE extname = 'pgaudit';") == '0',
                'Audit extension needs a separately reviewed secret injection path')
        target_names = ','.join("'" + c.replace('-', '_') + "_dev'" for c in CONTEXTS)
        catalog_sql = ("SELECT oid, datname, datdba, datacl FROM pg_database "
                       f"WHERE datname NOT IN ({target_names}) ORDER BY oid;")
        baseline = admin(catalog_sql)
        manifest = admin('SELECT row_to_json(s) FROM public.account_dev_bootstrap_status s;')
        identity = run(['podman', 'inspect', CONTAINER, '--format',
                        '{{.Id}} {{.Image}} {{json .Mounts}}']).stdout
        pending, targets = {}, []
        for context in CONTEXTS:
            database = context.replace('-', '_') + '_dev'
            key = context.replace('-', '_').upper()
            expected = {key + '_DB_URL': prefix.format(database=database),
                        key + '_DB_USER': database + '_app'}
            for name, value in expected.items():
                current = env_value(lines, name)
                require(not current or current == value, 'Existing target connection differs')
                if not current:
                    pending[name] = value
            owner = env_value(lines, key + '_DB_OWNER_PASSWORD')
            app = env_value(lines, key + '_DB_PASSWORD')
            role_count = int(admin(f"SELECT count(*) FROM pg_roles WHERE rolname IN ('{database}_owner','{database}_app');"))
            db_count = int(admin(f"SELECT count(*) FROM pg_database WHERE datname = '{database}';"))
            require(not (role_count or db_count) or (owner and app),
                    'Existing target requires previously saved owner/runtime credentials')
            require(admin(f"SELECT count(*) FROM pg_roles WHERE rolname IN ('{database}_owner','{database}_app') AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls OR NOT rolcanlogin);") == '0',
                    'Existing target role attributes are unsafe')
            require(admin(f"SELECT count(*) FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.member WHERE r.rolname IN ('{database}_owner','{database}_app');") == '0',
                    'Existing target role memberships are unsafe')
            require(admin(f"SELECT count(*) FROM pg_database d JOIN pg_roles r ON r.oid=d.datdba WHERE d.datname='{database}' AND r.rolname <> '{database}_owner';") == '0',
                    'Existing target database ownership differs')
            if not owner:
                owner = pending[key + '_DB_OWNER_PASSWORD'] = secrets.token_hex(32)
            if not app:
                app = pending[key + '_DB_PASSWORD'] = secrets.token_hex(32)
            require(owner != app and re.fullmatch('[a-f0-9]{64}', owner)
                    and re.fullmatch('[a-f0-9]{64}', app), 'Distinct generated credentials required')
            # Empty DBs can resume after a pre-migration failure. Non-empty DBs
            # must validate; partial histories require reviewed forward recovery.
            if db_count:
                require(admin("SELECT count(*) FROM pg_namespace n JOIN pg_roles r ON r.oid=n.nspowner "
                              f"WHERE n.nspname='public' AND r.rolname NOT IN ('{database}_owner', 'pg_database_owner');",
                              database) == '0', 'Existing public schema ownership differs')
                if admin("SELECT count(*) FROM pg_tables WHERE schemaname = 'public';", database) != '0':
                    migrate(jar, context, database, owner, prefix, 'validate')
                require(runtime_sql(database, app, host, port, 'SELECT 1;').stdout.strip() == '1',
                        'Existing runtime credentials do not authenticate')
            targets.append((context, database, owner, app))
        print('Preflight PASS: six canonical development targets; existing state preserved', flush=True)
        if not apply:
            return
        if pending:
            # Preserve unrelated lines; replace only empty target keys or append missing ones.
            updated = [line for line in lines if line.split('=', 1)[0] not in pending]
            updated.extend(name + '=' + value for name, value in pending.items())
            current_info = os.stat(env_path, follow_symlinks=False)
            require((current_info.st_dev, current_info.st_ino, current_info.st_size, current_info.st_mtime_ns)
                    == (info.st_dev, info.st_ino, info.st_size, info.st_mtime_ns),
                    'Environment file changed during preflight')
            temporary = None
            try:
                with tempfile.NamedTemporaryFile(mode='w', dir=env_path.parent,
                                                 prefix='.env.external-dev.', delete=False) as output:
                    temporary = output.name
                    output.write('\n'.join(updated) + '\n')
                    output.flush()
                    os.fsync(output.fileno())
                os.replace(temporary, env_path)
                temporary = None
                os.fsync(lock_fd)
            finally:
                if temporary:
                    os.unlink(temporary)
        print('Environment inputs persisted securely before database mutation', flush=True)
        for context, database, owner, app in targets:
            provision(database, owner, app)
            restrict_acl(database)
            migrate(jar, context, database, owner, prefix, 'migrate')
            restrict_acl(database)
            migrate(jar, context, database, owner, prefix, 'validate')
            gate = (ROOT / 'postgres/runtime/check-runtime-schemas.sh').read_text()
            container_shell(gate, {'ACCOUNT_DATABASES': database})
            tables = verify(database, app, host, port)
            print(f'{context}: migrate/validate/login/ACL/DDL PASS; public tables={tables}', flush=True)
        final = admin(catalog_sql)
        require(baseline == final, 'Unrelated database catalog changed')
        require(manifest == admin('SELECT row_to_json(s) FROM public.account_dev_bootstrap_status s;'),
                'Existing bootstrap manifest changed')
        require(identity == run(['podman', 'inspect', CONTAINER, '--format',
                                 '{{.Id}} {{.Image}} {{json .Mounts}}']).stdout,
                'PostgreSQL container/image/mount identity changed')
        print('PASS: 6/6 databases; unrelated database catalog, manifest, container and mounts unchanged', flush=True)
    finally:
        if fd is not None:
            os.close(fd)
        os.close(lock_fd)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env-file', type=Path, required=True)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--apply', action='store_true', help='Apply the approved GH-649 change')
    args = parser.parse_args()
    try:
        execute(args.env_file.absolute(), args.jar.resolve(), args.apply)
    except GateError as error:
        print('FAIL: ' + str(error), flush=True)
        return 1
    except (OSError, ValueError, subprocess.SubprocessError):
        print('FAIL: products/risk development gate stopped; diagnostic values/output redacted', flush=True)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
