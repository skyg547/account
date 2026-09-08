#!/usr/bin/env python3
"""Issue #640: bounded, secret-redacting provisioning of seven development DBs."""
import argparse
import fcntl
import os
from pathlib import Path
import re
import secrets
import shlex
import stat
import subprocess
import tempfile
from urllib.parse import urlsplit

CONTEXTS = ('journal-ledger', 'closing', 'payable', 'receivable', 'tax',
            'expenditure-resolution', 'reporting')
CONTAINER = 'account-postgres'
JRE = 'docker.io/library/eclipse-temurin:17-jre-alpine'
ROOT = Path(__file__).resolve().parents[2]


class GateError(Exception):
    pass


def require(condition, message):
    if not condition:
        raise GateError(message)


def run(args, payload=None, check=True):
    result = subprocess.run(args, input=payload, text=True, capture_output=True,
                            timeout=600)
    if check:
        require(result.returncode == 0, 'Subprocess failed (output redacted)')
    return result


def assignments(values):
    return ''.join('export ' + k + '=' + shlex.quote(v) + '\n'
                   for k, v in values.items())


def container_shell(script, values=None, check=True):
    return run(['podman', 'exec', '-i', CONTAINER, 'sh', '-s'],
               'set -eu\n' + assignments(values or {}) + script, check)


def admin(sql, database=None, values=None):
    # Local administrative socket; neither existing admin credentials nor env are read.
    return container_shell(
        'psql -X -qAt -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d '
        + (shlex.quote(database) if database else '"$POSTGRES_DB"')
        + " <<'ACCOUNT_SQL'\n" + sql + '\nACCOUNT_SQL\n', values).stdout.strip()


def env_value(lines, key):
    entries = [line.split('=', 1)[1].strip() for line in lines
               if line.startswith(key + '=')]
    require(len(entries) <= 1, 'Duplicate required environment key')
    if not entries:
        return ''
    value = entries[0]
    if value.startswith(('"', "'")):
        require(len(value) >= 2 and value[-1] == value[0], 'Invalid quoted value')
        value = value[1:-1]
    require(not any(c in value for c in '\n\r\x00'), 'Invalid environment value')
    return value


def endpoint(lines):
    value = env_value(lines, 'AUTH_DB_URL')
    require(value.startswith('jdbc:postgresql://'), 'Missing canonical Auth endpoint')
    url = urlsplit(value[5:])
    require(url.path == '/auth_dev' and url.hostname and not url.username
            and not url.password and not url.fragment
            and url.query in ('', 'sslmode=disable', 'sslmode=prefer', 'sslmode=require'),
            'Auth endpoint must be credential-free with a supported development sslmode')
    require(url.hostname not in ('localhost', '127.0.0.1', '::1', '0.0.0.0'),
            'A non-loopback external endpoint is required for password authentication')
    require(url.port is not None and 1 <= url.port <= 65535, 'Explicit DB port required')
    return value.replace('/auth_dev', '/{database}', 1), url.hostname, str(url.port)


def runtime_sql(database, password, host, port, sql, check=True):
    return container_shell(
        'psql -X -qAt -v ON_ERROR_STOP=1 --host "$PGHOST" --port "$PGPORT" '
        '--username "$PGUSER" --dbname "$PGDATABASE" '
        "<<'ACCOUNT_SQL'\n" + sql + '\nACCOUNT_SQL\n',
        {'PGPASSWORD': password, 'PGHOST': host, 'PGPORT': port,
         'PGUSER': database + '_app', 'PGDATABASE': database, 'PGCONNECT_TIMEOUT': '10'}, check)


def migrate(jar, context, database, password, prefix, action):
    values = {'MIGRATION_DB_URL': prefix.format(database=database),
              'MIGRATION_DB_USER': database + '_owner',
              'MIGRATION_DB_PASSWORD': password, 'MIGRATION_TARGET_ENV': 'development',
              'MIGRATION_EXPECTED_DATABASE': database,
              'MIGRATION_ALLOW_MIGRATE': 'true', 'MIGRATION_CHANGE_TICKET': 'GH-640'}
    # Only the jar is mounted. Secrets travel on stdin, never container config/argv.
    run(['podman', 'run', '--rm', '-i', '--pull=never', '--network=account-network', '--cpus=1',
         '--memory=512m', '--pids-limit=128', '--read-only', '--cap-drop=ALL',
         '--security-opt=no-new-privileges', '-v', str(jar) + ':/runner.jar:ro',
         '--entrypoint', 'sh', JRE, '-s'],
        'set -eu\n' + assignments(values) + 'exec java -Xmx256m -jar /runner.jar '
        + '--context=' + context + ' --action=' + action + '\n')


def provision(database, owner_password, app_password):
    # Same role/database/schema logic as init/10-create-service-databases.sh,
    # without password rotation, existing ownership changes or manifest replacement.
    admin(r"""
SET log_statement = 'none';
SET log_min_error_statement = 'panic';
SET log_min_duration_statement = -1;
SET log_min_duration_sample = -1;
SET log_statement_sample_rate = 0;
SET log_transaction_sample_rate = 0;
SET log_duration = off;
\getenv owner_password ACCOUNT_OWNER_PASSWORD
\getenv app_password ACCOUNT_APP_PASSWORD
""" + f"""
SELECT format('CREATE ROLE %I LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L', '{database}_owner', :'owner_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '{database}_owner')
\\gexec
SELECT format('CREATE ROLE %I LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L', '{database}_app', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '{database}_app')
\\gexec
SELECT 'CREATE DATABASE {database} OWNER {database}_owner'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = '{database}')
\\gexec
REVOKE ALL ON DATABASE {database} FROM PUBLIC;
GRANT CONNECT ON DATABASE {database} TO {database}_app;
""", values={'ACCOUNT_OWNER_PASSWORD': owner_password, 'ACCOUNT_APP_PASSWORD': app_password})
    admin(f"""
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO {database}_owner;
GRANT USAGE ON SCHEMA public TO {database}_app;
""", database)


def verify(database, password, host, port):
    require(runtime_sql(database, secrets.token_hex(32), host, port,
                        'SELECT 1;', check=False).returncode != 0,
            'Password-negative login unexpectedly succeeded')
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
                         'BEGIN; CREATE TABLE public.account_640_ddl_probe(id integer); ROLLBACK;', False)
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
        baseline = admin("SELECT oid, datname, datdba, datacl FROM pg_database ORDER BY oid;")
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
                if admin("SELECT count(*) FROM pg_tables WHERE schemaname = 'public';", database) != '0':
                    migrate(jar, context, database, owner, prefix, 'validate')
                require(runtime_sql(database, app, host, port, 'SELECT 1;').stdout.strip() == '1',
                        'Existing runtime credentials do not authenticate')
            targets.append((context, database, owner, app))
        print('Preflight PASS: seven canonical development targets; existing state preserved', flush=True)
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
            finally:
                if temporary:
                    os.unlink(temporary)
        print('Environment inputs persisted securely before database mutation', flush=True)
        for context, database, owner, app in targets:
            provision(database, owner, app)
            migrate(jar, context, database, owner, prefix, 'migrate')
            grant = (ROOT / 'postgres/runtime/grant-runtime-privileges.sh').read_text()
            container_shell(grant, {'ACCOUNT_DATABASES': database})
            migrate(jar, context, database, owner, prefix, 'validate')
            gate = (ROOT / 'postgres/runtime/check-runtime-schemas.sh').read_text()
            container_shell(gate, {'ACCOUNT_DATABASES': database})
            tables = verify(database, app, host, port)
            print(f'{context}: migrate/validate/login/ACL/DDL PASS; public tables={tables}', flush=True)
        final = admin('SELECT oid, datname, datdba, datacl FROM pg_database ORDER BY oid;')
        require(set(baseline.splitlines()) <= set(final.splitlines()), 'Existing database catalog changed')
        require(manifest == admin('SELECT row_to_json(s) FROM public.account_dev_bootstrap_status s;'),
                'Existing bootstrap manifest changed')
        require(identity == run(['podman', 'inspect', CONTAINER, '--format',
                                 '{{.Id}} {{.Image}} {{json .Mounts}}']).stdout,
                'PostgreSQL container/image/mount identity changed')
        print('PASS: 7/7 databases; prior database catalog, manifest, container and mounts unchanged', flush=True)
    finally:
        if fd is not None:
            os.close(fd)
        os.close(lock_fd)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env-file', type=Path, required=True)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--apply', action='store_true', help='Apply the approved GH-640 change')
    args = parser.parse_args()
    try:
        execute(args.env_file.absolute(), args.jar.resolve(), args.apply)
    except GateError as error:
        print('FAIL: ' + str(error), flush=True)
        return 1
    except (OSError, ValueError, subprocess.SubprocessError):
        print('FAIL: accounting development gate stopped; diagnostic values/output redacted', flush=True)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
