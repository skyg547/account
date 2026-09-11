// Opt-in local smoke: only the explicitly labelled account-713-probe may be mutated.
// Password/JWT remain in memory; no HAR, trace, response body or token URL is printed.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const origin = 'http://localhost:8080';
const base = `${origin}/portainer`;
const probe = 'account-713-probe';
let stage = 'initial HTTP and authentication';
const localFetch = (url, options = {}) => fetch(url, {
  signal: AbortSignal.timeout(20000), ...options,
});

async function main() {
  const passwordFile = process.env.PORTAINER_ADMIN_PASSWORD_FILE;
  assert(passwordFile, 'Set PORTAINER_ADMIN_PASSWORD_FILE to the test instance file');
  const password = fs.readFileSync(passwordFile, 'utf8').trim();
  const redirect = await localFetch(base, { redirect: 'manual' });
  assert.equal(redirect.status, 301);
  assert.equal(redirect.headers.get('location'), '/portainer/');
  assert.equal((await localFetch(`${base}/api/status`)).status, 200);
  assert.equal((await localFetch(`${base}/api/endpoints`)).status, 401);
  const auth = await localFetch(`${base}/api/auth`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Origin: origin },
    body: JSON.stringify({ Username: 'admin', Password: password }),
  });
  assert.equal(auth.status, 200);
  const { jwt } = await auth.json();
  assert(jwt);
  const api = (path, options = {}) => fetch(`${base}/api${path}`, {
    ...options,
    signal: AbortSignal.timeout(20000),
    headers: { Authorization: `Bearer ${jwt}`, Origin: origin,
      'Content-Type': 'application/json', ...options.headers },
  });
  const endpointsResponse = await api('/endpoints');
  assert.equal(endpointsResponse.status, 200);
  const endpoints = await endpointsResponse.json();
  const local = endpoints.find(endpoint => endpoint.URL === 'unix:///var/run/docker.sock');
  assert(local && local.Status === 1, 'Local socket environment must be up');
  const docker = `/endpoints/${local.Id}/docker`;
  const inspect = await api(`${docker}/containers/${probe}/json`);
  assert.equal(inspect.status, 200);
  const container = await inspect.json();
  assert.equal(container.Config.Labels['account.issue'], '713', 'Refuse unrelated container');
  assert(container.State.Running);
  console.log('PASS redirect, status, unauthenticated denial, login, local environment');

  stage = 'probe log stream';
  const logs = await api(`${docker}/containers/${probe}/logs?follow=true&stdout=true&stderr=true&tail=1`);
  assert.equal(logs.status, 200);
  const reader = logs.body.getReader();
  let logText = '';
  while (!logText.includes('portainer-probe-ready')) {
    const part = await reader.read();
    assert(!part.done, 'Probe log stream ended before the marker');
    logText += Buffer.from(part.value).toString();
  }
  await reader.cancel();
  console.log('PASS streamed probe logs');
  stage = 'probe restart';
  const restart = await api(`${docker}/containers/${probe}/restart?t=2`, { method: 'POST' });
  assert.equal(restart.status, 204);
  const after = await (await api(`${docker}/containers/${probe}/json`)).json();
  assert(after.State.Running && after.State.StartedAt !== container.State.StartedAt);
  console.log('PASS probe-only restart');

  stage = 'exec creation';
  const exec = await api(`${docker}/containers/${probe}/exec`, {
    method: 'POST', body: JSON.stringify({ AttachStdin: true, AttachStdout: true,
      AttachStderr: true, Tty: true, Cmd: ['/bin/sh'] }),
  });
  assert.equal(exec.status, 201);
  const execId = (await exec.json()).Id;
  const browser = await chromium.launch({ headless: true });
  try {
    stage = 'Chromium login';
    const page = await browser.newPage();
    const assetFailures = [];
    let assetCount = 0;
    page.on('response', response => {
      const url = new URL(response.url());
      if (url.origin === origin && /\.(js|css)$/.test(url.pathname)) {
        assetCount++;
        if (response.status() >= 400) assetFailures.push(response.status());
      }
    });
    await page.goto(`${base}/`, { waitUntil: 'networkidle' });
    await page.locator('#username').fill('admin');
    await page.locator('#password').fill(password);
    await page.getByRole('button', { name: 'Login', exact: true }).click();
    await page.waitForFunction(() => !document.querySelector('#password'));
    await page.getByRole('link', { name: 'Home', exact: true }).waitFor();
    assert(assetCount > 0 && assetFailures.length === 0, 'Real browser JS/CSS must load');
    console.log(`PASS Chromium login and ${assetCount} JavaScript/CSS responses`);

    stage = 'interactive WebSocket';
    const cdp = await page.context().newCDPSession(page);
    await cdp.send('Network.enable');
    let upgradeStatus;
    cdp.on('Network.webSocketHandshakeResponseReceived', event => {
      upgradeStatus = event.response.status;
    });
    // Split the marker in the shell command so terminal input echo cannot fake success.
    const terminalOk = await page.evaluate(({ execId, endpointId, jwt }) =>
      new Promise((resolve, reject) => {
        const query = new URLSearchParams({ id: execId, endpointId, token: jwt });
        const socket = new WebSocket(`ws://localhost:8080/portainer/api/websocket/exec?${query}`);
        socket.binaryType = 'arraybuffer';
        let output = '';
        const timer = setTimeout(() => { socket.close(); reject(new Error('Terminal timeout')); }, 15000);
        socket.onopen = () => socket.send("printf 'portainer-%s\\n' 'ws-ok'\n");
        socket.onmessage = event => {
          output += typeof event.data === 'string' ? event.data : new TextDecoder().decode(event.data);
          if (output.includes('portainer-ws-ok')) {
            clearTimeout(timer);
            socket.send('exit\n');
            socket.close();
            resolve(true);
          }
        };
        socket.onerror = () => { clearTimeout(timer); reject(new Error('Terminal handshake failed')); };
      }), { execId, endpointId: String(local.Id), jwt });
    assert(terminalOk);
    assert.equal(upgradeStatus, 101);
    console.log('PASS WebSocket HTTP 101 and interactive shell command output');
  } finally {
    await browser.close();
  }
}

// Assertion/runtime errors can contain sensitive request data. Emit a fixed failure only.
main().catch(() => { console.error(`FAIL live smoke at ${stage}; inspect locally without logging credentials`); process.exitCode = 1; });
