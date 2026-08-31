import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createServer } from 'node:http';
import { once } from 'node:events';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { after, before, test } from 'node:test';

const MOCK_SESSION_TOKEN = 'mockHeader.mockPayload.mockSignature';
const productionMode = process.env.AUTH_BFF_TEST_MODE === 'production';
const expectedCookieName = productionMode ? '__Host-account_session' : 'account_session';
const upstreamRequests = [];
let mockGateway;
let nextProcess;
let frontendOrigin;
let sessionCookie;
let serverOutput = '';

function listenOnRandomPort(server) {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      server.off('error', reject);
      resolve(server.address().port);
    });
  });
}

async function reservePort() {
  const server = createServer();
  const port = await listenOnRandomPort(server);
  await new Promise((resolve, reject) => server.close((error) => (error ? reject(error) : resolve())));
  return port;
}

async function readRequestBody(request) {
  const chunks = [];
  for await (const chunk of request) {
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString('utf8');
}

async function waitForFrontend() {
  const deadline = Date.now() + 60_000;
  while (Date.now() < deadline) {
    if (nextProcess.exitCode !== null) {
      throw new Error(`Next server exited early (${nextProcess.exitCode}).\n${serverOutput}`);
    }
    try {
      const response = await fetch(`${frontendOrigin}/next.svg`);
      if (response.ok) {
        return;
      }
    } catch {
      // The development server is still compiling.
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(`Next server did not become ready.\n${serverOutput}`);
}

function sameOriginHeaders(additional = {}) {
  return {
    Origin: frontendOrigin,
    'Sec-Fetch-Site': 'same-origin',
    ...additional,
  };
}

before(async () => {
  mockGateway = createServer(async (request, response) => {
    const body = await readRequestBody(request);
    upstreamRequests.push({
      method: request.method,
      url: request.url,
      authorization: request.headers.authorization,
      cookie: request.headers.cookie,
      userId: request.headers['x-user-id'],
      body,
    });

    if (request.method === 'POST' && request.url === '/api/auth/login') {
      const login = JSON.parse(body);
      if (
        login.username !== 'valid-user' ||
        login.password !== 'test-password' ||
        login.loginType !== 'NORMAL'
      ) {
        response.writeHead(401, { 'Content-Type': 'application/json' });
        response.end('{"message":"denied"}');
        return;
      }
      response.writeHead(200, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({
        token: MOCK_SESSION_TOKEN,
        tokenType: 'Bearer',
        expiresIn: 3600,
        username: 'valid-user',
        departmentCode: 'TEST',
        roles: ['ROLE_TEST'],
        roleVersion: 1,
      }));
      return;
    }

    if (request.url === '/api/unauthorized') {
      response.writeHead(401, {
        'Content-Type': 'application/json',
        'Retry-After': '3',
        'X-Auth-Error': 'token_expired',
      });
      response.end('{"message":"expired"}');
      return;
    }

    response.writeHead(200, {
      'Content-Type': 'application/json',
      'Set-Cookie': 'upstream_cookie=must_not_escape',
      'X-RateLimit-Remaining': '41',
      'X-Request-Id': 'request-from-gateway',
    });
    response.end(JSON.stringify({ ok: true }));
  });
  const gatewayPort = await listenOnRandomPort(mockGateway);
  const frontendPort = await reservePort();
  frontendOrigin = `http://127.0.0.1:${frontendPort}`;

  nextProcess = spawn(
    process.execPath,
    [
      'node_modules/next/dist/bin/next',
      productionMode ? 'start' : 'dev',
      '--hostname',
      '127.0.0.1',
      '--port',
      String(frontendPort),
    ],
    {
      cwd: process.cwd(),
      env: {
        ...process.env,
        GATEWAY_INTERNAL_URL: `http://127.0.0.1:${gatewayPort}`,
        NEXT_TELEMETRY_DISABLED: '1',
      },
      stdio: ['ignore', 'pipe', 'pipe'],
    },
  );
  for (const stream of [nextProcess.stdout, nextProcess.stderr]) {
    stream.on('data', (chunk) => {
      serverOutput = `${serverOutput}${chunk.toString('utf8')}`.slice(-12_000);
    });
  }
  await waitForFrontend();
});

after(async () => {
  if (nextProcess && nextProcess.exitCode === null) {
    nextProcess.kill('SIGTERM');
    await Promise.race([
      once(nextProcess, 'exit'),
      new Promise((resolve) => setTimeout(resolve, 5000)),
    ]);
  }
  if (mockGateway?.listening) {
    await new Promise((resolve, reject) =>
      mockGateway.close((error) => (error ? reject(error) : resolve())),
    );
  }
});

test('cross-origin login is rejected before Gateway', async () => {
  const beforeCount = upstreamRequests.length;
  const response = await fetch(`${frontendOrigin}/api/auth/login`, {
    method: 'POST',
    headers: {
      Origin: 'https://untrusted.example',
      'Sec-Fetch-Site': 'cross-site',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username: 'valid-user', password: 'test-password' }),
  });
  assert.equal(response.status, 403);
  assert.equal(upstreamRequests.length, beforeCount);
});

test('login and authenticated master-data clients have no readable JWT dependency', () => {
  const loginPage = readFileSync(join(process.cwd(), 'src', 'app', 'login', 'page.tsx'), 'utf8');
  const authService = readFileSync(join(process.cwd(), 'src', 'services', 'authService.ts'), 'utf8');
  const browserSession = readFileSync(
    join(process.cwd(), 'src', 'services', 'browserSession.ts'),
    'utf8',
  );
  const masterDataService = readFileSync(
    join(process.cwd(), 'src', 'services', 'masterDataService.ts'),
    'utf8',
  );
  const authClientSource = [loginPage, authService, masterDataService].join('\n');
  assert.doesNotMatch(authClientSource, /localStorage\.(?:getItem|setItem)\(['"]auth_token['"]/);
  assert.doesNotMatch(authClientSource, /localStorage\.(?:getItem|setItem)\(['"]user_info['"]/);
  assert.doesNotMatch(authClientSource, /sessionStorage[^\n]*(?:auth_token|user_info)/);
  assert.doesNotMatch(authClientSource, /document\.cookie/);
  assert.doesNotMatch(authClientSource, /NEXT_PUBLIC_AUTH_API_URL/);
  assert.doesNotMatch(loginPage, /token\s*[,}]/);
  assert.doesNotMatch(masterDataService, /Authorization|Bearer/);
  assert.match(browserSession, /['"]auth_token['"]/);
  assert.match(browserSession, /localStorage\.removeItem\(key\)/);
  assert.doesNotMatch(browserSession, /localStorage\.(?:getItem|setItem)/);
});

test('declared oversized login body is rejected before Gateway', async () => {
  const beforeCount = upstreamRequests.length;
  const response = await fetch(`${frontendOrigin}/api/auth/login`, {
    method: 'POST',
    headers: sameOriginHeaders({ 'Content-Type': 'application/json' }),
    body: 'x'.repeat((16 * 1024) + 1),
  });
  assert.equal(response.status, 413);
  assert.equal(upstreamRequests.length, beforeCount);
});

test('chunked oversized login body is stopped before Gateway', async () => {
  const beforeCount = upstreamRequests.length;
  const body = new ReadableStream({
    start(controller) {
      controller.enqueue(new TextEncoder().encode('x'.repeat(10 * 1024)));
      controller.enqueue(new TextEncoder().encode('x'.repeat(10 * 1024)));
      controller.close();
    },
  });
  const response = await fetch(`${frontendOrigin}/api/auth/login`, {
    method: 'POST',
    headers: sameOriginHeaders({ 'Content-Type': 'application/json' }),
    body,
    duplex: 'half',
  });
  assert.equal(response.status, 413);
  assert.equal(upstreamRequests.length, beforeCount);
});

test('same-origin login stores an HttpOnly session and hides JWT', async () => {
  const response = await fetch(`${frontendOrigin}/api/auth/login`, {
    method: 'POST',
    headers: sameOriginHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ username: 'valid-user', password: 'test-password' }),
  });
  assert.equal(response.status, 200);

  const setCookie = response.headers.get('set-cookie');
  assert.ok(setCookie);
  assert.match(setCookie, new RegExp(`^${expectedCookieName}=`));
  assert.match(setCookie, /HttpOnly/i);
  assert.match(setCookie, /SameSite=Strict/i);
  assert.match(setCookie, /Path=\//i);
  assert.match(setCookie, /Max-Age=3600/i);
  if (productionMode) {
    assert.match(setCookie, /;\s*Secure/i);
  } else {
    assert.doesNotMatch(setCookie, /;\s*Secure/i);
  }
  sessionCookie = setCookie.split(';', 1)[0];

  const body = await response.json();
  assert.equal(body.username, 'valid-user');
  assert.equal(body.token, undefined);
  assert.equal(body.tokenType, undefined);
  assert.doesNotMatch(JSON.stringify(body), new RegExp(MOCK_SESSION_TOKEN));
});

test('protected proxy injects only the server cookie token and strips spoofed identity', async () => {
  const response = await fetch(`${frontendOrigin}/api/protected?view=summary`, {
    headers: {
      Cookie: `${sessionCookie}; theme=dark`,
      Authorization: 'Bearer browser-controlled-value',
      'X-User-ID': 'spoofed-browser-user',
    },
  });
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('set-cookie'), null);
  assert.equal(response.headers.get('x-request-id'), 'request-from-gateway');
  assert.equal(response.headers.get('x-ratelimit-remaining'), '41');

  const forwarded = upstreamRequests.at(-1);
  assert.equal(forwarded.url, '/api/protected?view=summary');
  assert.equal(forwarded.authorization, `Bearer ${MOCK_SESSION_TOKEN}`);
  assert.equal(forwarded.cookie, undefined);
  assert.equal(forwarded.userId, undefined);
});

test('missing session is rejected without reaching Gateway', async () => {
  const beforeCount = upstreamRequests.length;
  const response = await fetch(`${frontendOrigin}/api/protected`);
  assert.equal(response.status, 401);
  assert.equal(upstreamRequests.length, beforeCount);
});

test('cross-origin mutation is rejected before Gateway', async () => {
  const beforeCount = upstreamRequests.length;
  const response = await fetch(`${frontendOrigin}/api/protected`, {
    method: 'POST',
    headers: {
      Cookie: sessionCookie,
      Origin: 'https://untrusted.example',
      'Sec-Fetch-Site': 'cross-site',
      'Content-Type': 'application/json',
    },
    body: '{"change":true}',
  });
  assert.equal(response.status, 403);
  assert.equal(upstreamRequests.length, beforeCount);
});

test('same-origin mutation reaches Gateway with the server credential', async () => {
  const response = await fetch(`${frontendOrigin}/api/protected`, {
    method: 'POST',
    headers: sameOriginHeaders({
      Cookie: sessionCookie,
      'Content-Type': 'application/json',
      'X-User-ID': 'spoofed-browser-user',
    }),
    body: '{"change":true}',
  });
  assert.equal(response.status, 200);
  const forwarded = upstreamRequests.at(-1);
  assert.equal(forwarded.authorization, `Bearer ${MOCK_SESSION_TOKEN}`);
  assert.equal(forwarded.userId, undefined);
  assert.equal(forwarded.body, '{"change":true}');
});

test('upstream 401 clears the browser session', async () => {
  const response = await fetch(`${frontendOrigin}/api/unauthorized`, {
    headers: { Cookie: sessionCookie },
  });
  assert.equal(response.status, 401);
  assert.equal(response.headers.get('retry-after'), '3');
  assert.equal(response.headers.get('x-auth-error'), 'token_expired');
  const setCookie = response.headers.get('set-cookie');
  assert.ok(setCookie);
  assert.match(setCookie, new RegExp(`^${expectedCookieName}=`));
  assert.match(setCookie, /Max-Age=0/i);
  assert.match(setCookie, /HttpOnly/i);
});

test('session status exposes only a boolean', async () => {
  const active = await fetch(`${frontendOrigin}/api/auth/session`, {
    headers: { Cookie: sessionCookie },
  });
  assert.deepEqual(await active.json(), { authenticated: true });

  const anonymous = await fetch(`${frontendOrigin}/api/auth/session`);
  assert.deepEqual(await anonymous.json(), { authenticated: false });
});

test('logout requires same origin and expires the HttpOnly cookie', async () => {
  const rejected = await fetch(`${frontendOrigin}/api/auth/logout`, {
    method: 'POST',
    headers: {
      Cookie: sessionCookie,
      Origin: 'https://untrusted.example',
      'Sec-Fetch-Site': 'cross-site',
    },
  });
  assert.equal(rejected.status, 403);

  const response = await fetch(`${frontendOrigin}/api/auth/logout`, {
    method: 'POST',
    headers: sameOriginHeaders({ Cookie: sessionCookie }),
  });
  assert.equal(response.status, 204);
  const setCookie = response.headers.get('set-cookie');
  assert.ok(setCookie);
  assert.match(setCookie, new RegExp(`^${expectedCookieName}=`));
  assert.match(setCookie, /Max-Age=0/i);
  assert.match(setCookie, /HttpOnly/i);
});
