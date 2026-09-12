#!/usr/bin/env python3
"""Opt-in real registry/update proof. Only random, newly created fixtures mutate."""
import argparse
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import tempfile
import time
import urllib.request
import uuid

import yaml


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--engine', choices=['docker', 'podman'], default='docker')
    parser.add_argument('--socket', default='/var/run/docker.sock')
    args = parser.parse_args()
    if not Path(args.socket).is_absolute() or not stat.S_ISSOCK(os.stat(args.socket).st_mode):
        parser.error('--socket must be an existing absolute Unix socket')
    # Ensure the CLI and mounted socket talk to the same engine, without reading
    # user credential files or relying on a selected remote context.
    engine = ([args.engine, '--host', 'unix://' + args.socket] if args.engine == 'docker'
              else [args.engine, '--remote', '--url', 'unix://' + args.socket])
    spec = yaml.safe_load((Path(__file__).parents[1] / 'compose.yml').read_text())['services']['watchtower']
    update = yaml.safe_load((Path(__file__).parents[1] / 'compose.update.yml').read_text())['services']['watchtower']
    prefix = 'account-710-' + uuid.uuid4().hex[:12]
    scope = prefix
    owned = []
    tags = []

    def call(*command, check=True, timeout=120):
        result = subprocess.run(engine + list(command), text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.PIPE, timeout=timeout)
        if check and result.returncode:
            # Never dump arbitrary engine/config/credential-helper diagnostics.
            raise RuntimeError('fixture command failed: ' + command[0])
        return result.stdout.strip()

    def state(name):
        return json.loads(call('inspect', '--format', '{{json .State}}', name))

    def identity(name):
        return call('inspect', '--format', '{{.Id}} {{.Image}}', name)

    def create(suffix, image, labels=(), start=True, extra=()):
        name = prefix + '-' + suffix
        # A successful create is required before registering cleanup ownership.
        call('create', '--name', name, '--label', 'account.issue=710',
             '--memory', '32m', '--cpus', '0.1', '--pids-limit', '32',
             *sum((['--label', label] for label in labels), []), *extra, image)
        owned.append(name)
        if start:
            call('start', name)
        return name

    def watch(suffix, monitor=False, persistent=False):
        name = prefix + '-' + suffix
        command = list(spec['command'] if monitor else update['command'])
        command[command.index('--scope') + 1] = scope
        if not persistent:
            command.append('--run-once')
        call('create', '--name', name, '--label', 'account.issue=710',
             '--label', 'com.centurylinklabs.watchtower.enable=false',
             '--label', 'com.centurylinklabs.watchtower.scope=' + scope,
             '--network', 'host', '--read-only', '--tmpfs', spec['tmpfs'][0],
             '--cap-drop', 'ALL', '--security-opt', 'no-new-privileges',
             '--memory', spec['mem_limit'], '--cpus', str(spec['cpus']),
             '--pids-limit', str(spec['pids_limit']),
             '-e', 'DOCKER_API_VERSION=' + spec['environment']['DOCKER_API_VERSION'],
             '-e', 'WATCHTOWER_MONITOR_ONLY=' + ('true' if monitor else 'false'),
             '--mount', 'type=bind,source=' + args.socket + ',target=/var/run/docker.sock',
             spec['image'], *command)
        owned.append(name)
        call('start', name)
        if persistent:
            time.sleep(3)
            assert state(name)['Running'], 'watcher failed to start'
            usage = call('stats', '--no-stream', '--format', '{{.MemUsage}}', name)
            print('MEASURE watcher idle usage / limit:', usage, flush=True)
            call('stop', '--time', '5', name)
        else:
            call('wait', name, timeout=180)
            status = state(name)
            if status['ExitCode'] != 0 or status['OOMKilled']:
                print('WATCHER FAILURE exit=', status['ExitCode'], 'OOM=', status['OOMKilled'], flush=True)
                result = subprocess.run(engine + ['logs', name], text=True, capture_output=True, timeout=30)
                print(result.stdout + result.stderr, flush=True)
            assert status['ExitCode'] == 0 and not status['OOMKilled'], 'watcher failed/OOM'
        log = call('logs', name)
        # Watchtower writes to stderr; retrieve synthetic-only logs separately.
        result = subprocess.run(engine + ['logs', name], text=True, capture_output=True, timeout=30)
        return log + result.stderr

    def summary(log, scanned, updated, failed=0):
        counts = ', '.join(re.findall(r'(?:Scanned|Updated|Failed)=\d+', log))
        assert re.search(r'Scanned=' + str(scanned) + r'\b', log), 'selection count mismatch: ' + counts
        assert re.search(r'Updated=' + str(updated) + r'\b', log), 'update count mismatch: ' + counts
        assert re.search(r'Failed=' + str(failed) + r'\b', log), 'failure count mismatch: ' + counts

    try:
        for image in (spec['image'], 'docker.io/library/registry:2.8.3', 'docker.io/library/busybox:1.37.0'):
            print('PULL fixture image:', image, flush=True)
            call('pull', image, timeout=240)
        registry = create('registry', 'docker.io/library/registry:2.8.3',
                          extra=('-p', '127.0.0.1::5000', '--tmpfs', '/var/lib/registry:size=32m'))
        address = call('port', registry, '5000/tcp')
        assert re.fullmatch(r'127\.0\.0\.1:[0-9]+', address), 'registry not loopback-only'
        for attempt in range(30):
            try:
                with urllib.request.urlopen('http://' + address + '/v2/', timeout=2) as response:
                    assert response.status == 200
                break
            except OSError:
                time.sleep(1)
        else:
            raise RuntimeError('registry not ready')
        channel = address + '/account-710:dev'
        with tempfile.TemporaryDirectory(prefix=prefix) as directory:
            for version in ('v1', 'v2'):
                folder = Path(directory)
                (folder / 'version').write_text(version + '\n')
                (folder / 'Dockerfile').write_text(
                    'FROM docker.io/library/busybox:1.37.0\nCOPY version /version\n'
                    'CMD ["sh", "-c", "trap \'exit 0\' TERM INT; while :; do sleep 1; done"]\n')
                tag = address + '/account-710:' + version
                call('build', '-t', tag, directory, timeout=180)
                tags.append(tag)
            def publish(version):
                call('tag', address + '/account-710:' + version, channel)
                if channel not in tags:
                    tags.append(channel)
                flags = ['--tls-verify=false'] if args.engine == 'podman' else []
                call('push', *flags, channel)

            publish('v1')
            enabled = 'com.centurylinklabs.watchtower.enable=true'
            scoped = 'com.centurylinklabs.watchtower.scope=' + scope
            selected = create('selected', channel, (enabled, scoped))
            selected_two = create('selected-two', channel, (enabled, scoped))
            targets = [selected, selected_two]
            controls = [create('unlabelled', channel),
                        create('wrong-scope', channel, (enabled, scoped + '-other')),
                        create('disabled', channel, (scoped, 'com.centurylinklabs.watchtower.enable=false')),
                        create('stopped', channel, (enabled, scoped), start=False)]
            before = {name: identity(name) for name in targets + controls}
            publish('v2')
            # Publisher and deployment daemon share this test host. Put the
            # deployment cache back to v1: only a real registry pull can find v2.
            call('tag', address + '/account-710:v1', channel)
            old_image = call('image', 'inspect', '--format', '{{.Id}}', channel)
            new_image = call('image', 'inspect', '--format', '{{.Id}}', address + '/account-710:v2')
            assert old_image != new_image
            # v1.7.1's legacy Updated metric includes stale monitor-only images;
            # container IDs and /version below prove that nothing restarted.
            summary(watch('monitor', monitor=True), 2, 2)
            assert call('image', 'inspect', '--format', '{{.Id}}', channel) == new_image, 'monitor did not pull registry v2'
            assert all(identity(name) == before[name] for name in targets)
            assert call('exec', selected, 'cat', '/version') == 'v1'
            print('PASS registry v2 detection in monitor-only, v1 stays running', flush=True)
            update_log = watch('update')
            summary(update_log, 2, 2)
            transitions = re.findall(r'msg="(Stopping|Creating) /?(' + re.escape(prefix) + r'-selected(?:-two)?)\b', update_log)
            assert len(transitions) == 4, 'missing two-target lifecycle log evidence'
            assert [event for event, _ in transitions] == ['Stopping', 'Creating', 'Stopping', 'Creating']
            assert transitions[0][1] == transitions[1][1] and transitions[2][1] == transitions[3][1]
            assert transitions[0][1] != transitions[2][1], 'targets did not restart sequentially'
            after = identity(selected)
            assert after.split()[0] != before[selected].split()[0]
            assert after.split()[1] != before[selected].split()[1]
            assert call('exec', selected, 'cat', '/version') == 'v2'
            assert call('exec', selected_two, 'cat', '/version') == 'v2'
            after_two = identity(selected_two)
            assert all(identity(name) == before[name] for name in controls)
            assert not state(controls[-1])['Running']
            print('PASS two selected v1 -> v2 sequential recreations; four excluded controls unchanged', flush=True)
            summary(watch('repeat'), 2, 0)
            assert identity(selected) == after and identity(selected_two) == after_two
            print('PASS unchanged-image repeat is idempotent', flush=True)
            # Force an unavailable registry with a different local image: a
            # failed pull must not cause update from the local cache.
            call('tag', address + '/account-710:v1', channel)
            call('stop', '--time', '5', registry)
            unavailable_log = watch('unavailable')
            # Pull errors are Skipped, excluded from both Scanned and Failed in
            # 1.7.1. A zero exit/Failed count alone is not success evidence.
            summary(unavailable_log, 0, 0)
            assert 'connection refused' in unavailable_log
            for name in targets:
                assert re.search(r'Unable to update container[^\n]*' + re.escape(name), unavailable_log)
            assert identity(selected) == after and state(selected)['Running']
            assert identity(selected_two) == after_two and state(selected_two)['Running']
            print('PASS unavailable registry preserves running target', flush=True)
            call('start', registry)
            publish('v1')
            summary(watch('rollback'), 2, 2)
            assert call('exec', selected, 'cat', '/version') == 'v1'
            assert call('exec', selected_two, 'cat', '/version') == 'v1'
            assert all(identity(name) == before[name] for name in controls)
            print('PASS previous image rollback, controls preserved', flush=True)
            watch('idle', monitor=True, persistent=True)
    except Exception as error:
        print('FAIL before fixture cleanup:', str(error), flush=True)
        raise
    finally:
        failures = []
        for name in reversed(owned):
            try:
                call('rm', '--force', name)
            except (RuntimeError, subprocess.TimeoutExpired):
                failures.append(name)
        for tag in reversed(tags):
            call('image', 'rm', tag, check=False)
        if failures:
            raise RuntimeError('fixture cleanup incomplete: ' + ', '.join(failures))
        print('CLEANUP owned fixtures only; base images retained', flush=True)


if __name__ == '__main__':
    main()
