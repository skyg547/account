#!/bin/sh
set -eu

check_database() {
    label=$1
    jdbc_url=$2
    username=$3
    password=$4

    database=$(printf '%s' "$label" | tr '-' '_')_dev
    if [ "$username" != "${database}_app" ]; then
        echo "Canonical runtime role required for $label" >&2
        return 1
    fi
    case "$jdbc_url" in
        jdbc:postgresql://*/*) ;;
        *)
            echo "Invalid PostgreSQL URL contract for $label" >&2
            return 1
            ;;
    esac

    connection=${jdbc_url#jdbc:postgresql://}
    authority=${connection%%/*}
    target=${connection#*/}
    case "$authority" in
        ''|*'@'*|*'%'*|*'?'*|*'#'*|*' '*|*'\'*)
            echo "Invalid credential-free PostgreSQL authority for $label" >&2
            return 1 ;;
    esac
    case "$target" in
        "$database"|"$database?sslmode=disable"|"$database?sslmode=prefer"|"$database?sslmode=require") ;;
        *) echo "Canonical PostgreSQL database and supported sslmode required for $label" >&2; return 1 ;;
    esac
    connection_uri=${jdbc_url#jdbc:}

    if ! result=$(PGCONNECT_TIMEOUT=10 PGPASSWORD="$password" psql \
        --dbname "$connection_uri" \
        --username "$username" \
        --no-psqlrc \
        --tuples-only \
        --no-align \
        --quiet \
        --set=ON_ERROR_STOP=1 \
        --command "SELECT CASE WHEN
            current_database() = '$database'
            AND current_user = '${database}_app'
            AND has_database_privilege(current_user, current_database(), 'CONNECT')
            AND has_schema_privilege(current_user, 'public', 'USAGE')
            AND NOT has_schema_privilege(current_user, 'public', 'CREATE')
            AND NOT has_database_privilege(current_user, current_database(), 'CREATE,TEMP')
            AND NOT EXISTS (
                SELECT 1 FROM pg_roles WHERE rolname = current_user
                  AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
            )
            AND NOT EXISTS (
                SELECT 1 FROM pg_auth_members
                WHERE member = (SELECT oid FROM pg_roles WHERE rolname = current_user)
            )
            AND to_regclass('public.flyway_schema_history') IS NOT NULL
            AND to_regclass('public.flyway_schema_history_batch') IS NOT NULL
            AND EXISTS (
                SELECT 1 FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename NOT LIKE 'flyway\_schema\_history%' ESCAPE '\\'
            )
            AND NOT EXISTS (
                SELECT 1 FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename NOT LIKE 'flyway\_schema\_history%' ESCAPE '\\'
                  AND NOT (
                      has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'SELECT')
                      AND has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'INSERT')
                      AND has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'UPDATE')
                      AND has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'DELETE')
                  )
            )
            AND NOT EXISTS (
                SELECT 1 FROM pg_class sequence
                JOIN pg_namespace namespace ON namespace.oid = sequence.relnamespace
                WHERE namespace.nspname = 'public'
                  AND sequence.relkind = 'S'
                  AND NOT (
                      has_sequence_privilege(current_user, sequence.oid, 'USAGE')
                      AND has_sequence_privilege(current_user, sequence.oid, 'SELECT')
                  )
            )
            AND NOT EXISTS (
                SELECT 1 FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename LIKE 'flyway\_schema\_history%' ESCAPE '\\'
                  AND (
                      has_table_privilege(current_user, format('%I.%I', schemaname, tablename),
                          'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
                      OR has_any_column_privilege(current_user, format('%I.%I', schemaname, tablename),
                          'SELECT,INSERT,UPDATE,REFERENCES')
                  )
            )
            AND NOT EXISTS (
                SELECT 1 FROM pg_tables WHERE schemaname = 'public'
                  AND (tableowner = current_user OR has_table_privilege(current_user,
                      format('%I.%I', schemaname, tablename),
                      'TRUNCATE,REFERENCES,TRIGGER,SELECT WITH GRANT OPTION,INSERT WITH GRANT OPTION,UPDATE WITH GRANT OPTION,DELETE WITH GRANT OPTION'))
            )
            AND NOT EXISTS (
                SELECT 1 FROM pg_class s JOIN pg_namespace n ON n.oid = s.relnamespace
                WHERE n.nspname = 'public' AND s.relkind = 'S'
                  AND has_sequence_privilege(current_user, s.oid,
                      'UPDATE,USAGE WITH GRANT OPTION,SELECT WITH GRANT OPTION')
            )
            THEN 1 ELSE 0 END" 2>/dev/null); then
        echo "External development database connection failed for $label" >&2
        return 1
    fi

    if [ "$result" != "1" ]; then
        echo "External development database privilege gate failed for $label" >&2
        return 1
    fi
}

