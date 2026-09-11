"""Offline safety and restart tests; all process/DB operations are mocked."""
import contextlib
import importlib.util
import io
import re
from pathlib import Path
import subprocess
import sqlite3
import sys
import tempfile
import unittest
from unittest.mock import patch
from urllib.parse import urlsplit

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))
import business_batch_support as support


def load_script(name, filename):
    spec = importlib.util.spec_from_file_location(name, TOOLS / filename)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module  # dataclasses resolves postponed annotations here
    spec.loader.exec_module(module)
    return module


seed = load_script('business_batch_seed_test_target', 'seed-external-dev.py')
runner = load_script('business_batch_runner_test_target', 'run-batch-external-dev.py')
SYNTHETIC_PASSWORD = 'offline-test-only-value'


def inputs(context='deposit'):
    prefix, database = support.CONTEXTS[context]
    return {f'{prefix}_DB_URL': f'jdbc:postgresql://localhost:5432/{database}_dev',
            f'{prefix}_DB_USER': f'{database}_dev_app',
            f'{prefix}_DB_PASSWORD': SYNTHETIC_PASSWORD}


class OfflineTestCase(unittest.TestCase):
    def setUp(self):
        self.offline = contextlib.ExitStack()
        self.addCleanup(self.offline.close)
        directory = self.offline.enter_context(tempfile.TemporaryDirectory())
        self.offline.enter_context(patch.object(support, 'PENDING', Path(directory) / 'pending'))
        self.offline.enter_context(patch.object(support.subprocess, 'run', side_effect=AssertionError('unmocked subprocess forbidden')))


class InputAndProcessTests(OfflineTestCase):
    def test_env_input_accepts_private_synthetic_file_and_redacts_invalid_content(self):
        with tempfile.NamedTemporaryFile(mode='w+', encoding='utf-8') as stream:
            stream.write('DEPOSIT_DB_PASSWORD=' + SYNTHETIC_PASSWORD + '\n')
            stream.flush()
            self.assertEqual(support.load_inputs(Path(stream.name))['DEPOSIT_DB_PASSWORD'], SYNTHETIC_PASSWORD)
            stream.seek(0)
            stream.truncate()
            stream.write(SYNTHETIC_PASSWORD + '\n')
            stream.flush()
            with self.assertRaises(support.BatchError) as failure:
                support.load_inputs(Path(stream.name))
            self.assertNotIn(SYNTHETIC_PASSWORD, str(failure.exception))
        with self.assertRaises(support.BatchError):
            support.load_inputs(Path(stream.name))

    def test_all_contexts_require_development_database_and_runtime_role(self):
        for context, (prefix, database) in support.CONTEXTS.items():
            with self.subTest(context=context):
                values = inputs(context)
                self.assertEqual(support.database_inputs(values, context)['PGDATABASE'], database + '_dev')
                for key, invalid in ((f'{prefix}_DB_URL', f'jdbc:postgresql://localhost/{database}'),
                                     (f'{prefix}_DB_USER', 'postgres'),
                                     (f'{prefix}_DB_PASSWORD', '')):
                    with self.assertRaises(support.BatchError) as failure:
                        support.database_inputs(dict(values, **{key: invalid}), context)
                    self.assertNotIn(SYNTHETIC_PASSWORD, str(failure.exception))

    def test_malformed_or_ambiguous_uri_is_rejected(self):
        for suffix in ('?sslmode=', '?unknown=', '?sslmode=require&sslmode=disable',
                       '?sslmode=unknown', '?sslmode', '?options=-csearch_path=x', '#fragment'):
            with self.subTest(suffix=suffix):
                values = inputs()
                values['DEPOSIT_DB_URL'] += suffix
                with self.assertRaises(support.BatchError):
                    support.database_inputs(values, 'deposit')

    def test_query_passes_credentials_only_in_environment_and_reuses_client(self):
        with patch.object(support, 'command', return_value='t') as command:
            self.assertEqual(support.query('podman', inputs(), 'deposit', 'SELECT TRUE;'), 't')
        args = command.call_args.args[0]
        self.assertNotIn(SYNTHETIC_PASSWORD, repr(args))
        self.assertEqual(command.call_args.kwargs['env']['PGPASSWORD'], SYNTHETIC_PASSWORD)
        self.assertIn('default_transaction_read_only=on', command.call_args.kwargs['env']['PGOPTIONS'])
        self.assertEqual(args[1], 'exec')
        self.assertIn('account-products-external-dev-products-db-check-1', args)

    def test_master_data_reuses_persistent_accounting_checker(self):
        with patch.object(support, 'command', return_value='t') as command:
            support.query('podman', inputs('master-data'), 'master-data', 'SELECT TRUE;')
        self.assertIn('account-accounting-external-dev-accounting-db-check-1', command.call_args.args[0])
        self.assertEqual(command.call_args.kwargs['env']['PGDATABASE'], 'master_data_dev')

    def test_child_failure_output_and_timeout_are_suppressed(self):
        failures = [subprocess.CompletedProcess(['fake'], 1, SYNTHETIC_PASSWORD, SYNTHETIC_PASSWORD),
                    subprocess.TimeoutExpired(['fake', SYNTHETIC_PASSWORD], 1),
                    OSError(SYNTHETIC_PASSWORD)]
        for failure in failures:
            with self.subTest(kind=type(failure).__name__):
                kwargs = {'side_effect': failure} if isinstance(failure, Exception) else {'return_value': failure}
                with patch.object(support.subprocess, 'run', **kwargs), self.assertRaises(support.BatchError) as error:
                    support.command(['fake'])
                self.assertNotIn(SYNTHETIC_PASSWORD, str(error.exception))

    def test_engine_gate_rejects_running_container_and_pending_launch(self):
        with patch.object(support, 'command', return_value='synthetic-container') as command:
            with self.assertRaises(support.BatchError):
                support.assert_engine_idle('podman')
            self.assertIn('label=account.issue=690', command.call_args.args[0])
        support.mark_pending('offline-test')
        with patch.object(support, 'command') as command:
            with self.assertRaises(support.BatchError):
                support.assert_engine_idle('docker')
            command.assert_not_called()
        support.clear_pending()
        with patch.object(support, 'command', return_value=''):
            support.assert_engine_idle('docker')

    def test_recovery_requires_valid_issue_owned_container_before_stop(self):
        cases = [('unrelated-container', '690'), ('account-690-deposit-abcdef01', 'other-issue')]
        for name, label in cases:
            with self.subTest(name=name, label=label):
                support.PENDING.write_text(name + '\n')
                with patch.object(support, 'command', side_effect=[name, label]) as command:
                    with self.assertRaises(support.BatchError):
                        support.recover_pending('podman')
                self.assertFalse(any('stop' in call.args[0] for call in command.call_args_list))
                self.assertTrue(support.PENDING.exists())

    def test_recovery_inventory_failure_retains_marker(self):
        support.mark_pending('account-690-deposit-abcdef01')
        with patch.object(support, 'command', side_effect=support.BatchError('inventory unavailable')) as command:
            with self.assertRaises(support.BatchError):
                support.recover_pending('podman')
        self.assertTrue(support.PENDING.exists())
        command.assert_called_once_with(['podman', 'ps', '--all', '--format', '{{.Names}}'])

    def test_recovery_proves_exact_absence_without_stopping_similar_names(self):
        name = 'account-690-deposit-abcdef01'
        for inventory in ('', name + '-extra', 'prefix-' + name,
                          name + '-extra\nprefix-' + name):
            with self.subTest(inventory=inventory):
                support.PENDING.write_text(name + '\n')
                with patch.object(support, 'command', return_value=inventory) as command:
                    support.recover_pending('podman')
                self.assertFalse(support.PENDING.exists())
                command.assert_called_once_with(['podman', 'ps', '--all', '--format', '{{.Names}}'])

    def test_recovery_clears_only_after_confirmed_stop(self):
        name = 'account-690-deposit-abcdef01'
        for state in ('true', 'false'):
            with self.subTest(state=state):
                support.PENDING.write_text(name + '\n')
                with patch.object(support, 'command', side_effect=[name, '690', 'true', '', state]) as command:
                    if state == 'true':
                        with self.assertRaises(support.BatchError):
                            support.recover_pending('podman')
                    else:
                        support.recover_pending('podman')
                self.assertEqual(support.PENDING.exists(), state != 'false')
                self.assertEqual(command.call_args_list[3].args[0], ['podman', 'stop', '--time', '10', name])
                self.assertTrue(all(call.args[0][-1] == name for call in command.call_args_list[1:]))
        with patch.object(support, 'command') as command, self.assertRaises(support.BatchError):
            support.recover_pending('podman')
        command.assert_not_called()

    def test_recovery_already_stopped_does_not_send_stop(self):
        name = 'account-690-deposit-abcdef01'
        support.mark_pending(name)
        with patch.object(support, 'command', side_effect=[name, '690', 'false']) as command:
            support.recover_pending('podman')
        self.assertFalse(support.PENDING.exists())
        self.assertFalse(any('stop' in call.args[0] for call in command.call_args_list))

    def test_recovery_unknown_state_preserves_marker_without_stop(self):
        name = 'account-690-deposit-abcdef01'
        support.mark_pending(name)
        with patch.object(support, 'command', side_effect=[name, '690', 'unknown']) as command:
            with self.assertRaises(support.BatchError):
                support.recover_pending('podman')
        self.assertTrue(support.PENDING.exists())
        self.assertFalse(any('stop' in call.args[0] for call in command.call_args_list))

    def test_execution_lock_rejects_concurrent_holder_and_releases(self):
        # Redirect the lock to an isolated temporary file, retaining real flock semantics.
        with tempfile.TemporaryDirectory() as directory, patch.object(support, 'Path', return_value=Path(directory) / 'lock'):
            with support.execution_lock():
                with self.assertRaises(support.BatchError):
                    with support.execution_lock():
                        self.fail('second holder entered')
            with support.execution_lock():
                pass


class SeederTests(OfflineTestCase):
    def invoke(self, action='seed', *, mutation_error=False):
        stack = contextlib.ExitStack()
        self.addCleanup(stack.close)
        stack.enter_context(patch.object(seed, 'execution_lock', contextlib.nullcontext))
        stack.enter_context(patch.object(seed, 'configure_transport'))
        stack.enter_context(patch.object(seed, 'assert_engine_idle'))
        stack.enter_context(patch.object(seed, 'load_inputs', return_value=inputs()))
        validate = stack.enter_context(patch.object(seed, 'database_inputs'))
        assertion = stack.enter_context(patch.object(seed, 'assert_query'))
        query = stack.enter_context(patch.object(seed, 'query', side_effect=support.BatchError('transaction rejected') if mutation_error else None))
        stdout, stderr = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            result = seed.main([action, '--package', 'products', '--env-file', '/unused'])
        return result, query, validate, assertion, stdout.getvalue(), stderr.getvalue()

    def test_seed_assertions_are_in_transaction_before_commit(self):
        result, query, validate, assertion, output, _ = self.invoke()
        self.assertEqual(result, 0)
        self.assertEqual([call.args[1] for call in validate.call_args_list], list(support.PACKAGES['products']))
        self.assertEqual(query.call_count, 3)
        # The in-transaction assertion must be the commit gate, with no second
        # connection reopening a race after a verified successful commit.
        assertion.assert_not_called()
        for call in query.call_args_list:
            sql = call.args[3]
            self.assertTrue(sql.startswith('BEGIN;'))
            self.assertLess(sql.index('RAISE EXCEPTION'), sql.rindex('COMMIT;'))
            self.assertIn('COALESCE', sql)
            self.assertFalse(call.kwargs['readonly'])
        self.assertEqual(output.count('PASS seed'), 3)

    def test_transaction_failure_stops_remaining_packages_without_success(self):
        result, query, _, assertion, output, errors = self.invoke(mutation_error=True)
        self.assertEqual(result, 1)
        self.assertEqual(query.call_count, 1)
        assertion.assert_not_called()
        self.assertNotIn('PASS', output)
        self.assertIn('FAIL', errors)

    def test_missing_later_package_assertion_blocks_every_mutation(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for context in support.PACKAGES['products']:
                (root / context).mkdir()
                (root / context / 'seed.sql').write_text('SELECT 1;')
                if context != 'asset-lease':
                    (root / context / 'verify.sql').write_text('SELECT TRUE;')
            with patch.object(seed, 'seed_path', side_effect=lambda context, name: root / context / name):
                result, query, _, assertion, output, errors = self.invoke()
        self.assertEqual(result, 1)
        query.assert_not_called()
        assertion.assert_not_called()
        self.assertNotIn('PASS', output)
        self.assertIn('complete seed package is required', errors)

    def test_verify_is_read_only(self):
        result, query, _, assertion, _, _ = self.invoke('verify')
        self.assertEqual(result, 0)
        query.assert_not_called()
        self.assertEqual(assertion.call_count, 3)


class RunnerTests(OfflineTestCase):
    def setUp(self):
        super().setUp()
        self.stack = contextlib.ExitStack()
        self.addCleanup(self.stack.close)
        root = Path(self.stack.enter_context(tempfile.TemporaryDirectory()))
        jar_dir = root / runner.JOBS['deposit'].project / 'build/libs'
        jar_dir.mkdir(parents=True)
        (jar_dir / 'batch.jar').write_bytes(b'offline synthetic jar')
        self.stack.enter_context(patch.object(runner, 'ROOT', root))
        self.stack.enter_context(patch.object(runner, 'scope_check'))
        self.stack.enter_context(patch.object(runner, 'assert_engine_idle'))
        self.stack.enter_context(patch.object(runner, 'runtime_properties', return_value={'spring.datasource.password': SYNTHETIC_PASSWORD}))
        self.assertion = self.stack.enter_context(patch.object(runner, 'assert_query'))
        self.limits = '805306368 0 50000 100000'
        self.state = 'false 0 false'
        self.command = self.stack.enter_context(patch.object(runner, 'command', side_effect=self.engine_response))
        self.cleanup_command = self.stack.enter_context(patch.object(support, 'command', side_effect=self.engine_response))
        self.output = io.StringIO()
        self.stack.enter_context(contextlib.redirect_stdout(self.output))

    def engine_response(self, args, **kwargs):
        if 'ps' in args:
            return support.PENDING.read_text().strip() if support.PENDING.exists() else ''
        if 'inspect' not in args:
            return 'container-id'
        template = args[args.index('--format') + 1]
        if 'HostConfig' in template:
            return self.limits
        if 'Labels' in template:
            return '690'
        if template == '{{.State.Running}}':
            return 'false'
        return self.state

    def test_actual_resource_limits_must_match_before_metadata_success(self):
        for limits in ('0 0 50000 100000', '805306368 0 100000 100000',
                       '805306368 0 50000 0', '805306368 1000000000 0 0',
                       '805306368 invalid 0 0', '', '805306368 0 50000'):
            with self.subTest(limits=limits):
                self.limits = limits
                with patch.object(runner, 'metadata', return_value='') as metadata:
                    with self.assertRaises(support.BatchError):
                        runner.run_job('podman', {}, 'deposit', 10)
                metadata.assert_called_once()
                self.assertFalse(support.PENDING.exists())
        self.assertNotIn('PASS', self.output.getvalue())

    def test_nano_cpu_resource_encoding_is_accepted(self):
        self.limits = '805306368 500000000 0 0'
        with patch.object(runner, 'metadata', side_effect=['', '8|COMPLETED|COMPLETED']):
            runner.run_job('docker', {}, 'deposit', 10)
        self.assertIn('PASS', self.output.getvalue())

    def test_completed_replay_checks_outputs_without_launch(self):
        with patch.object(runner, 'metadata', return_value='7|COMPLETED|COMPLETED'):
            runner.run_job('podman', {}, 'deposit', 10)
        self.command.assert_not_called()
        self.assertion.assert_called_once()
        self.assertIn('PASS', self.output.getvalue())

    def test_completed_replay_with_invalid_business_results_fails(self):
        self.assertion.side_effect = support.BatchError('invalid business results')
        with patch.object(runner, 'metadata', return_value='7|COMPLETED|COMPLETED'), self.assertRaises(support.BatchError):
            runner.run_job('podman', {}, 'deposit', 10)
        self.command.assert_not_called()
        self.assertNotIn('PASS', self.output.getvalue())

    def test_failed_execution_retries_same_parameters_with_resource_limits(self):
        with patch.object(runner, 'metadata', side_effect=['7|FAILED|FAILED', '8|COMPLETED|COMPLETED']):
            runner.run_job('podman', {}, 'deposit', 10)
        launch = next(call for call in self.command.call_args_list if 'run' in call.args[0])
        args = launch.args[0]
        self.assertEqual(args[args.index('--memory') + 1], '768m')
        self.assertEqual(args[args.index('--cpus') + 1], '0.5')
        self.assertTrue(args[args.index('--name') + 1].startswith('account-690-deposit-'))
        self.assertIn('asOfDate=' + support.DATE, args)
        self.assertEqual([arg for arg in args if arg.startswith('run.id=')], ['run.id=6900001,java.lang.Long'])
        self.assertNotIn(SYNTHETIC_PASSWORD, repr(args))
        self.assertIn(SYNTHETIC_PASSWORD, launch.kwargs['env']['SPRING_APPLICATION_JSON'])
        self.assertEqual(self.assertion.call_count, 2)

    def test_new_completed_metadata_still_requires_business_results(self):
        self.assertion.side_effect = [None, support.BatchError('output mismatch')]
        with patch.object(runner, 'metadata', side_effect=['', '8|COMPLETED|COMPLETED']), self.assertRaises(support.BatchError):
            runner.run_job('podman', {}, 'deposit', 10)
        self.assertNotIn('PASS', self.output.getvalue())
        self.assertFalse(support.PENDING.exists())

    def test_launch_failure_or_interrupt_preserves_original_cause_when_cleanup_times_out(self):
        for failure in (support.BatchError('launch failed'), KeyboardInterrupt()):
            with self.subTest(failure=type(failure).__name__):
                support.clear_pending()
                self.command.side_effect = failure
                def cleanup(args, **kwargs):
                    if 'stop' in args:
                        raise support.BatchError('cleanup command timed out')
                    if 'inspect' in args and args[-2] == '{{.State.Running}}':
                        return 'true'
                    return self.engine_response(args, **kwargs)
                self.cleanup_command.side_effect = cleanup
                with patch.object(runner, 'metadata', return_value=''), self.assertRaises(support.BatchError) as caught:
                    runner.run_job('podman', {}, 'deposit', 10)
                expected = 'launch failed' if isinstance(failure, support.BatchError) else 'execution interrupted'
                self.assertIn(expected, str(caught.exception))
                self.assertIn('cleanup unconfirmed, pending record retained', str(caught.exception))
                self.assertTrue(support.PENDING.exists())
                self.assertTrue(any('stop' in call.args[0] for call in self.cleanup_command.call_args_list))
                self.assertNotIn('PASS', self.output.getvalue())

    def test_runtime_failure_survives_cleanup_inventory_timeout(self):
        self.state = 'false 5 false'
        self.cleanup_command.side_effect = support.BatchError('cleanup inventory timed out')
        with patch.object(runner, 'metadata', return_value=''), self.assertRaises(support.BatchError) as caught:
            runner.run_job('podman', {}, 'deposit', 10)
        self.assertIn('runtime exit/OOM gate failed', str(caught.exception))
        self.assertIn('cleanup unconfirmed, pending record retained', str(caught.exception))
        self.assertTrue(support.PENDING.exists())
        self.assertNotIn('PASS', self.output.getvalue())

    def test_failed_launch_with_absent_container_cleans_marker_and_preserves_failure(self):
        original = support.BatchError('launch rejected before creation')
        self.command.side_effect = original
        self.cleanup_command.return_value = ''
        self.cleanup_command.side_effect = None
        with patch.object(runner, 'metadata', return_value=''), self.assertRaises(support.BatchError) as caught:
            runner.run_job('podman', {}, 'deposit', 10)
        self.assertIs(caught.exception, original)
        self.assertFalse(support.PENDING.exists())
        self.cleanup_command.assert_called_once_with(['podman', 'ps', '--all', '--format', '{{.Names}}'])

    def test_runtime_exit_failure_or_oom_rejects_success_and_stops(self):
        for state in ('false 1 false', 'false 137 true'):
            with self.subTest(state=state):
                self.state = state
                with patch.object(runner, 'metadata', return_value=''), self.assertRaises(support.BatchError):
                    runner.run_job('podman', {}, 'deposit', 10)
                self.assertFalse(support.PENDING.exists())
        self.assertNotIn('PASS', self.output.getvalue())

    def test_incrementer_jobs_launch_with_fixed_typed_run_id(self):
        for key in ('deposit', 'mart', 'reconciliation'):
            with self.subTest(key=key):
                jar_dir = runner.ROOT / runner.JOBS[key].project / 'build/libs'
                jar_dir.mkdir(parents=True, exist_ok=True)
                if not list(jar_dir.glob('*.jar')):
                    (jar_dir / 'batch.jar').write_bytes(b'offline incrementer jar')
                self.command.reset_mock()
                with patch.object(runner, 'metadata', side_effect=['', '8|COMPLETED|COMPLETED']):
                    runner.run_job('podman', {}, key, 10)
                args = next(call.args[0] for call in self.command.call_args_list if 'run' in call.args[0])
                self.assertEqual(args.count('run.id=6900001,java.lang.Long'), 1)
                self.assertNotIn('run.id=6900001', args)

    def test_closing_identifiers_are_passed_as_long_parameters(self):
        jar_dir = runner.ROOT / 'closing/batch/build/libs'
        jar_dir.mkdir(parents=True)
        (jar_dir / 'batch.jar').write_bytes(b'offline closing jar')
        for key, parameter in (('fx', 'valuationBatchId'), ('provision', 'provisionBatchId')):
            with self.subTest(key=key):
                self.command.reset_mock()
                with patch.object(runner, 'metadata', side_effect=['', '8|COMPLETED|COMPLETED']):
                    runner.run_job('podman', {}, key, 10)
                args = next(call.args[0] for call in self.command.call_args_list if 'run' in call.args[0])
                self.assertIn(parameter + '=6900001,java.lang.Long', args)
                self.assertNotIn(parameter + '=6900001', args)
                date_name = 'valuationDate' if key == 'fx' else 'closingDate'
                self.assertIn(date_name + '=' + support.DATE, args)

    def test_recover_cli_does_not_load_credentials_or_launch_jobs(self):
        with patch.object(runner, 'execution_lock', contextlib.nullcontext), patch.object(runner, 'recover_pending') as recover, patch.object(runner, 'load_inputs') as load, patch.object(runner, 'run_job') as run:
            self.assertEqual(runner.main(['--recover', '--engine', 'docker', '--env-file', '/unused']), 0)
        recover.assert_called_once_with('docker')
        load.assert_not_called()
        run.assert_not_called()

    def test_exit_zero_requires_new_completed_metadata(self):
        for after in ('', '8|FAILED|FAILED', '8|COMPLETED|FAILED'):
            with self.subTest(after=after), patch.object(runner, 'metadata', side_effect=['', after]), self.assertRaises(support.BatchError):
                runner.run_job('podman', {}, 'deposit', 10)
        self.assertNotIn('PASS', self.output.getvalue())

    def test_running_or_unknown_previous_execution_blocks_launch(self):
        for status in ('STARTING', 'STARTED', 'STOPPING', 'UNKNOWN'):
            with self.subTest(status=status), patch.object(runner, 'metadata', return_value=f'7|{status}|UNKNOWN'), self.assertRaises(support.BatchError):
                runner.run_job('podman', {}, 'deposit', 10)
        self.command.assert_not_called()

    def test_cli_selection_runs_only_selected_job(self):
        with patch.object(runner, 'execution_lock', contextlib.nullcontext), patch.object(runner, 'load_inputs', return_value={}), patch.object(runner, 'run_job') as run:
            self.assertEqual(runner.main(['--job', 'deposit', '--engine', 'docker', '--env-file', '/unused']), 0)
        run.assert_called_once_with('docker', {}, 'deposit', 900)

    def test_all_cli_jobs_run_in_declared_dependency_order(self):
        with patch.object(runner, 'execution_lock', contextlib.nullcontext), patch.object(runner, 'load_inputs', return_value={}), patch.object(runner, 'run_job') as run:
            self.assertEqual(runner.main(['--env-file', '/unused']), 0)
        keys = [call.args[2] for call in run.call_args_list]
        self.assertEqual(keys, list(runner.JOBS))
        self.assertLess(keys.index('ecl'), keys.index('provision'))


class MetadataIdentityTests(OfflineTestCase):
    def setUp(self):
        super().setUp()
        self.db = sqlite3.connect(':memory:')
        self.addCleanup(self.db.close)
        self.db.executescript("""
            CREATE TABLE batch_job_instance (job_instance_id INTEGER, job_name TEXT);
            CREATE TABLE batch_job_execution
                (job_execution_id INTEGER, job_instance_id INTEGER, status TEXT, exit_code TEXT);
            CREATE TABLE batch_job_execution_params
                (job_execution_id INTEGER, parameter_name TEXT, parameter_value TEXT,
                 parameter_type TEXT, identifying TEXT);
        """)
        def evaluate(engine, values, context, sql):
            row = self.db.execute(sql).fetchone()
            return row[0] if row else ''
        self.offline.enter_context(patch.object(runner, 'query', side_effect=evaluate))

    def insert(self, number, key, change=None, extra=None):
        job = runner.JOBS[key]
        self.db.execute('INSERT INTO batch_job_instance VALUES(?, ?)', (number, job.name))
        self.db.execute("INSERT INTO batch_job_execution VALUES(?, ?, 'COMPLETED', 'COMPLETED')", (number, number))
        for name, value in job.params.items():
            kind = 'java.lang.Long' if name in ('valuationBatchId', 'provisionBatchId', 'run.id') else 'java.lang.String'
            param = [name, value, kind, 'Y']
            if change and name == change[0]:
                param = change
            self.db.execute('INSERT INTO batch_job_execution_params VALUES(?, ?, ?, ?, ?)', [number, *param])
        if extra:
            self.db.execute('INSERT INTO batch_job_execution_params VALUES(?, ?, ?, ?, ?)', [number, *extra])

    def test_wrong_type_flag_value_or_extra_identifier_cannot_shadow_exact_instance(self):
        for key, name in [('fx', 'valuationBatchId'), ('provision', 'provisionBatchId')]:
            with self.subTest(key=key):
                self.insert(1, key)
                self.insert(2, key, change=[name, '6900001', 'java.lang.String', 'Y'])
                self.insert(3, key, change=[name, '6900001', 'java.lang.Long', 'N'])
                self.insert(4, key, extra=['run.id', '1', 'java.lang.Long', 'Y'])
                self.insert(5, key, change=[name, '6900002', 'java.lang.Long', 'Y'])
                self.assertEqual(runner.metadata('podman', {}, runner.JOBS[key]), '1|COMPLETED|COMPLETED')
                self.db.execute('DELETE FROM batch_job_execution WHERE job_execution_id=1')
                self.assertEqual(runner.metadata('podman', {}, runner.JOBS[key]), '')
                for table in ('batch_job_execution', 'batch_job_instance', 'batch_job_execution_params'):
                    self.db.execute('DELETE FROM ' + table)

    def test_incrementer_manifest_pins_only_required_jobs_and_excludes_wrong_run_ids(self):
        self.assertEqual({key for key, job in runner.JOBS.items() if 'run.id' in job.params},
                         {'deposit', 'mart', 'reconciliation'})
        for key in ('deposit', 'mart', 'reconciliation'):
            self.assertEqual(runner.JOBS[key].params['run.id'], '6900001')
            self.assertEqual(runner.parameter_type('run.id'), 'java.lang.Long')
            self.insert(1, key)
            self.insert(2, key, change=['run.id', '1', 'java.lang.Long', 'Y'])
            self.insert(3, key, change=['run.id', '6900001', 'java.lang.String', 'Y'])
            self.assertEqual(runner.metadata('podman', {}, runner.JOBS[key]), '1|COMPLETED|COMPLETED')
            for table in ('batch_job_execution', 'batch_job_instance', 'batch_job_execution_params'):
                self.db.execute('DELETE FROM ' + table)

    def test_all_job_identities_allow_nonidentifying_diagnostics(self):
        for number, key in enumerate(runner.JOBS, 1):
            self.insert(number, key, extra=['diagnostic', 'offline', 'java.lang.String', 'N'])
            self.assertEqual(runner.metadata('podman', {}, runner.JOBS[key]), f'{number}|COMPLETED|COMPLETED')


class LoanLineageTests(OfflineTestCase):
    def setUp(self):
        super().setUp()
        self.db = sqlite3.connect(':memory:')
        self.addCleanup(self.db.close)
        self.db.executescript("""
            CREATE TABLE loan_accrual_log
                (loan_id INTEGER, accrual_date TEXT, status TEXT, journal_entry_id INTEGER, journal_no TEXT);
            CREATE TABLE loan_events
                (loan_id INTEGER, event_date TEXT, event_type TEXT, journal_entry_id INTEGER, journal_entry_slip_no TEXT);
            CREATE TABLE journal_entries
                (id INTEGER, slip_no TEXT, lineage_source_type TEXT, lineage_source_id TEXT,
                 accounting_date TEXT, status TEXT, currency_code TEXT);
        """)
        self.db.execute("INSERT INTO loan_accrual_log VALUES(6900001, ?, 'SUCCESS', 17, 'SYNTHETIC-I')", (support.DATE,))
        self.db.execute("INSERT INTO loan_events VALUES(6900001, ?, 'SCHEDULED_REPAYMENT', 18, 'SYNTHETIC-R')", (support.DATE,))
        self.db.execute("INSERT INTO journal_entries VALUES(17, 'SYNTHETIC-I', 'LOAN', '6900001', ?, 'POSTED', 'KRW')", (support.DATE,))
        self.db.execute("INSERT INTO journal_entries VALUES(18, 'SYNTHETIC-R', 'LOAN_SCHEDULED_REPAYMENT', ?, ?, 'POSTED', 'KRW')", ('6900001:' + support.DATE, support.DATE))
        def evaluate(engine, values, context, sql):
            return '\n'.join(str(row[0]) if row[0] is not None else '' for row in self.db.execute(sql))
        self.offline.enter_context(patch.object(runner, 'query', side_effect=evaluate))

    def test_exact_local_remote_pairs_pass_for_both_jobs(self):
        for key in ('loan-interest', 'loan-repayment'):
            self.assertTrue(runner.assert_loan_lineage('podman', {}, key))

    def test_unrelated_nonnull_reference_rejects_completed_reuse_without_exposing_values(self):
        for key, table, slip in [('loan-interest', 'loan_accrual_log', 'journal_no'),
                                 ('loan-repayment', 'loan_events', 'journal_entry_slip_no')]:
            for assignment in ('journal_entry_id=999', slip + "='UNRELATED-SLIP'"):
                with self.subTest(key=key, assignment=assignment):
                    self.db.execute('SAVEPOINT mutation')
                    self.db.execute('UPDATE ' + table + ' SET ' + assignment)
                    output = io.StringIO()
                    with patch.object(runner, 'metadata', return_value='1|COMPLETED|COMPLETED'), patch.object(runner, 'assert_query') as assertions, contextlib.redirect_stdout(output):
                        with self.assertRaises(support.BatchError) as caught:
                            runner.run_job('podman', {}, key, 10)
                    self.assertEqual(assertions.call_count, 2)
                    self.assertNotIn('PASS', output.getvalue())
                    self.assertNotIn('999', str(caught.exception))
                    self.assertNotIn('UNRELATED-SLIP', str(caught.exception))
                    self.db.execute('ROLLBACK TO mutation')
                    self.db.execute('RELEASE mutation')


class BalanceAndProvisionScopeTests(OfflineTestCase):
    def setUp(self):
        super().setUp()
        self.db = sqlite3.connect(':memory:')
        self.addCleanup(self.db.close)
        self.db.executescript("""
            CREATE TABLE journal_entries (id INTEGER, accounting_date TEXT, slip_no TEXT, lineage_source_type TEXT, lineage_source_id TEXT);
            CREATE TABLE gl_balances (balance_date TEXT, account_code TEXT, currency_code TEXT, period TEXT);
            CREATE TABLE sl_balances (balance_date TEXT, account_code TEXT, currency_code TEXT, period TEXT,
                                      bp_code TEXT, dept_code TEXT);
            CREATE TABLE allowance_summary (base_date TEXT, run_id TEXT, model_version TEXT,
                legal_entity_code TEXT, currency_code TEXT, exposure_account_code TEXT,
                allowance_account_code TEXT, bad_debt_expense_account_code TEXT, reversal_income_account_code TEXT);
        """)
        def evaluate(engine, values, context, sql):
            return 't' if self.db.execute(sql).fetchone()[0] else 'f'
        self.offline.enter_context(patch.object(runner, 'query', side_effect=evaluate))

    def test_foreign_lineage_without_balance_projections_blocks_launch(self):
        for source_type in ('LOAN', 'LOAN_SCHEDULED_REPAYMENT', 'FX_VALUATION', 'ECL_PROVISION', 'GH690'):
            for source_id in ('FOREIGN', None):
                for slip in ('OTHER-SLIP', 'GH690-PREFIX-IS-NOT-OWNERSHIP'):
                    with self.subTest(source_type=source_type, source_id=source_id, slip=slip):
                        self.db.execute('INSERT INTO journal_entries VALUES(999, ?, ?, ?, ?)',
                                        (support.DATE, slip, source_type, source_id))
                        with patch.object(runner, 'metadata', return_value=''), patch.object(runner, 'command') as command, patch.object(runner, 'assert_query') as assertion:
                            with self.assertRaises(support.BatchError):
                                runner.run_job('podman', {}, 'balances', 10)
                        command.assert_not_called()
                        assertion.assert_not_called()
                        self.db.execute('DELETE FROM journal_entries')

    def test_exact_seed_and_generated_lineages_allowed(self):
        rows = [
            (6900001, 'GH690-KRW-001', 'GH690', 'GH690-KRW-001'),
            (6900002, 'GH690-USD-001', 'GH690', 'GH690-USD-001'),
            (6900003, 'GH690-DRAFT-001', 'GH690', 'GH690-DRAFT-001'),
            (7000001, 'GENERATED', 'LOAN', '6900001'),
            (7000002, 'GENERATED', 'LOAN_SCHEDULED_REPAYMENT', '6900001:' + support.DATE),
            (7000003, 'GENERATED', 'FX_VALUATION', '6900001|GH690-FX|USD'),
            (7000004, 'GENERATED', 'FX_VALUATION', '6900001|GH690-FXLIAB|USD'),
            (7000005, 'GENERATED', 'ECL_PROVISION', '6900001|GH690-ALLOWANCE|KRW'),
        ]
        for number, slip, source_type, source_id in rows:
            self.db.execute('INSERT INTO journal_entries VALUES(?, ?, ?, ?, ?)',
                            (number, support.DATE, slip, source_type, source_id))
            runner.scope_check('podman', {}, 'balances')
        self.db.execute('UPDATE journal_entries SET id=999 WHERE id=6900001')
        with self.assertRaises(support.BatchError):
            runner.scope_check('podman', {}, 'balances')

    def test_orphan_balance_or_wrong_dimensions_block_before_launch(self):
        for table in ('gl_balances', 'sl_balances'):
            dimensions = ['account_code', 'currency_code', 'period']
            if table == 'sl_balances':
                dimensions += ['bp_code', 'dept_code']
            for dimension in dimensions:
                for wrong in ('UNRELATED', None):
                    with self.subTest(table=table, dimension=dimension, wrong=wrong):
                        row = dict(balance_date=support.DATE, account_code='GH690-CASH', currency_code='KRW', period='2090-01')
                        if table == 'sl_balances':
                            row.update(bp_code='GH690-CUSTOMER', dept_code='GH690-DEPT')
                        row[dimension] = wrong
                        self.db.execute('INSERT INTO ' + table + ' VALUES(' + ','.join('?' for _ in row) + ')', list(row.values()))
                        with patch.object(runner, 'metadata', return_value=''), patch.object(runner, 'command') as command, patch.object(runner, 'assert_query') as assertion:
                            with self.assertRaises(support.BatchError):
                                runner.run_job('podman', {}, 'balances', 10)
                        command.assert_not_called()
                        assertion.assert_not_called()
                        self.db.execute('DELETE FROM ' + table)

    def test_empty_partial_fixture_and_other_date_balances_allowed(self):
        runner.scope_check('podman', {}, 'balances')
        for account, currency in [('GH690-CASH', 'KRW'), ('GH690-EQUITY', 'KRW'),
                                   ('GH690-FX', 'USD'), ('GH690-FXLIAB', 'USD')]:
            self.db.execute("INSERT INTO gl_balances VALUES(?, ?, ?, '2090-01')", (support.DATE, account, currency))
            self.db.execute("INSERT INTO sl_balances VALUES(?, ?, ?, '2090-01', 'GH690-CUSTOMER', 'GH690-DEPT')", (support.DATE, account, currency))
            runner.scope_check('podman', {}, 'balances')
        self.db.execute("INSERT INTO gl_balances VALUES('2089-12-31', 'UNRELATED', 'EUR', '2089-12')")
        self.db.execute("INSERT INTO sl_balances VALUES('2089-12-31', 'UNRELATED', 'EUR', '2089-12', 'OTHER', 'OTHER')")
        runner.scope_check('podman', {}, 'balances')

    def test_same_run_model_with_foreign_summary_dimensions_rejected(self):
        expected = dict(base_date=support.DATE, run_id='GH690', model_version='GH690', legal_entity_code='GH690',
                        currency_code='KRW', exposure_account_code='GH690-LOAN', allowance_account_code='GH690-ALLOWANCE',
                        bad_debt_expense_account_code='GH690-BADDEBT', reversal_income_account_code='GH690-REVERSAL')
        insert = 'INSERT INTO allowance_summary VALUES(' + ','.join('?' for _ in expected) + ')'
        self.db.execute(insert, list(expected.values()))
        runner.scope_check('podman', {}, 'provision')
        for field in list(expected)[1:]:
            for wrong in ('UNRELATED', None):
                with self.subTest(field=field, wrong=wrong):
                    row = expected | {field: wrong}
                    self.db.execute(insert, list(row.values()))
                    with self.assertRaises(support.BatchError):
                        runner.scope_check('podman', {}, 'provision')
                    self.db.execute('DELETE FROM allowance_summary WHERE rowid=(SELECT MAX(rowid) FROM allowance_summary)')
        self.db.execute(insert, list((expected | {'base_date': '2089-12-31', 'exposure_account_code': 'UNRELATED'}).values()))
        runner.scope_check('podman', {}, 'provision')


class MartScopeTests(OfflineTestCase):
    def setUp(self):
        super().setUp()
        self.db = sqlite3.connect(':memory:')
        self.addCleanup(self.db.close)
        self.db.executescript("""
            CREATE TABLE ods_acc_ledger (is_active BOOLEAN, acc_no TEXT);
            CREATE TABLE allowance_input_positions (base_dt TEXT, acc_no TEXT);
            CREATE TABLE allowance_exposure_snapshots
                (base_date TEXT, exposure_id TEXT, source_account_no TEXT);
        """)
        self.query = self.offline.enter_context(patch.object(runner, 'query', side_effect=self.evaluate))

    def evaluate(self, engine, values, context, sql):
        self.assertEqual(context, 'account-mart')
        return 't' if self.db.execute(sql).fetchone()[0] else 'f'

    def test_inactive_ledger_does_not_hide_unrelated_existing_position(self):
        self.db.execute("INSERT INTO ods_acc_ledger VALUES(FALSE, 'UNRELATED')")
        runner.scope_check('podman', {}, 'mart')
        self.db.execute('INSERT INTO allowance_input_positions VALUES(?, ?)', (support.DATE, 'UNRELATED'))
        with patch.object(runner, 'metadata', return_value=''), patch.object(runner, 'command') as command, patch.object(runner, 'assert_query') as assertion:
            with self.assertRaises(support.BatchError):
                runner.run_job('podman', {}, 'mart', 10)
        command.assert_not_called()
        assertion.assert_not_called()

    def test_exposure_identity_and_source_account_must_both_be_synthetic(self):
        for exposure, account in [('UNRELATED', 'GH690-ACCOUNT'), ('GH690-EXPOSURE', 'UNRELATED'),
                                  (None, 'GH690-ACCOUNT'), ('GH690-EXPOSURE', None)]:
            with self.subTest(exposure=exposure, account=account):
                self.db.execute('DELETE FROM allowance_exposure_snapshots')
                self.db.execute('INSERT INTO allowance_exposure_snapshots VALUES(?, ?, ?)',
                                (support.DATE, exposure, account))
                with self.assertRaises(support.BatchError):
                    runner.scope_check('podman', {}, 'mart')

    def test_synthetic_rows_and_other_date_outputs_do_not_block(self):
        self.db.execute("INSERT INTO ods_acc_ledger VALUES(TRUE, 'GH690-ACCOUNT')")
        self.db.execute('INSERT INTO allowance_input_positions VALUES(?, ?)', (support.DATE, 'GH690-ACCOUNT'))
        self.db.execute('INSERT INTO allowance_exposure_snapshots VALUES(?, ?, ?)',
                        (support.DATE, 'GH690-EXPOSURE', 'GH690-ACCOUNT'))
        self.db.execute("INSERT INTO allowance_input_positions VALUES('2089-12-31', 'UNRELATED')")
        self.db.execute("INSERT INTO allowance_exposure_snapshots VALUES('2089-12-31', 'UNRELATED', 'UNRELATED')")
        runner.scope_check('podman', {}, 'mart')

    def test_null_position_identity_fails_closed(self):
        self.db.execute('INSERT INTO allowance_input_positions VALUES(?, NULL)', (support.DATE,))
        with self.assertRaises(support.BatchError):
            runner.scope_check('podman', {}, 'mart')


class MasterDataEndpointContractTests(OfflineTestCase):
    def setUp(self):
        super().setUp()
        compose = (TOOLS / 'compose.minimal-auth-external-dev.yml').read_text()
        provider = re.search(r'^  minimal-master-data:\n(?P<body>.*?)(?=^  [a-z][a-z-]*:|\Z)',
                             compose, re.MULTILINE | re.DOTALL)
        self.assertIsNotNone(provider, 'Master Data provider service must exist')
        body = provider.group('body')
        port = re.search(r'SERVER_PORT: "([0-9]+)"', body)
        self.assertIsNotNone(port, 'Provider must declare its listening port')
        self.port = int(port.group(1))
        health = re.search(r'http://127\.0\.0\.1:([0-9]+)/actuator/health/readiness', body)
        self.assertIsNotNone(health, 'Provider must probe its own readiness endpoint')
        self.assertEqual(int(health.group(1)), self.port)

    def assert_provider_endpoint(self, url):
        endpoint = urlsplit(url)
        self.assertEqual(endpoint.hostname, 'minimal-master-data')
        self.assertEqual(endpoint.port, self.port, 'Consumer must use the provider listening port')

    def test_loan_required_account_mappings_match_seed_and_posting_assertions(self):
        mappings = {
            'cash-account-code': ('GH690-REPAYCASH', 'repayment', 'DEBIT'),
            'loan-receivable-account-code': ('GH690-LOAN', 'repayment', 'CREDIT'),
            'accrued-interest-receivable-account-code': ('115010', 'interest', 'DEBIT'),
            'interest-income-account-code': ('410100', 'interest', 'CREDIT'),
        }
        master_seed = support.seed_path('master-data', 'seed.sql').read_text()
        for key in ('loan-interest', 'loan-repayment'):
            properties = runner.runtime_properties(runner.JOBS[key], inputs('loan'))
            for name, (account, result, side) in mappings.items():
                with self.subTest(job=key, property=name):
                    self.assertEqual(properties['account.loan.accounting.' + name], account)
                    self.assertIn("'" + account + "'", master_seed)
                    result_sql = support.seed_path('journal-ledger', 'result-loan-' + result + '.sql').read_text()
                    self.assertIn("d.side='" + side + "' AND d.account_code='" + account + "'", result_sql)

    def test_only_loan_gets_three_connections_for_nested_transactions(self):
        values = {}
        for context in support.CONTEXTS:
            values.update(inputs(context))
        for key, job in runner.JOBS.items():
            with self.subTest(job=key):
                properties = runner.runtime_properties(job, values)
                expected = 3 if key in ('loan-interest', 'loan-repayment') else 2
                self.assertEqual(properties['spring.datasource.hikari.maximum-pool-size'], expected)
                self.assertEqual(properties['spring.datasource.hikari.minimum-idle'], 0)
                self.assertEqual(properties['spring.datasource.hikari.connection-timeout'], 15000)

    def test_runtime_http_timeouts_are_bounded_and_add_no_retry_configuration(self):
        values = {}
        for context in support.CONTEXTS:
            values.update(inputs(context))
        for key, prefix in [('fx', 'closing.journal-ledger'), ('loan-interest', 'account.loan.http'),
                            ('deposit', 'account.deposit.http'), ('reconciliation', 'reconciliation.journal-ledger')]:
            with self.subTest(job=key):
                properties = runner.runtime_properties(runner.JOBS[key], values)
                self.assertEqual(properties[prefix + '.connect-timeout'], '10s')
                self.assertEqual(properties[prefix + '.read-timeout'], '60s')
                self.assertFalse(any('retry' in name.lower() for name in properties))

    def test_runtime_consumers_match_provider_listening_port(self):
        values = {}
        for context in support.CONTEXTS:
            values.update(inputs(context))
        properties = runner.runtime_properties(runner.JOBS['balances'], values)
        for key in ('journal-ledger.master-data.base-url', 'account.loan.master-data-base-url',
                    'account.deposit.master-data-base-url'):
            with self.subTest(property=key):
                self.assert_provider_endpoint(properties[key])

    def test_compose_consumer_defaults_match_provider_listening_port(self):
        for filename in ('compose.accounting-external-dev.yml', 'compose.products-external-dev.yml'):
            with self.subTest(compose=filename):
                source = (TOOLS / filename).read_text()
                endpoints = re.findall(r'http://minimal-master-data:[0-9]+', source)
                self.assertTrue(endpoints, 'Expected explicit Master Data consumer defaults')
                for endpoint in endpoints:
                    self.assert_provider_endpoint(endpoint)

    def test_journal_dev_fallback_matches_provider_listening_port(self):
        source = (TOOLS.parent / 'journal-ledger/api/src/main/resources/application-dev.yml').read_text()
        endpoint = re.search(r'\$\{MASTER_DATA_BASE_URL:(http://minimal-master-data:[0-9]+)\}', source)
        self.assertIsNotNone(endpoint, 'Expected a Master Data endpoint fallback')
        self.assert_provider_endpoint(endpoint.group(1))


if __name__ == '__main__':
    unittest.main()
