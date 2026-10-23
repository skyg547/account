import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { join } from 'node:path';
import { test } from 'node:test';

const require = createRequire(import.meta.url);
const ts = require('typescript');
const frontendRoot = join(import.meta.dirname, '..');

// Execute the checked-in TS/TSX modules; the fakes replace only network and React's browser host.
function loadSource(relativePath, dependencies = {}) {
  const source = readFileSync(join(frontendRoot, relativePath), 'utf8');
  const { outputText } = ts.transpileModule(source, {
    fileName: relativePath,
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      jsx: ts.JsxEmit.ReactJSX,
      target: ts.ScriptTarget.ES2022,
      esModuleInterop: true,
    },
  });
  const compiledModule = { exports: {} };
  const localRequire = (name) => name in dependencies ? dependencies[name] : require(name);
  new Function('require', 'module', 'exports', outputText)(localRequire, compiledModule, compiledModule.exports);
  return compiledModule.exports;
}

const { adminService } = loadSource('src/services/adminService.ts');
const target = {
  id: 9001,
  name: 'Audit User',
  email: 'audit-user@example.invalid',
  role: 'USER',
  status: 'ACTIVE',
  lastLogin: '',
  dept: 'Audit',
  pendingRequestId: 'previous-42',
};
const other = { ...target, id: 9002, name: 'Other User', pendingRequestId: 'other-11' };

function response(status, body) {
  return { ok: status >= 200 && status < 300, status, json: async () => body };
}

async function withFetch(fakeFetch, action) {
  const previous = globalThis.fetch;
  globalThis.fetch = fakeFetch;
  try {
    return await action();
  } finally {
    globalThis.fetch = previous;
  }
}

test('service rejects transport, HTTP, JSON, and incomplete responses without a synthetic request', async () => {
  const cases = [
    ['403', async () => response(403, { message: 'private-server-detail' })],
    ['500', async () => response(500, {})],
    ['network', async () => { throw new Error('private-network-detail'); }],
    ['invalid JSON', async () => ({ ok: true, json: async () => { throw new SyntaxError('private-json-detail'); } })],
    ['missing ID', async () => response(200, { status: 'PENDING' })],
    ['legacy approvalId alone', async () => response(200, { approvalId: 123, status: 'PENDING' })],
    ['null body', async () => response(200, null)],
    ['zero ID', async () => response(200, { id: 0, status: 'PENDING' })],
    ['negative ID', async () => response(200, { id: -4, status: 'PENDING' })],
    ['fractional ID', async () => response(200, { id: 1.5, status: 'PENDING' })],
    ['unsafe ID', async () => response(200, { id: Number.MAX_SAFE_INTEGER + 1, status: 'PENDING' })],
    ['blank ID', async () => response(200, { id: ' ', status: 'PENDING' })],
    ['noncanonical ID', async () => response(200, { id: '1e3', status: 'PENDING' })],
    ['missing status', async () => response(200, { id: 123 })],
    ['unknown status', async () => response(200, { id: 123, status: 'UNKNOWN' })],
  ];

  for (const [name, fakeFetch] of cases) {
    await withFetch(fakeFetch, async () => {
      await assert.rejects(
        adminService.requestRoleChange(target, 'AUDITOR'),
        (error) => error instanceof Error && !/private-|ROLE-9001|example\.invalid/.test(error.message),
        name,
      );
    });
  }
});

test('service sends one request and preserves the verified server ID, status, and date', async () => {
  const calls = [];
  await withFetch(async (url, options) => {
    calls.push({ url, options });
    return response(200, { id: 999, status: 'PENDING', effectiveDate: '2026-10-01' });
  }, async () => {
    assert.deepEqual(await adminService.requestRoleChange(target, 'AUDITOR'), {
      requestId: '999', status: 'PENDING', effectiveDate: '2026-10-01',
    });
  });
  assert.equal(calls.length, 1);
  assert.equal(calls[0].url, '/api/audit/approvals/requests');
  assert.equal(calls[0].options.method, 'POST');
  const payload = JSON.parse(calls[0].options.body);
  assert.equal(payload.masterType, 'AUTH_USER_ROLE');
  assert.equal(payload.requestType, 'UPDATE');
  assert.equal(JSON.parse(payload.payload).role, 'AUDITOR');
});

test('service preserves a valid string ID and returned non-pending status on repeated calls', async () => {
  let calls = 0;
  await withFetch(async () => {
    calls += 1;
    return response(200, { id: '1000', status: 'APPROVED', effectiveDate: null });
  }, async () => {
    for (let i = 0; i < 2; i += 1) {
      assert.deepEqual(await adminService.requestRoleChange(target, 'AUDITOR'), {
        requestId: '1000', status: 'APPROVED', effectiveDate: null,
      });
    }
  });
  assert.equal(calls, 2);
});

test('repeated successes and failures do not mutate the fallback user fixture or caller input', async () => {
  const originalTarget = structuredClone(target);
  let roleCalls = 0;
  await withFetch(async (url) => {
    if (url === '/api/admin/users') return response(503, {});
    roleCalls += 1;
    return roleCalls === 2
      ? response(200, { id: 777, status: 'PENDING', effectiveDate: '2026-10-01' })
      : response(500, {});
  }, async () => {
    const fixtureBefore = structuredClone(await adminService.getUsers());
    await assert.rejects(adminService.requestRoleChange(target, 'AUDITOR'));
    assert.equal((await adminService.requestRoleChange(target, 'AUDITOR')).requestId, '777');
    await assert.rejects(adminService.requestRoleChange(target, 'AUDITOR'));
    assert.deepEqual(await adminService.getUsers(), fixtureBefore);
  });
  assert.equal(roleCalls, 3);
  assert.deepEqual(target, originalTarget);
});

function pageHarness() {
  const state = [];
  const toasts = { success: [], error: [] };
  let hookIndex = 0;
  const react = {
    useState(initialValue) {
      const index = hookIndex++;
      if (!(index in state)) state[index] = initialValue;
      return [state[index], (update) => {
        state[index] = typeof update === 'function' ? update(state[index]) : update;
      }];
    },
    useEffect() {},
    useCallback(callback) { return callback; },
  };
  const element = (type, props, key) => ({ type, props: { ...props, key } });
  const { default: Page } = loadSource('src/app/admin/users/page.tsx', {
    react,
    'react/jsx-runtime': { jsx: element, jsxs: element, Fragment: 'fragment' },
    'lucide-react': new Proxy({}, { get: (_, icon) => String(icon) }),
    '@/context/NavContext': { useNav: () => ({ userRole: 'SYSTEM_ADMIN' }) },
    '@/context/ToastContext': {
      useToast: () => ({
        success: (message) => toasts.success.push(message),
        error: (message) => toasts.error.push(message),
      }),
    },
    '@/services/adminService': { adminService },
  });
  function render() {
    hookIndex = 0;
    return Page();
  }
  render();
  state[1] = [structuredClone(target), structuredClone(other)];
  function roleSelect() {
    const walk = (node) => {
      if (Array.isArray(node)) return node.map(walk).find(Boolean);
      if (!node || typeof node !== 'object') return undefined;
      if (node.type === 'select' && node.props['aria-label'] === 'Audit User 역할 변경') return node;
      return walk(node.props?.children);
    };
    return walk(render());
  }
  return { state, toasts, roleSelect };
}

test('page displays one confirmed success and changes only the selected row', async () => {
  const page = pageHarness();
  const untouched = structuredClone(page.state[1][1]);
  let calls = 0;
  await withFetch(async () => {
    calls += 1;
    return response(200, { id: 999, status: 'PENDING', effectiveDate: '2026-10-01' });
  }, () => page.roleSelect().props.onChange({ target: { value: 'AUDITOR' } }));
  assert.equal(calls, 1);
  assert.equal(page.state[1][0].status, 'PENDING');
  assert.equal(page.state[1][0].pendingRequestId, '999');
  assert.deepEqual(page.state[1][1], untouched);
  assert.match(page.state[4], /999/);
  assert.equal(page.toasts.success.length, 1);
  assert.equal(page.toasts.error.length, 0);
});

test('page first failure preserves an ACTIVE user, existing request ID, and empty success message', async () => {
  const failedResponses = [
    ['403', async () => response(403, {})],
    ['500', async () => response(500, {})],
    ['network', async () => { throw new Error('private-network-detail'); }],
    ['invalid JSON', async () => ({ ok: true, json: async () => { throw new SyntaxError('private-json-detail'); } })],
    ['missing ID', async () => response(200, { status: 'PENDING' })],
  ];
  for (const [name, fakeFetch] of failedResponses) {
    const page = pageHarness();
    const beforeRows = structuredClone(page.state[1]);
    await withFetch(fakeFetch, () => page.roleSelect().props.onChange({ target: { value: 'AUDITOR' } }));
    assert.deepEqual(page.state[1], beforeRows, name);
    assert.equal(page.state[1][0].status, 'ACTIVE', name);
    assert.equal(page.state[1][0].pendingRequestId, 'previous-42', name);
    assert.equal(page.state[4], '', name);
    assert.equal(page.toasts.success.length, 0, name);
    assert.equal(page.toasts.error.length, 1, name);
  }
});

test('page failure leaves prior row, pending ID, and success message untouched', async () => {
  const page = pageHarness();
  await withFetch(async () => response(200, { id: 42, status: 'PENDING', effectiveDate: '2026-10-01' }),
    () => page.roleSelect().props.onChange({ target: { value: 'AUDITOR' } }));
  const beforeRows = structuredClone(page.state[1]);
  const beforeMessage = page.state[4];
  const failedResponses = [
    async () => response(403, {}),
    async () => response(500, {}),
    async () => { throw new Error('private-network-detail'); },
    async () => ({ ok: true, json: async () => { throw new SyntaxError('private-json-detail'); } }),
    async () => response(200, { status: 'PENDING' }),
  ];
  for (const fakeFetch of failedResponses) {
    await withFetch(fakeFetch, () => page.roleSelect().props.onChange({ target: { value: 'AUDITOR' } }));
    assert.deepEqual(page.state[1], beforeRows);
    assert.equal(page.state[4], beforeMessage);
  }
  assert.equal(page.toasts.success.length, 1);
  assert.equal(page.toasts.error.length, failedResponses.length);
  assert.ok(page.toasts.error.every((message) => !/private-|example\.invalid/.test(message)));
});

test('page does not mark a user PENDING when server returns APPROVED', async () => {
  const page = pageHarness();
  const beforeRows = structuredClone(page.state[1]);
  await withFetch(async () => response(200, { id: 1001, status: 'APPROVED', effectiveDate: null }),
    () => page.roleSelect().props.onChange({ target: { value: 'AUDITOR' } }));
  assert.deepEqual(page.state[1], beforeRows);
  assert.equal(page.toasts.success.length, 1);
  assert.equal(page.toasts.error.length, 0);
});
