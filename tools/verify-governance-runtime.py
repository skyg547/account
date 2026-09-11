#!/usr/bin/env python3
"""Read-only GH-696 live gate; prints status/counts only, never logs or credentials."""
import json
import subprocess
import urllib.request
import urllib.error


def command(args):
    result = subprocess.run(args, capture_output=True, text=True, timeout=30)
    if result.returncode:
        raise RuntimeError('container probe failed')
    return result.stdout


def request(url):
    with urllib.request.urlopen(url, timeout=45) as response:
        return response.status, response.read()


def verify():
    for name, port, discovery in [('budget-api', 8096, 'BUDGET-API'),
                                   ('internal-audit-api', 8083, 'INTERNAL-AUDIT-SERVICE')]:
        _, data = request(f'http://127.0.0.1:{port}/actuator/health')
        assert json.loads(data)['status'] == 'UP', 'API health is not UP'
        registry = json.loads(command(['podman', 'exec', 'account-frontend-nginx',
            'wget', '-qO-', '--header=Accept: application/json',
            f'http://minimal-discovery:8761/eureka/apps/{discovery}']))
        instances = registry['application']['instance']
        if isinstance(instances, dict):
            instances = [instances]
        ip = command(['podman', 'inspect', name, '--format',
            '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}']).strip()
        assert ip and any(i['status'] == 'UP' and int(i['port']['$']) == port
                          and i['ipAddr'] == ip for i in instances), 'current Eureka instance missing'
        print(f'{name}: health/Eureka UP port={port}', flush=True)
    command(['podman', 'exec', 'account-frontend-nginx', 'nginx', '-t'])
    for path in ['/healthz', '/login', '/grafana/api/health']:
        status, data = request('http://127.0.0.1:8080' + path)
        assert status == 200, 'proxy response failed'
        if path.endswith('api/health'):
            assert json.loads(data)['database'] == 'ok', 'Grafana database not ready'
        print(f'proxy {path}: HTTP 200', flush=True)
    for origin, expected in [('http://127.0.0.1:8080', 400), ('http://invalid.example', 403)]:
        req = urllib.request.Request('http://127.0.0.1:8080/api/auth/login', data=b'{}',
            headers={'Content-Type': 'application/json', 'Origin': origin}, method='POST')
        try:
            with urllib.request.urlopen(req, timeout=45) as response:
                status = response.status
        except urllib.error.HTTPError as error:
            status = error.code
        assert status == expected, 'frontend origin/input validation differs'
        print(f'frontend API validation: HTTP {expected}', flush=True)
    _, data = request('http://127.0.0.1:13001/grafana/api/health')
    assert json.loads(data)['database'] == 'ok', 'direct Grafana health failed'
    prometheus = json.loads(command(['podman', 'exec', 'grafana', 'wget', '-qO-',
                                     'http://account-prometheus-dev:9090/api/v1/query?query=up']))
    assert prometheus['status'] == 'success' and prometheus['data']['result'], 'Prometheus query failed'
    print(f'Grafana network Prometheus query: success targets={len(prometheus["data"]["result"])}', flush=True)
    mount = command(['podman', 'volume', 'inspect', 'grafana-storage', '--format', '{{.Mountpoint}}']).strip()
    # Query only datasource metadata; never read user/password/token tables or columns.
    sql = "SELECT count(*) FROM data_source WHERE type='prometheus' AND url='http://account-prometheus-dev:9090' AND is_default=1"
    probe = "import sqlite3,sys; c=sqlite3.connect('file:'+sys.argv[1]+'?mode=ro',uri=True); print(c.execute(sys.argv[2]).fetchone()[0])"
    count = command(['podman', 'unshare', 'python3', '-c', probe, mount + '/grafana.db', sql]).strip()
    assert count == '1', 'persisted Grafana datasource differs'
    print('Grafana persisted default datasource: verified', flush=True)
    for name in ['budget-api', 'internal-audit-api', 'account-frontend-nginx', 'grafana']:
        # Select fields at the engine boundary; never request container Env.
        data = command(['podman', 'inspect', name, '--format',
            '{{.State.Status}} {{.State.Health.Status}} {{.State.OOMKilled}} {{.RestartCount}} {{.HostConfig.CpuQuota}} {{.HostConfig.CpuPeriod}} {{.HostConfig.Memory}}'])
        state, health, oom, restarts, quota, period, memory = data.split()
        assert (state, health, oom, restarts) == ('running', 'healthy', 'false', '0'), 'container state failed'
        assert int(period) > 0 and int(quota) / int(period) == 0.5 and int(memory) == 805306368, 'resource cap differs'
        print(f'{name}: healthy restart=0 OOM=false CPU=0.50 RAM=768MiB', flush=True)


if __name__ == '__main__':
    try:
        verify()
    except Exception as error:
        # Error messages/bodies may contain environment-specific details.
        print('FAIL: live governance gate (' + type(error).__name__ + ')', flush=True)
        raise SystemExit(1)
    print('PASS: GH-696 live runtime gates', flush=True)
