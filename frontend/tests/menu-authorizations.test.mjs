import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join, resolve } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

const frontendRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const frontendRequire = createRequire(join(frontendRoot, 'package.json'));
const React = frontendRequire('react');
const { renderToStaticMarkup } = frontendRequire('react-dom/server');
const ts = frontendRequire('typescript');

function loadSource(file, imports = {}, cache = new Map()) {
  if (cache.has(file)) return cache.get(file).exports;
  const source = readFileSync(file, 'utf8');
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, target: ts.ScriptTarget.ES2022 },
    fileName: file,
  }).outputText;
  const loadedModule = { exports: {} };
  cache.set(file, loadedModule);
  const localRequire = (name) => {
    if (Object.hasOwn(imports, name)) return imports[name];
    if (name.startsWith('.')) {
      const relative = resolve(dirname(file), name);
      const target = name.endsWith('.tsx') || name.endsWith('.ts')
        ? relative
        : [relative + '.ts', relative + '.tsx', join(relative, 'index.ts')].find(candidate => {
          try { readFileSync(candidate); return true; } catch { return false; }
        });
      assert.ok(target, `Could not load ${name} from ${file}`);
      return loadSource(target, imports, cache);
    }
    return frontendRequire(name);
  };
  new Function('require', 'module', 'exports', 'process', 'fetch', 'AbortController', 'setTimeout', 'clearTimeout', compiled)(
    localRequire, loadedModule, loadedModule.exports, imports.process ?? process,
    imports.fetch ?? globalThis.fetch, AbortController, setTimeout, clearTimeout,
  );
  return loadedModule.exports;
}

// Drive the actual NavProvider hooks without a browser, including effect cleanup and rerenders.
function createProviderHarness(fetch) {
  const states = [];
  const effects = [];
  let pathname = '/';
  let stateCursor = 0;
  let effectCursor = 0;
  const reactHooks = {
    createContext: () => ({ Provider: function Provider() {} }),
    useContext: () => undefined,
    useState(initial) {
      const index = stateCursor++;
      if (!(index in states)) states[index] = initial;
      return [states[index], value => { states[index] = typeof value === 'function' ? value(states[index]) : value; }];
    },
    useEffect(callback, dependencies) {
      const index = effectCursor++;
      const previous = effects[index];
      if (!previous || dependencies.some((value, i) => value !== previous.dependencies[i])) {
        effects[index] = { callback, dependencies, cleanup: previous?.cleanup, pending: true };
      }
    },
  };
  const { NavProvider } = loadSource(join(frontendRoot, 'src/context/NavContext.tsx'), {
    react: reactHooks,
    'next/navigation': { usePathname: () => pathname },
    fetch,
  });
  return {
    render() {
      stateCursor = 0;
      effectCursor = 0;
      return NavProvider({ children: null }).props.value;
    },
    runEffects() {
      for (const effect of effects) {
        if (effect?.pending) {
          effect.cleanup?.();
          effect.cleanup = effect.callback();
          effect.pending = false;
        }
      }
    },
    setPathname(nextPathname) { pathname = nextPathname; },
    cleanup() { for (const effect of effects) effect?.cleanup?.(); },
  };
}

let sidebarContext;
const Icon = props => React.createElement('svg', { 'data-icon': 'test', ...props });
const icons = new Proxy({}, { get: () => Icon });
const Link = ({ href, children, ...props }) => React.createElement('a', { href, ...props }, children);
const { default: Sidebar } = loadSource(join(frontendRoot, 'src/components/layout/Sidebar.tsx'), {
  react: React,
  'next/link': { __esModule: true, default: Link },
  'next/navigation': { usePathname: () => '/' },
  'lucide-react': icons,
  '@/context/NavContext': { useNav: () => sidebarContext },
});

function renderSidebar(context, category = 'ACCOUNTING') {
  sidebarContext = {
    isCollapsed: false, toggleSidebar: () => {}, ...context, activeCategory: category,
  };
  return renderToStaticMarkup(React.createElement(Sidebar));
}

function grant(roleCode, functionCode) {
  return { id: 1, roleCode, functionCode, accessType: 'READ' };
}

function response(data, ok = true) {
  return { ok, json: async () => data };
}

async function settle() { await new Promise(resolve => setImmediate(resolve)); }

test('2xx [] is connected but protected Sidebar menus remain hidden, including standalone []', async () => {
  const harness = createProviderHarness(async () => response([]));
  try {
    let nav = harness.render();
    assert.equal(nav.authorizationStatus, 'loading');
    assert.deepEqual(nav.userAuthorizations, []);
    assert.match(renderSidebar(nav), /메뉴 권한을 확인하는 중입니다/);
    harness.runEffects();
    await settle();
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'success');
    assert.equal(nav.isGovernanceConnected, true);
    assert.deepEqual(nav.userAuthorizations, []);
    const html = renderSidebar(nav);
    assert.doesNotMatch(html, /href="\/ledger\/entry"/);
    assert.match(html, /href="\/system\/users"/); // Shared footer is outside menu grants.
    assert.match(renderSidebar(nav, 'DASHBOARD'), /href="\/"/); // Basic home navigation.
    assert.doesNotMatch(renderSidebar({ ...nav, userAuthorizations: [] }), /href="\/ledger\/entry"/);
  } finally { harness.cleanup(); }
});

test('individual grant and explicit MENU:* show business items; SYSTEM_ADMIN bypass remains', async () => {
  for (const code of ['MENU:/ledger/entry', 'MENU:*']) {
    const harness = createProviderHarness(async () => response([grant('ACCOUNTING_ADMIN', code)]));
    try {
      harness.render();
      harness.runEffects();
      await settle();
      const html = renderSidebar(harness.render());
      assert.match(html, /href="\/ledger\/entry"/);
      if (code !== 'MENU:*') assert.doesNotMatch(html, /href="\/ledger\/list"/);
      else assert.match(html, /href="\/ledger\/list"/);
    } finally { harness.cleanup(); }
  }
  const harness = createProviderHarness(async () => { throw new Error('admin should not fetch'); });
  try {
    let nav = harness.render();
    nav.setUserRole('SYSTEM_ADMIN');
    nav = harness.render();
    harness.runEffects();
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'success');
    assert.deepEqual(nav.userAuthorizations, []);
    assert.match(renderSidebar(nav), /href="\/ledger\/entry"/);
  } finally { harness.cleanup(); }
});

test('non-2xx, rejection and malformed responses never create a wildcard', async () => {
  const failures = [
    async () => response([], false),
    async () => { throw new Error('network unavailable'); },
    async () => response(null),
    async () => response({ authorizations: [] }),
    async () => response([grant('OTHER_ROLE', 'MENU:*')]),
    async () => response([{ functionCode: 'MENU:*' }]),
  ];
  for (const fetch of failures) {
    const harness = createProviderHarness(fetch);
    try {
      harness.render();
      harness.runEffects();
      await settle();
      const nav = harness.render();
      assert.equal(nav.authorizationStatus, 'unavailable');
      assert.equal(nav.isGovernanceConnected, false);
      assert.deepEqual(nav.userAuthorizations, []);
      const html = renderSidebar(nav);
      assert.match(html, /메뉴 권한을 불러오지 못했습니다/);
      assert.doesNotMatch(html, /href="\/ledger\/entry"/);
      assert.match(renderSidebar({ ...nav, isCollapsed: true }), /aria-label="메뉴 권한을 불러오지 못했습니다\."/);
    } finally { harness.cleanup(); }
  }
});

test('provider uses same-origin BFF credentials and no-store without a public API URL', async () => {
  const calls = [];
  const harness = createProviderHarness(async (url, options) => {
    calls.push({ url, options });
    return response([]);
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    assert.equal(harness.render().authorizationStatus, 'success');
    assert.equal(calls.length, 1);
    assert.equal(calls[0].url, '/api/audit/roles/ACCOUNTING_ADMIN/authorizations');
    assert.equal(calls[0].options.credentials, 'same-origin');
    assert.equal(calls[0].options.cache, 'no-store');
  } finally { harness.cleanup(); }
});

test('a WRITE-only menu grant does not expose a Sidebar item', async () => {
  const harness = createProviderHarness(async () => response([
    { id: 1, roleCode: 'ACCOUNTING_ADMIN', functionCode: 'MENU:*', accessType: 'WRITE' },
  ]));
  try {
    harness.render();
    harness.runEffects();
    await settle();
    const nav = harness.render();
    assert.equal(nav.authorizationStatus, 'success');
    assert.deepEqual(nav.userAuthorizations, []);
    assert.doesNotMatch(renderSidebar(nav), /href="\/ledger\/entry"/);
  } finally { harness.cleanup(); }
});

test('role transitions hide stale grants immediately and ignore late earlier responses', async () => {
  let finishOldRequest;
  const harness = createProviderHarness((url) => {
    if (url.includes('/ACCOUNTING_ADMIN/')) return new Promise(resolve => { finishOldRequest = resolve; });
    return Promise.resolve(response([]));
  });
  try {
    let nav = harness.render();
    harness.runEffects();
    nav.setUserRole('USER');
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'loading');
    assert.deepEqual(nav.userAuthorizations, []);
    harness.runEffects();
    finishOldRequest(response([grant('ACCOUNTING_ADMIN', 'MENU:*')]));
    await settle();
    nav = harness.render();
    assert.equal(nav.userRole, 'USER');
    assert.equal(nav.authorizationStatus, 'success');
    assert.deepEqual(nav.userAuthorizations, []);
    assert.doesNotMatch(renderSidebar(nav), /href="\/ledger\/entry"/);
  } finally { harness.cleanup(); }
});

test('a successful grant can be replaced by [] and then a failed role request clears grants', async () => {
  const harness = createProviderHarness(url => {
    if (url.includes('/ACCOUNTING_ADMIN/')) return Promise.resolve(response([grant('ACCOUNTING_ADMIN', 'MENU:*')]));
    if (url.includes('/USER/')) return Promise.resolve(response([]));
    return Promise.resolve(response([], false));
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    let nav = harness.render();
    assert.match(renderSidebar(nav), /href="\/ledger\/entry"/);
    nav.setUserRole('USER');
    nav = harness.render();
    assert.deepEqual(nav.userAuthorizations, []);
    assert.doesNotMatch(renderSidebar(nav), /href="\/ledger\/entry"/);
    harness.runEffects();
    await settle();
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'success');
    assert.deepEqual(nav.userAuthorizations, []);
    nav.setUserRole('RISK_MANAGER');
    harness.render();
    harness.runEffects();
    await settle();
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'unavailable');
    assert.deepEqual(nav.userAuthorizations, []);
  } finally { harness.cleanup(); }
});

test('a later successful [] revokes the same role\'s previous menu grant', async () => {
  let accountingRequests = 0;
  const harness = createProviderHarness(url => {
    if (url.includes('/ACCOUNTING_ADMIN/')) {
      accountingRequests++;
      return Promise.resolve(response(accountingRequests === 1 ? [grant('ACCOUNTING_ADMIN', 'MENU:*')] : []));
    }
    return Promise.resolve(response([]));
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    let nav = harness.render();
    assert.match(renderSidebar(nav), /href="\/ledger\/entry"/);
    nav.setUserRole('USER');
    harness.render();
    harness.runEffects();
    await settle();
    nav = harness.render();
    nav.setUserRole('ACCOUNTING_ADMIN');
    harness.render();
    harness.runEffects();
    await settle();
    nav = harness.render();
    assert.equal(accountingRequests, 2);
    assert.equal(nav.authorizationStatus, 'success');
    assert.equal(nav.isGovernanceConnected, true);
    assert.deepEqual(nav.userAuthorizations, []);
    assert.doesNotMatch(renderSidebar(nav), /href="\/ledger\/entry"/);
  } finally { harness.cleanup(); }
});

test('login route clears grants immediately and returning to the app refetches the same role', async () => {
  let requests = 0;
  const harness = createProviderHarness(async () => response(++requests === 1
    ? [grant('ACCOUNTING_ADMIN', 'MENU:*')]
    : []));
  try {
    harness.setPathname('/login');
    let nav = harness.render();
    assert.equal(nav.authorizationStatus, 'unavailable');
    harness.runEffects();
    assert.equal(requests, 0);

    harness.setPathname('/');
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'loading');
    harness.runEffects();
    await settle();
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'success');
    assert.match(renderSidebar(nav), /href="\/ledger\/entry"/);

    harness.setPathname('/login');
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'unavailable');
    assert.deepEqual(nav.userAuthorizations, []);
    assert.doesNotMatch(renderSidebar(nav), /href="\/ledger\/entry"/);
    harness.runEffects();

    harness.setPathname('/');
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'loading');
    assert.deepEqual(nav.userAuthorizations, []);
    harness.runEffects();
    await settle();
    nav = harness.render();
    assert.equal(requests, 2);
    assert.equal(nav.authorizationStatus, 'success');
    assert.deepEqual(nav.userAuthorizations, []);
  } finally { harness.cleanup(); }
});

test('app route navigation hides stale grants and a later 401 leaves menus unavailable', async () => {
  let requests = 0;
  const harness = createProviderHarness(async () => ++requests === 1
    ? response([grant('ACCOUNTING_ADMIN', 'MENU:*')])
    : response([], false));
  try {
    harness.render();
    harness.runEffects();
    await settle();
    assert.match(renderSidebar(harness.render()), /href="\/ledger\/entry"/);
    harness.setPathname('/system/menus');
    let nav = harness.render();
    assert.equal(nav.authorizationStatus, 'loading');
    assert.deepEqual(nav.userAuthorizations, []);
    harness.runEffects();
    await settle();
    nav = harness.render();
    assert.equal(nav.authorizationStatus, 'unavailable');
    assert.equal(nav.isGovernanceConnected, false);
    assert.doesNotMatch(renderSidebar(nav), /href="\/ledger\/entry"/);
  } finally { harness.cleanup(); }
});

test('late pre-logout response cannot restore grants on the login route', async () => {
  let finishRead;
  const harness = createProviderHarness(() => new Promise(resolve => { finishRead = resolve; }));
  try {
    harness.render();
    harness.runEffects();
    harness.setPathname('/login');
    let nav = harness.render();
    assert.equal(nav.authorizationStatus, 'unavailable');
    harness.runEffects();
    finishRead(response([grant('ACCOUNTING_ADMIN', 'MENU:*')]));
    await settle();
    nav = harness.render();
    assert.deepEqual(nav.userAuthorizations, []);
    assert.equal(nav.isGovernanceConnected, false);
  } finally { harness.cleanup(); }
});

function createPageHarness(fetch) {
  const states = [];
  const refs = [];
  const effects = [];
  let stateCursor = 0;
  let refCursor = 0;
  let effectCursor = 0;
  const hooks = {
    ...React,
    useState(initial) {
      const index = stateCursor++;
      if (!(index in states)) states[index] = initial;
      return [states[index], value => { states[index] = typeof value === 'function' ? value(states[index]) : value; }];
    },
    useRef(initial) {
      const index = refCursor++;
      if (!(index in refs)) refs[index] = { current: initial };
      return refs[index];
    },
    useEffect(callback, dependencies) {
      const index = effectCursor++;
      const previous = effects[index];
      if (!previous || dependencies.some((value, i) => value !== previous.dependencies[i])) {
        effects[index] = { callback, dependencies, cleanup: previous?.cleanup, pending: true };
      }
    },
  };
  const PageHeader = ({ title, description, actions }) => React.createElement('header', null, title, description, actions);
  const { default: Page } = loadSource(join(frontendRoot, 'src/app/system/menus/page.tsx'), {
    react: hooks,
    fetch,
    'lucide-react': icons,
    '@/components/ui/PageHeader': { __esModule: true, default: PageHeader },
    '@/components/layout/menus': { allMenus: [{ category: 'ACCOUNTING', group: 'Accounting', items: [{ href: '/ledger/entry', label: 'Ledger' }] }] },
  });
  return {
    render() {
      stateCursor = 0;
      refCursor = 0;
      effectCursor = 0;
      return Page();
    },
    runEffects() {
      for (const effect of effects) {
        if (effect?.pending) {
          effect.cleanup?.();
          effect.cleanup = effect.callback();
          effect.pending = false;
        }
      }
    },
    cleanup() { for (const effect of effects) effect?.cleanup?.(); },
  };
}

function findButton(node, title) {
  if (Array.isArray(node)) return node.map(child => findButton(child, title)).find(Boolean);
  if (!node || typeof node !== 'object') return undefined;
  if (node.type === 'button' && node.props.title === title) return node;
  return findButton(node.props?.children, title) ?? findButton(node.props?.actions, title);
}

function pageResponse(data, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: async () => data };
}

const userRole = [{ roleCode: 'USER', roleName: 'User' }];

test('menu page loads every role grant through authenticated same-origin BFF and accepts an empty role list', async () => {
  const calls = [];
  const harness = createPageHarness(async (url, options) => {
    calls.push({ url, options });
    return pageResponse(url === '/api/audit/roles' ? userRole : []);
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    const html = renderToStaticMarkup(harness.render());
    assert.match(html, /권한 조회 완료/);
    assert.match(html, /USER/);
    assert.doesNotMatch(html, /Mock|Governance 연동됨/);
    assert.deepEqual(calls.map(call => call.url), ['/api/audit/roles', '/api/audit/roles/USER/authorizations']);
    for (const call of calls) {
      assert.equal(call.options.credentials, 'same-origin');
      assert.equal(call.options.cache, 'no-store');
    }
  } finally { harness.cleanup(); }

  const empty = createPageHarness(async () => pageResponse([]));
  try {
    empty.render();
    empty.runEffects();
    await settle();
    const html = renderToStaticMarkup(empty.render());
    assert.match(html, /권한 조회 완료/);
    assert.match(html, /등록된 역할이 없습니다/);
  } finally { empty.cleanup(); }
});

test('menu page accepts lowercase and hyphenated backend role codes and encodes their path', async () => {
  const roleCode = 'risk-manager v2';
  const calls = [];
  const harness = createPageHarness(async (url, options) => {
    calls.push({ url, options });
    if (url === '/api/audit/roles') return pageResponse([{ roleCode, roleName: 'Risk manager' }]);
    if (options.method === 'POST') return pageResponse({ id: null });
    return pageResponse([]);
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    assert.match(renderToStaticMarkup(harness.render()), /risk-manager v2/);
    assert.equal(calls[1].url, '/api/audit/roles/risk-manager%20v2/authorizations');
    findButton(harness.render(), '권한 부여 요청').props.onClick();
    await settle();
    const post = calls.find(call => call.options.method === 'POST');
    assert.equal(post.url, '/api/audit/roles/risk-manager%20v2/authorizations');
    assert.match(renderToStaticMarkup(harness.render()), /권한 반영은 확인되지 않았습니다/);
  } finally { harness.cleanup(); }
});

test('menu page rejects role codes that cannot form a safe BFF path segment', async () => {
  for (const roleCode of ['a/b', '..', 'a\\b', 'x'.repeat(51), '\ud800']) {
    const harness = createPageHarness(async () => pageResponse([{ roleCode, roleName: 'Role' }]));
    try {
      harness.render();
      harness.runEffects();
      await settle();
      const html = renderToStaticMarkup(harness.render());
      assert.match(html, /권한 조회 불가/);
      assert.equal(findButton(harness.render(), '권한 부여 요청'), undefined);
    } finally { harness.cleanup(); }
  }
});

test('menu page rejects partial, mismatched and malformed role authorization loads', async () => {
  const failures = [
    pageResponse([], 401),
    pageResponse([], 403),
    pageResponse([], 404),
    pageResponse([grant('OTHER_ROLE', 'MENU:/ledger/entry')]),
    pageResponse(null),
  ];
  for (const authResponse of failures) {
    const actual = createPageHarness(async url => url === '/api/audit/roles' ? pageResponse(userRole) : authResponse);
    try {
      actual.render();
      actual.runEffects();
      await settle();
      const html = renderToStaticMarkup(actual.render());
      assert.match(html, /권한 조회 불가/);
      assert.match(html, /메뉴 권한을 불러오지 못했습니다/);
      assert.doesNotMatch(html, /USER/);
      assert.equal(findButton(actual.render(), '권한 부여 요청'), undefined);
    } finally { actual.cleanup(); }
  }

  const partial = createPageHarness(async url => {
    if (url === '/api/audit/roles') return pageResponse([...userRole, { roleCode: 'AUDITOR', roleName: 'Auditor' }]);
    if (url.includes('/USER/')) return pageResponse([grant('USER', 'MENU:/ledger/entry')]);
    throw new Error('Second role unavailable');
  });
  try {
    partial.render();
    partial.runEffects();
    await settle();
    const html = renderToStaticMarkup(partial.render());
    assert.match(html, /권한 조회 불가/);
    assert.doesNotMatch(html, /USER|AUDITOR/);
  } finally { partial.cleanup(); }
});

test('menu page never simulates a POST grant and reports pending approval after authoritative refetch', async () => {
  const calls = [];
  const harness = createPageHarness(async (url, options) => {
    calls.push({ url, options });
    if (url === '/api/audit/roles') return pageResponse(userRole);
    if (options.method === 'POST') return pageResponse({ id: null }, 200);
    return pageResponse([]);
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    const button = findButton(harness.render(), '권한 부여 요청');
    assert.ok(button);
    button.props.onClick();
    assert.match(renderToStaticMarkup(harness.render()), /권한 확인 중/);
    await settle();
    const html = renderToStaticMarkup(harness.render());
    assert.match(html, /권한 반영은 확인되지 않았습니다/);
    assert.doesNotMatch(html, /현재 권한 반영을 확인했습니다/);
    assert.ok(findButton(harness.render(), '권한 부여 요청'));
    const post = calls.find(call => call.options.method === 'POST');
    assert.equal(post.url, '/api/audit/roles/USER/authorizations');
    assert.equal(post.options.credentials, 'same-origin');
    assert.equal(post.options.cache, 'no-store');
    assert.deepEqual(JSON.parse(post.options.body), { functionCode: 'MENU:/ledger/entry', accessType: 'READ', dataScope: 'GLOBAL' });
    assert.equal(calls.filter(call => call.url === '/api/audit/roles').length, 2);
  } finally { harness.cleanup(); }
});

test('menu page treats a DELETE 202 approval as pending while the grant remains', async () => {
  const calls = [];
  const harness = createPageHarness(async (url, options) => {
    calls.push({ url, options });
    if (url === '/api/audit/roles') return pageResponse(userRole);
    if (options.method === 'DELETE') return pageResponse({ status: 'PENDING' }, 202);
    return pageResponse([grant('USER', 'MENU:/ledger/entry')]);
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    findButton(harness.render(), '권한 회수 요청').props.onClick();
    await settle();
    assert.match(renderToStaticMarkup(harness.render()), /권한 반영은 확인되지 않았습니다/);
    assert.ok(findButton(harness.render(), '권한 회수 요청'));
    const del = calls.find(call => call.options.method === 'DELETE');
    assert.equal(del.url, '/api/audit/authorizations/1');
    assert.equal(del.options.credentials, 'same-origin');
    assert.equal(del.options.cache, 'no-store');
  } finally { harness.cleanup(); }
});

test('menu page disables data after a failed write and refuses a second in-flight write', async () => {
  let finishWrite;
  let writes = 0;
  const harness = createPageHarness(async (url, options) => {
    if (url === '/api/audit/roles') return pageResponse(userRole);
    if (options.method === 'POST') {
      writes++;
      return new Promise(resolve => { finishWrite = resolve; });
    }
    return pageResponse([]);
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    const button = findButton(harness.render(), '권한 부여 요청');
    button.props.onClick();
    button.props.onClick();
    assert.equal(writes, 1);
    finishWrite(pageResponse(null, 404));
    await settle();
    const html = renderToStaticMarkup(harness.render());
    assert.match(html, /권한 조회 불가/);
    assert.match(html, /변경 요청 결과를 확인하지 못했습니다/);
    assert.equal(findButton(harness.render(), '권한 부여 요청'), undefined);
  } finally { harness.cleanup(); }
});

test('menu page confirms only POST and DELETE changes visible in the authoritative refetch', async () => {
  for (const initialGrant of [false, true]) {
    let grantPresent = initialGrant;
    const harness = createPageHarness(async (url, options) => {
      if (url === '/api/audit/roles') return pageResponse(userRole);
      if (options.method === 'POST' || options.method === 'DELETE') {
        grantPresent = !initialGrant;
        return pageResponse({}, options.method === 'DELETE' ? 202 : 200);
      }
      return pageResponse(grantPresent ? [grant('USER', 'MENU:/ledger/entry')] : []);
    });
    try {
      harness.render();
      harness.runEffects();
      await settle();
      const title = initialGrant ? '권한 회수 요청' : '권한 부여 요청';
      findButton(harness.render(), title).props.onClick();
      await settle();
      const html = renderToStaticMarkup(harness.render());
      assert.match(html, /현재 권한 반영을 확인했습니다/);
      assert.doesNotMatch(html, /권한 반영은 확인되지 않았습니다/);
      assert.ok(findButton(harness.render(), initialGrant ? '권한 부여 요청' : '권한 회수 요청'));
    } finally { harness.cleanup(); }
  }
});

test('menu page marks an accepted write unavailable when authoritative refetch fails', async () => {
  let wrote = false;
  const harness = createPageHarness(async (url, options) => {
    if (url === '/api/audit/roles') return pageResponse(userRole);
    if (options.method === 'POST') { wrote = true; return pageResponse({ id: null }); }
    return pageResponse([], wrote ? 503 : 200);
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    findButton(harness.render(), '권한 부여 요청').props.onClick();
    await settle();
    const html = renderToStaticMarkup(harness.render());
    assert.match(html, /권한 조회 불가/);
    assert.match(html, /변경 요청 후 현재 권한을 확인하지 못했습니다/);
    assert.doesNotMatch(html, /현재 권한 반영을 확인했습니다/);
    assert.equal(findButton(harness.render(), '권한 부여 요청'), undefined);
  } finally { harness.cleanup(); }
});

test('menu page ignores a late older refresh after a newer empty result', async () => {
  let finishOld;
  let roleReads = 0;
  const harness = createPageHarness(async url => {
    if (url === '/api/audit/roles') {
      roleReads++;
      if (roleReads === 2) return new Promise(resolve => { finishOld = resolve; });
      return pageResponse(roleReads === 3 ? [] : userRole);
    }
    return pageResponse([grant('USER', 'MENU:/ledger/entry')]);
  });
  try {
    harness.render();
    harness.runEffects();
    await settle();
    const refresh = findButton(harness.render(), undefined);
    refresh.props.onClick();
    refresh.props.onClick(); // Exercise the generation guard even if an old event races with the disabled UI.
    await settle();
    assert.match(renderToStaticMarkup(harness.render()), /등록된 역할이 없습니다/);
    finishOld(pageResponse(userRole));
    await settle();
    const html = renderToStaticMarkup(harness.render());
    assert.match(html, /등록된 역할이 없습니다/);
    assert.doesNotMatch(html, /USER/);
  } finally { harness.cleanup(); }
});
