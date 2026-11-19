import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { once } from 'node:events';
import { access, copyFile, mkdir, mkdtemp, readFile, symlink, writeFile } from 'node:fs/promises';
import { createServer } from 'node:net';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// This harness owns every service it contacts. It accepts local artifact paths, never remote URLs.
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const java = process.env.CHAIN_JAVA ?? 'java';
const authJar = resolve(root, process.env.CHAIN_AUTH_JAR ?? 'auth/api/build/libs/api-0.0.1-SNAPSHOT.jar');
const gatewayJar = resolve(root, process.env.CHAIN_GATEWAY_JAR ?? 'gateway/build/libs/gateway-0.0.1-SNAPSHOT.jar');
const frontend = join(root, 'frontend');
const children = new Set();
const sleep = (ms) => new Promise((done) => setTimeout(done, ms));
let stage = 'prerequisites';
let directory;

// Deliberate allowlist: do not inherit developer datasource, JWT, proxies, Java options or Next secrets.
function cleanEnvironment(extra = {}) {
  const environment = {};
  for (const key of ['PATH', 'Path', 'SystemRoot', 'WINDIR', 'TEMP', 'TMP', 'HOME', 'USERPROFILE', 'JAVA_HOME']) {
    if (process.env[key]) environment[key] = process.env[key];
  }
  return { ...environment, NEXT_TELEMETRY_DISABLED: '1', ...extra };
}

function launch(command, args, environment, cwd = directory) {
  const child = spawn(command, args, {
    cwd, env: cleanEnvironment(environment), windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'],
  });
  child.failure = null;
  child.on('error', () => { child.failure = 'process startup failed'; });
  // Raw application output can contain tokens or request bodies. Drain without persisting or echoing it.
  child.stdout.resume();
  child.stderr.resume();
  children.add(child);
  return child;
}

async function stop(child) {
  if (!child || child.exitCode !== null || child.signalCode !== null || (child.failure && !child.pid)) return;
  const exited = once(child, 'exit');
  child.kill('SIGTERM');
  await Promise.race([exited, sleep(5_000)]);
  if (child.exitCode === null && child.signalCode === null) {
    child.kill('SIGKILL');
    await Promise.race([exited, sleep(5_000)]);
  }
  assert.ok(child.exitCode !== null || child.signalCode !== null, 'owned process did not exit');
}

async function javaHelper(mode, environment) {
  const child = launch(java, [join(root, 'gateway/tests/GovernanceChainSupport.java'), mode, authJar, directory], environment);
  let result = '';
  child.stdout.on('data', (chunk) => { result += chunk; });
  let timer;
  try {
    const [code] = await Promise.race([once(child, 'exit'), new Promise((_, reject) => {
      timer = setTimeout(() => reject(new Error(`Java fixture ${mode} timed out`)), 30_000);
    })]);
    assert.equal(code, 0, `Java fixture ${mode} failed (raw output suppressed)`);
    return result.trim();
  } finally { clearTimeout(timer); }
}

async function randomPort() {
  const socket = createServer();
  socket.listen(0, '127.0.0.1');
  await once(socket, 'listening');
  const port = socket.address().port;
  await new Promise((done, reject) => socket.close((error) => error ? reject(error) : done()));
  return port;
}

async function ready(origin, path, child) {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    assert.ok(!child.failure && child.exitCode === null && child.signalCode === null,
      `${stage}: owned server exited before readiness (raw output suppressed)`);
    try {
      const response = await fetch(`${origin}${path}`, { signal: AbortSignal.timeout(2_000) });
      if (response.ok) return;
    } catch { /* The owned server may still be binding its dynamic port. */ }
    await sleep(250);
  }
  throw new Error(`${stage}: readiness timed out`);
}

async function prepareBff() {
  // Copy the actual route implementation into an isolated Next project. Never load checkout .env files.
  const project = join(directory, 'bff');
  await mkdir(project);
  for (const relative of ['src/server/auth/bff.ts', 'src/app/api/auth/login/route.ts',
    'src/app/api/auth/logout/route.ts', 'src/app/api/auth/session/route.ts', 'src/app/api/[...path]/route.ts']) {
    const target = join(project, relative);
    await mkdir(dirname(target), { recursive: true });
    await copyFile(join(frontend, relative), target);
  }
  await writeFile(join(project, 'package.json'), JSON.stringify({ private: true }));
  await writeFile(join(project, 'tsconfig.json'), JSON.stringify({ compilerOptions: {
    target: 'ES2017', lib: ['dom', 'dom.iterable', 'esnext'], strict: true, noEmit: true,
    module: 'esnext', moduleResolution: 'bundler', jsx: 'preserve', esModuleInterop: true,
    resolveJsonModule: true, isolatedModules: true, paths: { '@/*': ['./src/*'] },
  }, include: ['**/*.ts', '**/*.tsx', '.next/types/**/*.ts'], exclude: ['node_modules'] }));
  await writeFile(join(project, 'src/app/layout.tsx'),
    'export default function Layout({children}: {children: React.ReactNode}) { return <html><body>{children}</body></html>; }');
  await writeFile(join(project, 'src/app/page.tsx'), 'export default function Page() { return <main>Acceptance</main>; }');
  await symlink(join(frontend, 'node_modules'), join(project, 'node_modules'), process.platform === 'win32' ? 'junction' : 'dir');
  return project;
}

async function run() {
  assert.equal(Number(process.versions.node.split('.')[0]), 20, 'Node 20 is required; no prerequisite is skipped');
  for (const path of [authJar, gatewayJar, join(frontend, 'node_modules/next/dist/bin/next'),
    join(frontend, 'node_modules/typescript/package.json'), join(frontend, 'node_modules/@types/react/package.json'),
    join(frontend, 'node_modules/@types/node/package.json')]) await access(path);
  const lock = JSON.parse(await readFile(join(frontend, 'package-lock.json'), 'utf8'));
  for (const name of ['next', 'react', 'react-dom', 'typescript']) {
    const installed = JSON.parse(await readFile(join(frontend, `node_modules/${name}/package.json`), 'utf8'));
    assert.equal(installed.version, lock.packages[`node_modules/${name}`].version, `${name} must match lockfile`);
  }
  directory = await mkdtemp(join(tmpdir(), 'account-governance-chain-'));
  const password = randomBytes(32).toString('base64url');
  const databasePassword = randomBytes(32).toString('base64url');
  const jwtSecret = randomBytes(48).toString('base64url');
  const bffSecret = randomBytes(48).toString('base64url');
  stage = 'synthetic credentials';
  const hash = await javaHelper('hash', { CHAIN_PASSWORD: password });
  assert.match(hash, /^\$2[aby]\$/);
  const [authPort, gatewayPort, frontendPort] = await Promise.all([randomPort(), randomPort(), randomPort()]);
  assert.equal(new Set([authPort, gatewayPort, frontendPort]).size, 3, 'dynamic ports must be distinct');
  const authOrigin = `http://127.0.0.1:${authPort}`;
  const gatewayOrigin = `http://127.0.0.1:${gatewayPort}`;
  const frontendOrigin = `http://127.0.0.1:${frontendPort}`;
  const jdbcUrl = `jdbc:h2:file:${join(directory, 'auth-db').replaceAll('\\', '/')};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE`;
  const common = {
    'server.address': '127.0.0.1', 'spring.cloud.config.enabled': false,
    'spring.cloud.vault.enabled': false, 'eureka.client.enabled': false,
    'management.tracing.enabled': false, 'management.zipkin.tracing.export.enabled': false,
    'spring.config.location': 'classpath:/application.yml,classpath:/application-local.yml',
    'spring.profiles.active': 'local', 'auth.jwt.secret': jwtSecret,
    'logging.level.root': 'OFF', 'spring.main.banner-mode': 'off',
  };
  stage = 'Auth startup';
  const auth = launch(java, ['-jar', authJar], { SPRING_APPLICATION_JSON: JSON.stringify({
    ...common, 'server.port': authPort, 'spring.datasource.url': jdbcUrl,
    'spring.datasource.username': 'chain', 'spring.datasource.password': databasePassword,
    'spring.datasource.driver-class-name': 'org.h2.Driver', 'spring.jpa.hibernate.ddl-auto': 'validate',
    'spring.flyway.enabled': true, 'auth.internal-api.token': randomBytes(32).toString('base64url'),
    'auth.master-data.enabled': false, 'auth.users': [
      { username: 'chain-maker', password: hash, departmentCode: 'CHAIN', roles: ['ROLE_SYSTEM_ADMIN'] },
      { username: 'chain-checker', password: hash, departmentCode: 'CHAIN', roles: ['ROLE_SYSTEM_ADMIN'] },
      { username: 'chain-target', password: hash, departmentCode: 'CHAIN', roles: ['ROLE_USER'] },
    ],
  }) });
  await ready(authOrigin, '/actuator/health', auth);
  stage = 'Gateway startup';
  const gateway = launch(java, ['-jar', gatewayJar], { SPRING_APPLICATION_JSON: JSON.stringify({
    ...common, 'server.port': gatewayPort, 'spring.config.import': '',
    // Preserve the real packaged route list and token-version HTTP adapter; only discovery data is synthetic.
    'spring.cloud.discovery.enabled': true, 'spring.cloud.loadbalancer.enabled': true,
    'spring.cloud.discovery.client.simple.instances.auth-service[0].uri': authOrigin,
    'auth.token-version-validation.enabled': true,
    'auth.token-version-validation.timeout-millis': 5_000,
    'auth.token-version-validation.cache-ttl-seconds': 1, 'gateway.bff.shared-secret': bffSecret,
  }) });
  await ready(gatewayOrigin, '/actuator/health', gateway);
  stage = 'BFF startup';
  const bffProject = await prepareBff();
  const bff = launch(process.execPath, [join(root, 'gateway/tests/governance-bff-server.mjs')], {
    BFF_GATEWAY_SHARED_SECRET: bffSecret, GATEWAY_INTERNAL_URL: gatewayOrigin,
    FRONTEND_PUBLIC_ORIGIN: frontendOrigin, NODE_ENV: 'development',
    CHAIN_FRONTEND_PACKAGE: join(frontend, 'package.json'), CHAIN_BFF_PORT: String(frontendPort),
  }, bffProject);
  await ready(frontendOrigin, '/', bff);
  async function request(path, { cookie, body, origin = frontendOrigin, method = body ? 'POST' : 'GET' } = {}) {
    return fetch(`${origin}${path}`, { method, redirect: 'error', signal: AbortSignal.timeout(30_000),
      headers: { Origin: frontendOrigin, 'Sec-Fetch-Site': 'same-origin', 'Content-Type': 'application/json',
        ...(cookie ? { Cookie: cookie } : {}), 'X-User-ID': 'forged-client' },
      ...(body ? { body: JSON.stringify(body) } : {}) });
  }
  async function login(username) {
    const response = await request('/api/auth/login', { body: { username, password, loginType: 'NORMAL' } });
    assert.equal(response.status, 200, `login ${username}`);
    const cookie = response.headers.get('set-cookie')?.split(';')[0];
    assert.ok(cookie?.startsWith('account_session='), 'real BFF session cookie required');
    const session = await response.json();
    assert.ok(!Object.hasOwn(session, 'token'), 'JWT must remain HttpOnly');
    return { cookie, session };
  }
  stage = 'anonymous denial';
  const path = '/api/audit/roles/ROLE_USER/authorizations';
  assert.equal((await request(path)).status, 401);
  assert.equal((await request(path, { origin: gatewayOrigin })).status, 401);
  const maker = await login('chain-maker');
  const checker = await login('chain-checker');
  const target = await login('chain-target');
  assert.deepEqual(target.session.roles, ['ROLE_USER']);
  const receipts = new Set();
  async function pendingReceipt(response, status, masterType, masterKey, requestType) {
    assert.equal(response.status, status);
    const receipt = await response.json();
    assert.ok(Number.isSafeInteger(receipt.id) && receipt.id > 0, 'persisted positive receipt required');
    assert.ok(!receipts.has(receipt.id), 'each change must have a distinct persisted receipt');
    receipts.add(receipt.id);
    assert.equal(receipt.masterType, masterType);
    assert.equal(receipt.masterKey, masterKey);
    assert.equal(receipt.requestType, requestType);
    assert.equal(receipt.requestUser, 'chain-maker');
    assert.equal(receipt.status, 'PENDING');
    const pending = await request('/api/audit/approvals/pending', { cookie: checker.cookie });
    assert.equal(pending.status, 200);
    const persisted = (await pending.json()).filter(row => row.id === receipt.id);
    assert.equal(persisted.length, 1);
    // Compare the receipt contract; database timestamp precision may differ from the first HTTP response.
    for (const field of ['masterType', 'masterKey', 'requestType', 'payload', 'requestUser', 'status']) {
      assert.equal(persisted[0][field], receipt[field]);
    }
    return receipt;
  }
  async function approveReceipt(receipt) {
    const decisionPath = `/api/audit/approvals/${receipt.id}/approve`;
    const body = { approverUser: 'forged-approver', remarks: 'Synthetic acceptance' };
    assert.equal((await request(decisionPath, { cookie: maker.cookie, body })).status, 403);
    const decision = await request(decisionPath, { cookie: checker.cookie, body });
    assert.equal(decision.status, 200);
    const approved = await decision.json();
    assert.equal(approved.id, receipt.id);
    assert.equal(approved.masterType, receipt.masterType);
    assert.equal(approved.masterKey, receipt.masterKey);
    assert.equal(approved.requestType, receipt.requestType);
    assert.equal(approved.requestUser, 'chain-maker');
    assert.equal(approved.approverUser, 'chain-checker');
    assert.equal(approved.status, 'APPROVED');
    assert.equal((await request(decisionPath, { cookie: checker.cookie, body })).status, 409);
    const pending = await request('/api/audit/approvals/pending', { cookie: checker.cookie });
    assert.equal(pending.status, 200);
    assert.ok(!(await pending.json()).some(row => row.id === receipt.id));
  }
  async function targetGrants() {
    const response = await request(path, { cookie: target.cookie });
    assert.equal(response.status, 200);
    return response.json();
  }
  stage = 'current role permission boundary';
  assert.deepEqual(await targetGrants(), []);
  assert.equal((await request('/api/audit/roles/USER/authorizations', { cookie: target.cookie })).status, 403);
  assert.equal((await request('/api/audit/roles/ROLE_SYSTEM_ADMIN/authorizations', { cookie: target.cookie })).status, 403);
  assert.equal((await request('/api/audit/approvals/requests', { cookie: target.cookie, body: {
    masterType: 'AUTH_USER_ROLE', masterKey: 'chain-maker', requestType: 'UPDATE',
    payload: JSON.stringify({ username: 'chain-maker', role: 'AUDITOR' }), requestUser: 'chain-maker',
  } })).status, 403);

  // Exercise the menu endpoints themselves: receipt acceptance must not be confused with applied grants.
  stage = 'menu role creation and approval';
  const roleReceipt = await pendingReceipt(await request('/api/audit/roles', {
    cookie: maker.cookie, body: { roleCode: 'ROLE_USER', roleName: 'Synthetic user', description: 'Owned acceptance fixture' },
  }), 202, 'SYSTEM_ROLE', 'ROLE_USER', 'CREATE');
  assert.equal((await request('/api/audit/roles/ROLE_USER', { cookie: maker.cookie })).status, 404);
  await approveReceipt(roleReceipt);
  const role = await request('/api/audit/roles/ROLE_USER', { cookie: maker.cookie });
  assert.equal(role.status, 200);
  assert.equal((await role.json()).roleCode, 'ROLE_USER');
  assert.deepEqual(await targetGrants(), []);

  stage = 'menu READ grant pending and approved';
  const menuKey = 'ROLE_USER:MENU:/ledger/entry:READ';
  const grantBody = { functionCode: 'MENU:/ledger/entry', accessType: 'READ', dataScope: 'GLOBAL' };
  assert.equal((await request(path, { cookie: target.cookie, body: grantBody })).status, 403);
  const grantReceipt = await pendingReceipt(await request(path, { cookie: maker.cookie, body: grantBody }),
    202, 'AUTHORIZATION', menuKey, 'CREATE');
  assert.deepEqual(await targetGrants(), []);
  await approveReceipt(grantReceipt);
  const menuGrants = await targetGrants();
  assert.equal(menuGrants.length, 1);
  const menuGrant = menuGrants[0];
  assert.ok(Number.isSafeInteger(menuGrant.id) && menuGrant.id > 0);
  assert.equal(menuGrant.roleCode, 'ROLE_USER');
  assert.equal(menuGrant.functionCode, grantBody.functionCode);
  assert.equal(menuGrant.accessType, 'READ');
  assert.equal(menuGrant.dataScope, 'GLOBAL');

  stage = 'menu READ revoke pending and authoritative empty';
  const revokePath = `/api/audit/authorizations/${menuGrant.id}`;
  assert.equal((await request(revokePath, { cookie: target.cookie, method: 'DELETE' })).status, 403);
  const revokeReceipt = await pendingReceipt(await request(revokePath, { cookie: maker.cookie, method: 'DELETE' }),
    202, 'AUTHORIZATION', menuKey, 'DELETE');
  assert.deepEqual(await targetGrants(), menuGrants);
  await approveReceipt(revokeReceipt);
  assert.deepEqual(await targetGrants(), []);

  stage = 'frontend approval request';
  const approval = await request('/api/audit/approvals/requests', { cookie: maker.cookie, body: {
    masterType: 'AUTH_USER_ROLE', masterKey: 'chain-target', requestType: 'UPDATE',
    payload: JSON.stringify({ username: 'chain-target', userId: '3', role: 'AUDITOR' }),
    requestUser: 'frontend-admin', effectiveDate: new Date().toISOString().slice(0, 10), requestedVersion: 1,
  } });
  const receipt = await pendingReceipt(approval, 200, 'AUTH_USER_ROLE', 'chain-target', 'UPDATE');
  stage = 'separation of duties';
  await approveReceipt(receipt);
  stage = 'current Auth state after approval';
  const updated = await login('chain-target');
  assert.deepEqual(updated.session.roles, ['ROLE_AUDITOR']);
  assert.equal(updated.session.roleVersion, 2);
  await sleep(1_100);
  assert.equal((await request(path, { cookie: target.cookie })).status, 401);
  stage = 'persisted database verification';
  await stop(bff);
  await stop(gateway);
  await stop(auth);
  await javaHelper('verify', { CHAIN_DB_PASSWORD: databasePassword, CHAIN_JDBC_URL: jdbcUrl,
    CHAIN_APPROVAL_ID: String(receipt.id), CHAIN_ROLE_APPROVAL_ID: String(roleReceipt.id),
    CHAIN_GRANT_APPROVAL_ID: String(grantReceipt.id), CHAIN_REVOKE_APPROVAL_ID: String(revokeReceipt.id) });
  console.log('PASS: real BFF → Gateway → Auth; menu READ grant/revoke, authoritative empty, four receipts, SOD, persisted H2 effects. PostgreSQL not exercised.');
}

try { await run(); }
catch { console.error(`FAIL: governance chain at ${stage}; no skipped prerequisite counts as PASS; raw details suppressed.`); process.exitCode = 1; }
finally {
  for (const child of [...children].reverse()) {
    try { await stop(child); } catch { console.error('FAIL: owned process cleanup incomplete'); process.exitCode = 1; }
  }
  // Keep the owned temp directory for approved inspection; do not recursively delete arbitrary paths.
  if (directory) console.log(`Synthetic fixture retained: ${directory}`);
}
