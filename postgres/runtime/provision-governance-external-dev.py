#!/usr/bin/env python3
"""GH-696: provision only budget_db and internal_audit_db on account-postgres.

Usage (after building :migration-runner:bootJar):
  python3 postgres/runtime/provision-governance-external-dev.py \
    --env-file .env.governance-dev \
    --jar migration-runner/build/libs/account-migration-runner.jar [--apply]

The parent must ignore .env.governance-dev before execution. With no --apply,
preflight reads only metadata and validates previously provisioned targets.
New independent owner/app secrets are atomically persisted in an owned mode-600
file before mutation; all secret transport is via stdin. Repeated execution
preserves passwords and requires matching ownership and authenticated roles.
Apply resumes pending migrations with Flyway's applied-checksum validation;
read-only preflight requires fully current history. Failure preserves DBs and
saved credentials for forward recovery;
rollback stops the new applications, never drops databases or rewinds history.
"""
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

# Reuse the existing redacted process transport and catalog-based ACL publication.
_spec = importlib.util.spec_from_file_location(
    'products_risk_provision_helpers',
    Path(__file__).with_name('provision-products-risk-external-dev.py'))
_helpers = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_helpers)
GateError = _helpers.GateError
require = _helpers.require
run = _helpers.run
assignments = _helpers.assignments
admin = _helpers.admin
env_value = _helpers.env_value
runtime_sql = _helpers.runtime_sql
provision = _helpers.provision
restrict_acl = _helpers.restrict_acl
ROOT, CONTAINER, JRE = _helpers.ROOT, _helpers.CONTAINER, _helpers.JRE
TARGETS = (('budget', 'budget_db'), ('internal-audit', 'internal_audit_db'))
TABLES = {
    'budget_db': ('budget_fiscal_year_controls', 'budget_idempotency_shards',
                  'budget_plans', 'budget_transfers', 'budget_executions'),
    'internal_audit_db': ('rcm_process', 'rcm_risk', 'rcm_control_activity',
                          'eval_design', 'eval_operating',
                          'operating_evaluation_jpa_entity_evidence_file_paths',
                          'eval_deficiency', 'internal_audit_log'),
}


def migrate(jar, context, database, password, prefix, action):
    values = {'MIGRATION_DB_URL': prefix.format(database=database),
              'MIGRATION_DB_USER': database + '_owner',
              'MIGRATION_DB_PASSWORD': password, 'MIGRATION_TARGET_ENV': 'development',
              'MIGRATION_EXPECTED_DATABASE': database,
              'MIGRATION_ALLOW_MIGRATE': 'true', 'MIGRATION_CHANGE_TICKET': 'GH-696'}
    run(['podman', 'run', '--rm', '-i', '--pull=never', '--network=account-network',
         '--cpus=0.50', '--memory=768m', '--pids-limit=128', '--read-only',
         '--cap-drop=ALL', '--security-opt=no-new-privileges',
         '-v', str(jar) + ':/runner.jar:ro', '--entrypoint', 'sh', JRE, '-s'],
        'set -eu\n' + assignments(values) + 'exec java -Xmx256m -jar /runner.jar '
        + '--context=' + context + ' --action=' + action + '\n')


def schema_gate(database):
    expected = ','.join("'" + name + "'" for name in TABLES[database])
    require(admin(f"SELECT count(*) FROM pg_tables WHERE schemaname='public' "
                  f"AND tablename IN ({expected});", database) == str(len(TABLES[database])),
            'Required governance tables are missing')
    _helpers.container_shell((ROOT / 'postgres/runtime/check-runtime-schemas.sh').read_text(),
                             {'ACCOUNT_DATABASES': database})


def verify_owner_login(database, password, host, port):
    result = _helpers.container_shell(
        'psql -X -qAt -v ON_ERROR_STOP=1 --host "$PGHOST" --port "$PGPORT" '
        '--username "$PGUSER" --dbname "$PGDATABASE" '
        "<<'ACCOUNT_SQL'\n"
        f"SELECT current_database() = '{database}' AND current_user = '{database}_owner';\n"
        'ACCOUNT_SQL\n',
        {'PGPASSWORD': password, 'PGHOST': host, 'PGPORT': str(port),
         'PGUSER': database + '_owner', 'PGDATABASE': database,
         'PGCONNECT_TIMEOUT': '10'})
    require(result.stdout.strip() == 't', 'Existing owner credentials do not authenticate')


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
    require(len(result) == 7 and result[0] == 't' and int(result[1]) >= len(TABLES[database])
            and all(item == 't' for item in result[2:]), 'Runtime isolation gate failed')
    denied = runtime_sql(database, password, host, port,
                         'BEGIN; CREATE TABLE public.account_696_ddl_probe(id integer); ROLLBACK;', False)
    require(denied.returncode != 0 and 'permission denied for schema public' in denied.stderr,
            'Runtime DDL denial gate failed')
    return int(result[1])


def execute(env_path, jar, apply, host, port):
    require(jar.is_file(), 'Build the migration-runner jar before execution')
    require(env_path.name == '.env.governance-dev', 'Use the dedicated governance env file')
    require(env_path.parent == ROOT, 'Keep the dedicated env file in the isolated worktree root')
    require(run(['git', '-C', str(ROOT), 'check-ignore', '--quiet', str(env_path)],
                check=False).returncode == 0, 'Ignore the dedicated env file before execution')
    require(run(['git', '-C', str(ROOT), 'ls-files', '--', str(env_path)]).stdout == '',
            'The dedicated env file must not be tracked')
    run(['podman', 'image', 'exists', JRE])
    lock_fd = os.open(env_path.parent, os.O_RDONLY | os.O_DIRECTORY)
    fd = None
    try:
        fcntl.flock(lock_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        lines, info = [], None
        if os.path.lexists(env_path):
            fd = os.open(env_path, os.O_RDONLY | os.O_NOFOLLOW)
            fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
            info = os.fstat(fd)
            require(stat.S_ISREG(info.st_mode) and stat.S_IMODE(info.st_mode) == 0o600
                    and info.st_uid == os.getuid() and info.st_nlink == 1,
                    'Environment file must be an owned mode-600 regular file')
            lines = os.read(fd, info.st_size + 1).decode('utf-8').splitlines()
        allowed = {context.replace('-', '_').upper() + suffix
                   for context, _ in TARGETS
                   for suffix in ('_DB_URL', '_DB_USER', '_DB_PASSWORD', '_DB_OWNER_PASSWORD')}
        require(all(not line.strip() or line.startswith('#') or line.split('=', 1)[0] in allowed
                    for line in lines), 'The dedicated env file contains unrelated keys')
        prefix = f'jdbc:postgresql://{host}:{port}/' + '{database}'
        require(int(admin('SHOW server_version_num;')) >= 150000, 'PostgreSQL 15+ required')
        require(admin("SELECT count(*) FROM pg_extension WHERE extname = 'pgaudit';") == '0',
                'Audit extension needs a separately reviewed secret injection path')
        catalog_sql = ("SELECT oid, datname, datdba, datacl FROM pg_database "
                       "WHERE datname NOT IN ('budget_db','internal_audit_db') ORDER BY oid;")
        baseline = admin(catalog_sql)
        identity = run(['podman', 'inspect', CONTAINER, '--format',
                        '{{.Id}} {{.Image}} {{json .Mounts}}']).stdout
        pending, targets = {}, []
        for context, database in TARGETS:
            key = context.replace('-', '_').upper()
            for name, value in {key + '_DB_URL': prefix.format(database=database),
                                key + '_DB_USER': database + '_app'}.items():
                current = env_value(lines, name)
                require(not current or current == value, 'Existing target connection differs')
                if not current:
                    pending[name] = value
            owner = env_value(lines, key + '_DB_OWNER_PASSWORD')
            app = env_value(lines, key + '_DB_PASSWORD')
            roles = int(admin(f"SELECT count(*) FROM pg_roles WHERE rolname IN ('{database}_owner','{database}_app');"))
            exists = admin(f"SELECT count(*) FROM pg_database WHERE datname='{database}';") == '1'
            require(not (roles or exists) or (roles == 2 and exists and owner and app),
                    'Existing or partial target requires matching saved credentials and complete role/database state')
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
            if exists:
                require(admin("SELECT count(*) FROM pg_namespace n JOIN pg_roles r ON r.oid=n.nspowner "
                              f"WHERE n.nspname='public' AND r.rolname NOT IN ('{database}_owner', 'pg_database_owner');",
                              database) == '0', 'Existing public schema ownership differs')
                verify_owner_login(database, owner, host, port)
                require(runtime_sql(database, app, host, str(port), 'SELECT 1;').stdout.strip() == '1',
                        'Existing runtime credentials do not authenticate')
                # Read-only validation rejects pending migrations. Apply must allow
                # an interrupted bootstrap to continue: Flyway migrate validates
                # applied checksums before executing the remaining migrations.
                if not apply:
                    migrate(jar, context, database, owner, prefix, 'validate')
            targets.append((context, database, owner, app, exists))
        print('Preflight PASS: budget_db/internal_audit_db only; existing state preserved', flush=True)
        if not apply:
            return
        if pending:
            updated = [line for line in lines if line.split('=', 1)[0] not in pending]
            updated.extend(name + '=' + value for name, value in pending.items())
            if info:
                current = os.stat(env_path, follow_symlinks=False)
                require((current.st_dev, current.st_ino, current.st_size, current.st_mtime_ns)
                        == (info.st_dev, info.st_ino, info.st_size, info.st_mtime_ns),
                        'Environment file changed during preflight')
            else:
                require(not os.path.lexists(env_path), 'Environment file appeared during preflight')
            temporary = None
            try:
                with tempfile.NamedTemporaryFile(mode='w', dir=env_path.parent,
                                                 prefix='.env.governance-dev.', delete=False) as output:
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
        print('Dedicated development credentials saved securely before mutation', flush=True)
        for context, database, owner, app, exists in targets:
            if not exists:
                provision(database, owner, app)
            restrict_acl(database)
            migrate(jar, context, database, owner, prefix, 'migrate')
            restrict_acl(database)
            migrate(jar, context, database, owner, prefix, 'validate')
            schema_gate(database)
            tables = verify(database, app, host, str(port))
            print(f'{context}: migrate/validate/login/ACL/DDL PASS; public tables={tables}', flush=True)
        require(baseline == admin(catalog_sql), 'Unrelated database catalog changed')
        require(identity == run(['podman', 'inspect', CONTAINER, '--format',
                                 '{{.Id}} {{.Image}} {{json .Mounts}}']).stdout,
                'PostgreSQL container/image/mount identity changed')
        print('PASS: 2/2 governance databases; unrelated database catalog/container/mounts unchanged', flush=True)
    finally:
        if fd is not None:
            os.close(fd)
        os.close(lock_fd)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env-file', type=Path, required=True)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--host', choices=['account-postgres'], default='account-postgres')
    parser.add_argument('--port', type=int, choices=[5432], default=5432)
    parser.add_argument('--apply', action='store_true', help='Apply the approved GH-696 dev bootstrap')
    args = parser.parse_args()
    try:
        execute(args.env_file.absolute(), args.jar.resolve(), args.apply, args.host, args.port)
    except GateError as error:
        print('FAIL: ' + str(error), flush=True)
        return 1
    except (OSError, ValueError, subprocess.SubprocessError):
        print('FAIL: governance development gate stopped; diagnostic values/output redacted', flush=True)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
