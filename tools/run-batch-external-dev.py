#!/usr/bin/env python3
"""Run one real business batch at a time, checking metadata and synthetic outputs.

Closing ECL consumes a finalized Risk summary, so the all-jobs order explicitly
places provisioning after ECL calculation. Completed identifying parameters are
verified and reused; they are never silently replaced with a new timestamp.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import hashlib
import json
import os
from pathlib import Path
import sys
import time
import uuid

from business_batch_support import (BatchError, DATE, ROOT, assert_engine_idle, assert_query, clear_pending, command,
    configure_transport, database_inputs, execution_lock, load_inputs, mark_pending, query, recover_pending, seed_path)


@dataclass(frozen=True)
class Job:
    context: str
    project: str
    name: str
    params: dict[str, str]
    result_context: str
    result_file: str = "result.sql"


# Boot merges explicit parameters after RunIdIncrementer output. Pin run.id only
# on jobs declaring that incrementer so first launches and retries share identity.
JOBS = {
    "balances": Job("journal-ledger", "journal-ledger/batch", "dailyBalanceReaggregationJob",
                    {"startDate": DATE, "endDate": DATE}, "journal-ledger"),
    "fx": Job("closing", "closing/batch", "fxValuationJob",
              {"valuationDate": DATE, "valuationBatchId": "6900001"}, "journal-ledger", "result-fx.sql"),
    "deposit": Job("deposit", "deposit/batch", "depositAccountIntegrityJob", {"asOfDate": DATE, "run.id": "6900001"}, "deposit"),
    "loan-interest": Job("loan", "loan/batch", "loanInterestAccrualJob", {"accrualDate": DATE}, "loan"),
    "loan-repayment": Job("loan", "loan/batch", "loanScheduledRepaymentJob", {"repaymentDate": DATE}, "loan", "result-repayment.sql"),
    "depreciation": Job("asset-lease", "asset-lease/batch", "assetDepreciationJob", {"targetDate": DATE}, "asset-lease"),
    "ecl": Job("ecl", "ecl/ecl-batch", "allowanceEclJob",
               {"baseDate": DATE, "runId": "GH690", "modelVersion": "GH690"}, "ecl"),
    "mart": Job("account-mart", "account-mart/mart-batch", "integratedPositionEtlJob", {"baseDate": DATE, "run.id": "6900001"}, "account-mart"),
    "reconciliation": Job("reconciliation", "reconciliation/batch", "reconciliationDailyJob",
                          {"reconciliationDate": DATE, "runBy": "GH690", "deepMode": "false", "run.id": "6900001"}, "reconciliation"),
    "provision": Job("closing", "closing/batch", "eclProvisionJob",
                     {"closingDate": DATE, "provisionBatchId": "6900001"}, "journal-ledger", "result-ecl.sql"),
}


def runtime_properties(job: Job, values):
    pg = database_inputs(values, job.context)
    url = f"jdbc:postgresql://{pg['PGHOST']}:{pg['PGPORT']}/{pg['PGDATABASE']}?sslmode={pg['PGSSLMODE']}"
    properties = {
        "spring.profiles.active": "dev", "spring.main.web-application-type": "none",
        "spring.config.import": "", "spring.cloud.config.enabled": False,
        "spring.cloud.vault.enabled": False, "eureka.client.enabled": False,
        "spring.cloud.discovery.enabled": False, "spring.cloud.service-registry.auto-registration.enabled": False,
        "spring.datasource.url": url, "spring.datasource.username": pg["PGUSER"],
        "spring.datasource.password": pg["PGPASSWORD"], "spring.datasource.driver-class-name": "org.postgresql.Driver",
        "spring.datasource.hikari.maximum-pool-size": 2, "spring.datasource.hikari.minimum-idle": 0,
        "spring.datasource.hikari.connection-timeout": 15000, "spring.datasource.hikari.initialization-fail-timeout": 15000,
        "spring.jpa.hibernate.ddl-auto": "validate", "spring.flyway.enabled": False,
        "spring.flyway.clean-disabled": True, "spring.sql.init.mode": "never",
        "spring.batch.jdbc.initialize-schema": "never", "spring.batch.job.enabled": job.context != "ecl",
        "spring.batch.job.name": job.name, "spring.kafka.listener.auto-startup": False,
        "management.tracing.enabled": False, "logging.level.root": "WARN",
        "logging.config": "file:/account-runtime/logback.xml",
        "account.closing.batch.fx.grid-size": 1, "account.ecl.batch.worker-threads": 1,
        "account.ecl.batch.grid-size": 1, "mart.batch.cdm-event.enabled": False,
        "mart.batch.cdm-load.parallel-enabled": False,
        "journal-ledger.master-data.base-url": "http://minimal-master-data:8082",
        "account.loan.remote.enabled": True,
        "account.loan.journal-base-url": "http://account-accounting-external-dev-journal-ledger-api-1:8080",
        "account.loan.master-data-base-url": "http://minimal-master-data:8082",
        "account.loan.accounting.cash-account-code": "GH690-REPAYCASH",
        "account.loan.accounting.loan-receivable-account-code": "GH690-LOAN",
        "account.loan.accounting.accrued-interest-receivable-account-code": "115010",
        "account.loan.accounting.interest-income-account-code": "410100",
        "account.deposit.remote.enabled": True,
        "account.deposit.journal-base-url": "http://account-accounting-external-dev-journal-ledger-api-1:8080",
        "account.deposit.master-data-base-url": "http://minimal-master-data:8082",
        "reconciliation.journal-ledger.remote.enabled": True,
        "reconciliation.journal-ledger.base-url": "http://account-accounting-external-dev-journal-ledger-api-1:8080",
    }
    if job.context == "loan":
        # Loan's JPA paging reader, outer chunk, and per-loan REQUIRES_NEW
        # transaction can hold three connections concurrently. Keep the pool
        # bounded while allowing the inner transaction to acquire its connection.
        properties["spring.datasource.hikari.maximum-pool-size"] = 3
    # A 0.5-CPU cold provider can persist a write before its response arrives.
    # Allow bounded response time in this verification runtime; never retry HTTP writes.
    for prefix in ("account.loan.http", "account.deposit.http", "reconciliation.journal-ledger"):
        properties[prefix + ".connect-timeout"] = "10s"
        properties[prefix + ".read-timeout"] = "60s"
    if job.context == "closing":
        properties.update({
            "closing.journal-ledger.connect-timeout": "10s",
            "closing.journal-ledger.read-timeout": "60s",
            "closing.sources.enabled": True,
            "closing.journal-ledger.base-url": "http://account-accounting-external-dev-journal-ledger-api-1:8080",
            "account.closing.accounting.fx-translation-gain-account-code": "GH690-FXGAIN",
            "account.closing.accounting.fx-translation-loss-account-code": "GH690-FXLOSS",
            "account.closing.accounting.auto-post-adjustments": False,
            "account.closing.accounting.provision-rules.ECL.debit-account-code": "GH690-BADDEBT",
            "account.closing.accounting.provision-rules.ECL.credit-account-code": "GH690-ALLOWANCE",
            "account.closing.accounting.provision-rules.ECL.amount": "4808.5714",
        })
        for name, context in (("journal", "journal-ledger"), ("ecl", "ecl"), ("master-data", "master-data")):
            source = database_inputs(values, context)
            prefix = f"closing.sources.{name}"
            properties[prefix + ".url"] = f"jdbc:postgresql://{source['PGHOST']}:{source['PGPORT']}/{source['PGDATABASE']}?sslmode={source['PGSSLMODE']}"
            properties[prefix + ".username"] = source["PGUSER"]
            properties[prefix + ".password"] = source["PGPASSWORD"]
    return properties


def parameter_type(name):
    return "java.lang.Long" if name in {"valuationBatchId", "provisionBatchId", "run.id"} else "java.lang.String"


def metadata(engine, values, job):
    # Spring Batch identity includes types and identifying flags, not just values.
    # Nonidentifying diagnostics may vary without creating a different instance.
    predicates = " AND ".join(
        "EXISTS (SELECT 1 FROM batch_job_execution_params p WHERE p.job_execution_id=e.job_execution_id "
        f"AND p.parameter_name='{key}' AND p.parameter_value='{value}' "
        f"AND p.parameter_type='{parameter_type(key)}' AND p.identifying='Y')"
        for key, value in job.params.items())
    return query(engine, values, job.context,
        "SELECT e.job_execution_id||'|'||e.status||'|'||e.exit_code FROM batch_job_execution e "
        "JOIN batch_job_instance i USING(job_instance_id) "
        f"WHERE i.job_name='{job.name}' AND {predicates} "
        "AND (SELECT count(*) FROM batch_job_execution_params p "
        "WHERE p.job_execution_id=e.job_execution_id AND p.identifying='Y')="
        f"{len(job.params)} ORDER BY e.job_execution_id DESC LIMIT 1;")


def assert_loan_lineage(engine, values, key):
    """Compare exact cross-context references in memory without logging row values."""
    if key == "loan-interest":
        local = ("SELECT journal_entry_id||'|'||journal_no FROM loan_accrual_log "
                 f"WHERE loan_id=6900001 AND accrual_date='{DATE}' AND status='SUCCESS'")
        remote = "lineage_source_type='LOAN' AND lineage_source_id='6900001'"
    else:
        local = ("SELECT journal_entry_id||'|'||journal_entry_slip_no FROM loan_events "
                 f"WHERE loan_id=6900001 AND event_date='{DATE}' AND event_type='SCHEDULED_REPAYMENT'")
        remote = ("lineage_source_type='LOAN_SCHEDULED_REPAYMENT' "
                  f"AND lineage_source_id='6900001:{DATE}'")
    # Both result SQL gates have already checked amounts and posting status.
    # Require exactly one non-null pair on each side, including the same slip.
    actual = query(engine, values, "loan", local + ";")
    expected = query(engine, values, "journal-ledger",
                     "SELECT id||'|'||slip_no FROM journal_entries WHERE " + remote +
                     f" AND accounting_date='{DATE}' AND status='POSTED' AND currency_code='KRW';")
    if not actual or "\n" in actual or actual != expected:
        raise BatchError(f"{key}: local and remote journal references do not match")
    return True


def scope_check(engine, values, key):
    # Some existing jobs scan all active/historical rows, not just a date. Never
    # permit a synthetic run to mutate or reconcile an unrelated business row.
    scopes = {
        # Reaggregation clears every balance on the date before reading journals,
        # including orphan balances. Only the fixture's exact balance keys are owned.
        "balances": ("journal-ledger", f"NOT EXISTS(SELECT 1 FROM journal_entries WHERE accounting_date='{DATE}' AND NOT ("
                     "(COALESCE(lineage_source_type,'')='GH690' AND COALESCE(lineage_source_id,'')=COALESCE(slip_no,'') AND ("
                     "(COALESCE(id,0)=6900001 AND COALESCE(slip_no,'')='GH690-KRW-001') OR "
                     "(COALESCE(id,0)=6900002 AND COALESCE(slip_no,'')='GH690-USD-001') OR "
                     "(COALESCE(id,0)=6900003 AND COALESCE(slip_no,'')='GH690-DRAFT-001'))) OR "
                     "(COALESCE(lineage_source_type,'')='LOAN' AND COALESCE(lineage_source_id,'')='6900001') OR "
                     "(COALESCE(lineage_source_type,'')='LOAN_SCHEDULED_REPAYMENT' "
                     f"AND COALESCE(lineage_source_id,'')='6900001:{DATE}') OR "
                     "(COALESCE(lineage_source_type,'')='FX_VALUATION' "
                     "AND COALESCE(lineage_source_id,'') IN ('6900001|GH690-FX|USD','6900001|GH690-FXLIAB|USD')) OR "
                     "(COALESCE(lineage_source_type,'')='ECL_PROVISION' "
                     "AND COALESCE(lineage_source_id,'')='6900001|GH690-ALLOWANCE|KRW')))"
                     + " AND " + " AND ".join(
                         f"NOT EXISTS(SELECT 1 FROM {table} WHERE balance_date='{DATE}' AND NOT ("
                         "COALESCE(period,'')='2090-01' AND ("
                         "(COALESCE(account_code,'') IN ('GH690-CASH','GH690-EQUITY') AND COALESCE(currency_code,'')='KRW') OR "
                         "(COALESCE(account_code,'') IN ('GH690-FX','GH690-FXLIAB') AND COALESCE(currency_code,'')='USD'))"
                         + (" AND COALESCE(bp_code,'')='GH690-CUSTOMER' AND COALESCE(dept_code,'')='GH690-DEPT'"
                            if table == "sl_balances" else "") + "))"
                         for table in ("gl_balances", "sl_balances"))),
        "fx": ("journal-ledger", f"NOT EXISTS(SELECT 1 FROM journal_entries WHERE status='POSTED' AND currency_code<>'KRW' AND accounting_date<='{DATE}' AND slip_no NOT LIKE 'GH690%')"),
        "depreciation": ("asset-lease", "NOT EXISTS(SELECT 1 FROM fixed_assets WHERE status='ACTIVE' AND asset_code NOT LIKE 'GH690%')"),
        "loan-interest": ("loan", f"NOT EXISTS(SELECT 1 FROM eir_amortization_schedules s JOIN loans l ON l.id=s.loan_id WHERE s.schedule_date='{DATE}' AND l.loan_number NOT LIKE 'GH690%')"),
        "loan-repayment": ("loan", f"NOT EXISTS(SELECT 1 FROM eir_amortization_schedules s JOIN loans l ON l.id=s.loan_id WHERE s.schedule_date='{DATE}' AND l.loan_number NOT LIKE 'GH690%')"),
        "reconciliation": ("reconciliation", "NOT EXISTS(SELECT 1 FROM reconciliation_units WHERE is_active AND id<>6900001)"),
        "ecl": ("ecl", f"NOT EXISTS(SELECT 1 FROM cr_accounts WHERE is_active AND account_no NOT LIKE 'GH690%') AND NOT EXISTS(SELECT 1 FROM allowance_exposure_snapshots WHERE base_date='{DATE}' AND exposure_id NOT LIKE 'GH690%') AND NOT EXISTS(SELECT 1 FROM allowance_ecl_results r JOIN cr_accounts a ON a.id=r.account_id WHERE r.base_date='{DATE}' AND a.account_no NOT LIKE 'GH690%')"),
        "mart": ("account-mart", f"NOT EXISTS(SELECT 1 FROM ods_acc_ledger WHERE is_active AND acc_no NOT LIKE 'GH690%') AND NOT EXISTS(SELECT 1 FROM allowance_input_positions WHERE base_dt='{DATE}' AND COALESCE(acc_no,'') NOT LIKE 'GH690%') AND NOT EXISTS(SELECT 1 FROM allowance_exposure_snapshots WHERE base_date='{DATE}' AND (COALESCE(exposure_id,'') NOT LIKE 'GH690%' OR COALESCE(source_account_no,'') NOT LIKE 'GH690%'))"),
        # The Closing adapter aggregates all summaries for the date, including
        # different exposure accounts sharing the same run/model and posting key.
        "provision": ("ecl", f"NOT EXISTS(SELECT 1 FROM allowance_summary WHERE base_date='{DATE}' AND NOT ("
                      "COALESCE(run_id,'')='GH690' AND COALESCE(model_version,'')='GH690' "
                      "AND COALESCE(legal_entity_code,'')='GH690' AND COALESCE(currency_code,'')='KRW' "
                      "AND COALESCE(exposure_account_code,'')='GH690-LOAN' "
                      "AND COALESCE(allowance_account_code,'')='GH690-ALLOWANCE' "
                      "AND COALESCE(bad_debt_expense_account_code,'')='GH690-BADDEBT' "
                      "AND COALESCE(reversal_income_account_code,'')='GH690-REVERSAL'))"),
    }
    if key in scopes:
        context, predicate = scopes[key]
        if query(engine, values, context, f"SELECT {predicate};") != "t":
            raise BatchError(f"{key}: unrelated source rows fall within this job's scope")


def run_job(engine, values, key, timeout):
    job = JOBS[key]
    before = metadata(engine, values, job)
    if before.endswith("|COMPLETED|COMPLETED"):
        assert_query(engine, values, job.result_context, seed_path(job.result_context, job.result_file))
        if key.startswith("loan-"):
            assert_query(engine, values, "journal-ledger", seed_path("journal-ledger", f"result-{key}.sql"))
            assert_loan_lineage(engine, values, key)
        print(f"PASS {key}: existing completed instance {before.split('|')[0]}, output assertions", flush=True)
        return
    if before and before.split("|")[1] in {"STARTING", "STARTED", "STOPPING", "UNKNOWN"}:
        raise BatchError(f"{key}: unresolved previous execution requires inspection")
    scope_check(engine, values, key)
    assert_query(engine, values, job.context, seed_path(job.context, "verify.sql"))
    if key == "provision":
        assert_query(engine, values, "ecl", seed_path("ecl", "result.sql"))
    jars = [p for p in (ROOT / job.project / "build/libs").glob("*.jar") if not p.name.endswith("-plain.jar")]
    if len(jars) != 1:
        raise BatchError(f"{key}: build exactly one bootJar first")
    jar = jars[0]
    fingerprint = hashlib.sha256(jar.read_bytes()).hexdigest()
    name = f"account-690-{key}-{uuid.uuid4().hex[:8]}"
    env = os.environ.copy()
    env["SPRING_APPLICATION_JSON"] = json.dumps(runtime_properties(job, values))
    env["JAVA_TOOL_OPTIONS"] = "-Xms64m -Xmx448m -XX:MaxMetaspaceSize=160m -XX:ActiveProcessorCount=1"
    args = [engine, "run", "--detach", "--name", name, "--label", "account.issue=690",
            "--network", "account-network", "--cpus", "0.5", "--memory", "768m", "--pids-limit", "256",
            "--user", "1000:1000",
            "--read-only", "--tmpfs", "/tmp:rw,size=64m", "--cap-drop", "ALL",
            "--security-opt", "no-new-privileges", "--env", "SPRING_APPLICATION_JSON", "--env", "JAVA_TOOL_OPTIONS",
            "-v", f"{jar}:/app/app.jar:ro", "-v", f"{ROOT}/tools/logback-batch-console.xml:/account-runtime/logback.xml:ro",
            "--entrypoint", "java", "docker.io/library/eclipse-temurin:17-jre-alpine", "-jar", "/app/app.jar",
            f"--spring.batch.job.name={job.name}"]
    if job.context == "ecl":
        args += ["job.name=" + job.name]
    args += [f"{key}={value}" + (",java.lang.Long" if parameter_type(key) == "java.lang.Long" else "")
             for key, value in job.params.items()]
    print(f"START {key}: CPU0.5 RAM768MiB sha256={fingerprint[:16]} container={name}", flush=True)
    deadline = time.monotonic() + timeout
    mark_pending(name)
    try:
        command(args, env=env, timeout=300)
        limits = command([engine, "inspect", "--format",
                          "{{.HostConfig.Memory}} {{.HostConfig.NanoCpus}} {{.HostConfig.CpuQuota}} {{.HostConfig.CpuPeriod}}", name])
        try:
            memory, nano, quota, period = map(int, limits.split())
            cpu = nano / 1e9 if nano else (quota / period if period > 0 else 0)
        except ValueError:
            raise BatchError(f"{key}: resource configuration could not be verified") from None
        if memory != 768 * 1024 * 1024 or cpu != 0.5:
            raise BatchError(f"{key}: actual container resource limits differ from the required budget")
        while True:
            state = command([engine, "inspect", "--format", "{{.State.Running}} {{.State.ExitCode}} {{.State.OOMKilled}}", name])
            if state.startswith("false "):
                if state != "false 0 false":
                    raise BatchError(f"{key}: runtime exit/OOM gate failed (logs suppressed)")
                break
            if time.monotonic() >= deadline:
                raise BatchError(f"{key}: runtime deadline exceeded")
            time.sleep(3)
        after = metadata(engine, values, job)
        if after == before or not after.endswith("|COMPLETED|COMPLETED"):
            raise BatchError(f"{key}: no new COMPLETED metadata execution")
        assert_query(engine, values, job.result_context, seed_path(job.result_context, job.result_file))
        if key.startswith("loan-"):
            assert_query(engine, values, "journal-ledger", seed_path("journal-ledger", f"result-{key}.sql"))
            assert_loan_lineage(engine, values, key)
        print(f"PASS {key}: execution={after.split('|')[0]} COMPLETED, outputs, exit0/OOMfalse", flush=True)
        clear_pending()
    except BaseException as failure:
        # A durable marker survives kill -9 or an ambiguous engine response.
        # Never launch another job until this exact container is confirmed stopped.
        try:
            recover_pending(engine)
        except BatchError:
            # Engine cleanup can itself time out. Keep the original safe cause
            # visible so a financial/runtime failure is not mistaken for latency.
            cause = str(failure) if isinstance(failure, BatchError) else f"{key}: execution interrupted"
            raise BatchError(f"{cause}; cleanup unconfirmed, pending record retained") from None
        raise


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--job", choices=(*JOBS, "all"), default="all")
    parser.add_argument("--env-file", type=Path, required=True)
    parser.add_argument("--engine", choices=("podman", "docker"), default="podman")
    parser.add_argument("--podman-socket")
    parser.add_argument("--timeout", type=int, default=900)
    parser.add_argument("--recover", action="store_true", help="stop the pending issue-owned container; preserve job metadata")
    args = parser.parse_args(argv)
    try:
        configure_transport(args.podman_socket)
        with execution_lock():
            if args.recover:
                recover_pending(args.engine)
                print("PASS pending launch resolved (container stopped or confirmed absent); inspect job metadata and remote reservations before rerun")
                return 0
            assert_engine_idle(args.engine)
            values = load_inputs(args.env_file)
            for key in JOBS if args.job == "all" else (args.job,):
                run_job(args.engine, values, key, args.timeout)
        return 0
    except BatchError as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
