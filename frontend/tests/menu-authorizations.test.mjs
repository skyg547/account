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
function createProviderHarness(fetch, apiUrl = 'https://governance.test') {
  const states = [];
  const effects = [];
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
    process: { env: { NEXT_PUBLIC_GOVERNANCE_API_URL: apiUrl } },
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

test('non-2xx, rejection, malformed and missing responses never create a wildcard', async () => {
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
  const missing = createProviderHarness(async () => { throw new Error('should not fetch'); }, '');
  try {
    missing.render();
    missing.runEffects();
    assert.equal(missing.render().authorizationStatus, 'unavailable');
    assert.deepEqual(missing.render().userAuthorizations, []);
  } finally { missing.cleanup(); }
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
