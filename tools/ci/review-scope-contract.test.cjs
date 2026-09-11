'use strict';

const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const test = require('node:test');

// This approved document is the only disk input. Fixture paths are never opened,
// resolved or inspected: the plan models the document, not an AI runtime sandbox.
const document = readFileSync(path.join(__dirname, '../../GEMINI_REVIEW_PROMPT.md'), 'utf8');
const heading = '## Review Content Read Scope';
const inventoryCommands = [
  'git status --short --branch',
  'git diff --name-only',
  'git diff --cached --name-only',
  'git ls-files --others --exclude-standard',
];
const trackedCommand = "git --literal-pathspecs diff --no-ext-diff --no-textconv -- '<exact-path>'";
const stagedCommand = "git --literal-pathspecs diff --cached --no-ext-diff --no-textconv -- '<exact-path>'";
const untrackedCommand = "Get-Content -LiteralPath '<exact-path>'";
const whitespaceCommand = "git --literal-pathspecs diff --check --no-ext-diff --no-textconv -- '<exact-path>'";
const stagedWhitespaceCommand = "git --literal-pathspecs diff --check --cached --no-ext-diff --no-textconv -- '<exact-path>'";
const whitespaceRule = '공백 검사도 변경 행 본문을 출력할 수 있으므로 위 filename-first/승인/제외 게이트를 동일하게 적용한다';
const whitespaceScope = '현재 비어 있지 않은 exact allowlist ∩ 해당 상태의 변경목록에서 확정한 한 파일만 검사한다';
const requiredRules = [
  '현재 Issue/부모의 명시적 콘텐츠 review allowlist ∩ 실제 변경목록',
  '없거나 비어 있으면 내용을 읽지 않고 부모에게 확인한다',
  '상태/파일명 목록은 콘텐츠 읽기 승인이 아니다',
  '민감 가능 경로는 allowlist에 있어도 내용 조회 전에 제외한다',
  '범위 밖·절대경로·상위경로 이동·링크/경로 정체 불명확은 읽지 않고 부모에게 확인한다',
  'wildcard·폴더 전체·Git pathspec magic·자동 신규파일 전체 열람·무범위 raw diff fallback은 금지한다',
];

function section(text, start, end) {
  const from = text.indexOf(start);
  assert.notEqual(from, -1, `Missing section: ${start}`);
  const to = text.indexOf(end, from + start.length);
  assert.notEqual(to, -1, `Missing section boundary: ${end}`);
  return text.slice(from, to);
}

function assertDocumentContract(text) {
  assert.ok(text.startsWith('# Gemini Review Prompt\n'), 'The reusable template must be the active entry point');
  const contract = section(text, heading, '## Review Handoff Checklist');
  const handoff = section(text, '## Review Handoff Checklist', '## Inactive History');
  assert.equal(text.split(heading).length - 1, 1, 'Keep a single active read-scope contract');

  for (const [name, entry] of [['active contract', contract]]) {
    for (const command of inventoryCommands) assert.ok(entry.includes(command), `${name}: filename inventory ${command}`);
    for (const rule of requiredRules) assert.ok(entry.includes(rule), `${name}: missing rule ${rule}`);
    for (const command of [trackedCommand, stagedCommand, untrackedCommand, whitespaceCommand, stagedWhitespaceCommand]) {
      assert.ok(entry.includes(command), `${name}: missing scoped content command ${command}`);
    }
    assert.ok(entry.includes(whitespaceRule), `${name}: whitespace output needs the content gate`);
    assert.ok(entry.includes(whitespaceScope), `${name}: whitespace needs a nonempty state-specific intersection`);
    assert.ok(entry.indexOf(requiredRules[0]) < entry.indexOf(whitespaceRule), `${name}: approval before whitespace gate`);
    assert.ok(entry.indexOf(whitespaceScope) < entry.indexOf(whitespaceCommand), `${name}: whitespace scope before command`);
    assert.ok(entry.indexOf('git ls-files --others --exclude-standard') < entry.indexOf(requiredRules[0]), `${name}: inventory before approval`);
    assert.ok(entry.indexOf(requiredRules[0]) < entry.indexOf(trackedCommand), `${name}: approval before content`);
    for (const sensitiveExample of ['.env*', '.claude/settings.local.json', 'credential', 'key']) {
      assert.ok(entry.includes(sensitiveExample), `${name}: sensitive exclusion ${sensitiveExample}`);
    }
  }
  assert.ok(contract.includes('과거 날짜·모듈 목록·검증 명령은 현재의 읽기/실행 승인이 아니다'), 'Historical scope must not authorize current reads');
  assert.ok(handoff.includes('[Review Content Read Scope](#review-content-read-scope)'), 'Handoff must link the read gate');
  assert.ok(handoff.includes('filename-first → 명시된 비민감 exact allowlist ∩ 변경목록 → scoped 조회/보류'), 'Handoff must retain the ordered gate');
  assert.ok(handoff.includes('범위가 없거나 비어 있거나 불명확하면 내용은 읽지 않고 부모에게 확인한다'), 'Handoff must fail closed');
  assert.ok(handoff.includes('공백 검사도 본문 출력 가능성이 있으므로 동일 게이트와 상태별 exact 경로 제한을 적용한다'), 'Handoff must scope whitespace diagnostics too');

  // Check executable-looking diff/read lines, not old prose reporting --check PASS.
  // This deliberately small regression guard is not a shell or natural-language parser.
  // --check can print offending line contents; it is not a metadata-only exception.
  const allowed = new Set([...inventoryCommands, trackedCommand, stagedCommand, untrackedCommand, whitespaceCommand, stagedWhitespaceCommand]);
  for (const line of text.split(/\r?\n/)) {
    const command = line.trim().replace(/^-\s+/, '');
    if (/^git(?: --literal-pathspecs)? diff\b|^Get-Content\b/.test(command)) {
      assert.ok(allowed.has(command), `Unapproved active content command: ${command}`);
    }
  }
  assert.doesNotMatch(text, /새 파일이 있으면 해당 파일도 확인|(?:read|open) all (?:new|untracked) files/i, 'Automatic new-file fallback is forbidden');
  assert.ok(contract.includes('AI의 실제 파일 접근을 강제하는 sandbox가 아니다'), 'Document must state the runtime limitation');
  assert.ok(contract.includes('CI 미연결'), 'Local test must not imply CI coverage');
}

function unsafePathReason(file) {
  if (typeof file !== 'string' || file.length === 0) return 'missing-path';
  // Do not normalize away traversal/absolute/pathspec syntax into an allowed path.
  // Conservative fixture spellings intentionally cover less than every filesystem.
  if (/^(?:[A-Za-z]:|\/|\\)/.test(file)) return 'absolute-path';
  if (file.split(/[\\/]/).some(part => part === '..' || part === '.')) return 'traversal';
  if (!/^[A-Za-z0-9_ .-]+(?:\/[A-Za-z0-9_ .-]+)*$/.test(file) || file.trim() !== file) return 'ambiguous-path';
  if (file.split('/').some(part => /^\.env/i.test(part))
      || file.toLowerCase() === '.claude/settings.local.json'
      || /(?:^|[\/_.-])(?:credentials?|secrets?|tokens?|passwords?|keys?)(?:$|[\/_.-])/i.test(file)
      || /(?:^|\/)id_(?:rsa|ed25519)(?:\.|$)|\.(?:pem|key|p12|pfx)$/i.test(file)) return 'sensitive-path';
  return null;
}

function planContentReads(text, { allowlist, changes, whitespaceOnly = false }) {
  // Couple every fixture to the actual document gate: removing a rule cannot
  // leave an independent, self-validating path planner green.
  try { assertDocumentContract(text); } catch { return { reads: [], holds: ['invalid-document'] }; }
  if (!Array.isArray(allowlist) || allowlist.length === 0) return { reads: [], holds: ['missing-or-empty-scope'] };
  if (allowlist.some(file => unsafePathReason(file))) return { reads: [], holds: ['unsafe-allowlist'] };
  if (!Array.isArray(changes) || changes.length === 0) return { reads: [], holds: ['empty-intersection'] };
  const reads = [];
  const holds = [];
  for (const change of changes) {
    const reason = unsafePathReason(change.path);
    if (reason) { holds.push(reason); continue; }
    if (!allowlist.includes(change.path)) { holds.push('outside-allowlist'); continue; }
    // Identity is explicitly supplied synthetic evidence, never an fs probe.
    if (change.identity !== 'regular-file') { holds.push('unresolved-link-or-identity'); continue; }
    if (!['tracked', 'staged', 'untracked'].includes(change.kind)) { holds.push('unknown-change-kind'); continue; }
    // Whitespace plans share all content gates. Untracked has no index/HEAD diff;
    // never replace that missing comparison with an unscoped repository check.
    if (whitespaceOnly && change.kind === 'untracked') { holds.push('untracked-not-diff-check'); continue; }
    const args = change.kind === 'untracked'
      ? ['Get-Content', '-LiteralPath', change.path]
      : ['git', '--literal-pathspecs', 'diff', ...(whitespaceOnly ? ['--check'] : []), ...(change.kind === 'staged' ? ['--cached'] : []), '--no-ext-diff', '--no-textconv', '--', change.path];
    reads.push({ kind: change.kind, path: change.path, args });
  }
  return { reads, holds };
}

test('actual document enforces filename-first and fail-closed content review', () => assertDocumentContract(document));

for (const kind of ['tracked', 'staged', 'untracked']) {
  test(`approved ${kind} gets only one exact content read`, () => {
    const file = 'docs/review notes.md';
    const result = planContentReads(document, { allowlist: [file], changes: [{ path: file, kind, identity: 'regular-file' }] });
    assert.deepEqual(result.holds, []);
    assert.equal(result.reads.length, 1);
    assert.equal(result.reads[0].args.at(-1), file);
    assert.equal(result.reads[0].args.includes('--cached'), kind === 'staged');
    assert.equal(result.reads[0].args.includes('-LiteralPath'), kind === 'untracked');
  });
}

const safe = { path: 'docs/review.md', kind: 'tracked', identity: 'regular-file' };
const deniedCases = [
  ['missing scope', undefined, [safe], 'missing-or-empty-scope'],
  ['empty scope', [], [safe], 'missing-or-empty-scope'],
  ['no changes', [safe.path], [], 'empty-intersection'],
  ['outside scope', ['docs/other.md'], [safe], 'outside-allowlist'],
  ['unresolved link', [safe.path], [{ ...safe, identity: 'unresolved-link' }], 'unresolved-link-or-identity'],
  ['missing identity', [safe.path], [{ path: safe.path, kind: 'untracked' }], 'unresolved-link-or-identity'],
  ['unknown change kind', [safe.path], [{ ...safe, kind: 'unknown' }], 'unknown-change-kind'],
];
for (const file of ['.env', 'fake/.env.example', '.claude/settings.local.json', 'fake/credentials.json', 'fake/private.key', 'fake/id_rsa']) {
  deniedCases.push([`sensitive inventory ${file}`, [safe.path], [{ ...safe, path: file }], 'sensitive-path']);
  deniedCases.push([`sensitive allowlist ${file}`, [file], [{ ...safe, path: file }], 'unsafe-allowlist']);
}
for (const [file, reason] of [
  ['/fake/review.md', 'absolute-path'], ['C:/fake/review.md', 'absolute-path'], ['\\\\fake\\review.md', 'absolute-path'],
  ['../review.md', 'traversal'], ['docs/../review.md', 'traversal'], ['docs\\..\\review.md', 'traversal'],
  ['docs/*', 'ambiguous-path'], ['docs/', 'ambiguous-path'], [':(top)review.md', 'ambiguous-path'], ['', 'missing-path'],
]) {
  deniedCases.push([`invalid inventory ${file || '(empty)'}`, [safe.path], [{ ...safe, path: file }], reason]);
  deniedCases.push([`invalid allowlist ${file || '(empty)'}`, [file], [safe], 'unsafe-allowlist']);
}
for (const [name, allowlist, changes, reason] of deniedCases) {
  test(`${name} produces no content-read or whitespace fallback`, () => {
    for (const whitespaceOnly of [false, true]) {
      const result = planContentReads(document, { allowlist, changes, whitespaceOnly });
      assert.deepEqual(result.reads, [], `Denied scope must never become an unscoped diff/read (whitespace=${whitespaceOnly})`);
      assert.deepEqual(result.holds, [reason]);
    }
  });
}

test('mixed inventory reads only the exact safe intersection, holding the rest', () => {
  const result = planContentReads(document, { allowlist: [safe.path], changes: [safe, { ...safe, path: 'docs/other.md' }, { ...safe, path: '.env.fake' }] });
  assert.deepEqual(result.reads.map(read => read.path), [safe.path]);
  assert.deepEqual(result.holds, ['outside-allowlist', 'sensitive-path']);
});

for (const kind of ['tracked', 'staged']) {
  test(`approved ${kind} whitespace plan selects only its exact changed file`, () => {
    const change = { ...safe, kind };
    const result = planContentReads(document, { allowlist: [safe.path], changes: [change], whitespaceOnly: true });
    assert.deepEqual(result.holds, []);
    assert.deepEqual(result.reads, [{ kind, path: safe.path, args: ['git', '--literal-pathspecs', 'diff', '--check', ...(kind === 'staged' ? ['--cached'] : []), '--no-ext-diff', '--no-textconv', '--', safe.path] }]);
    assert.deepEqual(planContentReads(document, { allowlist: [safe.path], changes: [change], whitespaceOnly: true }), result, 'Same input produces the same plan without side effects');
  });
}

test('one file in both states keeps separate exact whitespace comparisons', () => {
  const result = planContentReads(document, { allowlist: [safe.path], changes: [safe, { ...safe, kind: 'staged' }], whitespaceOnly: true });
  assert.deepEqual(result.holds, []);
  assert.deepEqual(result.reads.map(read => read.args.includes('--cached')), [false, true]);
  assert.deepEqual(result.reads.map(read => read.path), [safe.path, safe.path]);
});

test('untracked whitespace comparison is held without a whole-repository fallback', () => {
  assert.deepEqual(planContentReads(document, { allowlist: [safe.path], changes: [{ ...safe, kind: 'untracked' }], whitespaceOnly: true }), { reads: [], holds: ['untracked-not-diff-check'] });
});

test('synthetic outside-scope whitespace line is excluded from diagnostic selection', () => {
  // In-memory output model only: demonstrate why line diagnostics require scope,
  // without executing Git, reading fixture files or claiming to emulate Git fully.
  const rows = [{ ...safe, line: 'SYNTHETIC_APPROVED' }, { ...safe, path: 'docs/outside.md', line: 'SYNTHETIC_OUTSIDE_ALLOWLIST   ' }];
  const diagnostics = selected => selected.filter(row => /[ \t]+$/.test(row.line)).map(row => `${row.path}: ${row.line}`);
  assert.deepEqual(diagnostics(rows), ['docs/outside.md: SYNTHETIC_OUTSIDE_ALLOWLIST   ']);
  const plan = planContentReads(document, { allowlist: [safe.path], changes: rows, whitespaceOnly: true });
  assert.deepEqual(plan.reads.map(read => read.path), [safe.path]);
  assert.deepEqual(plan.holds, ['outside-allowlist']);
  assert.deepEqual(diagnostics(rows.filter(row => plan.reads.some(read => read.path === row.path))), []);
});

test('historical success prose is absent from the active template', () => {
  assert.ok(!document.includes('- `git diff --check` passed.'));
  assert.doesNotMatch(document, /## Current Review Request|## Current Handoff Context|# 2026-07-29/);
  assertDocumentContract(document);
});

const mutations = [
  ['remove top gate', text => text.replace(heading, '## Removed Scope')],
  ['remove inventory', text => text.replaceAll('git diff --cached --name-only', '')],
  ...requiredRules.map(rule => [`remove ${rule}`, text => text.replaceAll(rule, '')]),
  ['remove handoff gate', text => text.replace('[Review Content Read Scope](#review-content-read-scope)', 'review')],
  ['unscoped diff fallback', text => text.replace(heading, heading + '\n- git diff')],
  ['unscoped staged fallback', text => text.replace(heading, heading + '\n- git diff --cached')],
  ['active bare whitespace fallback', text => text.replace(heading, heading + '\n- git diff --check')],
  ['active staged bare whitespace fallback', text => text.replace(heading, heading + '\n- git diff --check --cached')],
  ['replace scoped whitespace with bare check', text => text.replace(whitespaceCommand, 'git diff --check')],
  ['remove whitespace content gate', text => text.replaceAll(whitespaceRule, '')],
  ['remove whitespace state-specific scope', text => text.replaceAll(whitespaceScope, '')],
  ['folder diff fallback', text => text.replace(heading, heading + '\n- git diff -- docs/')],
  ['new-file fallback', text => text.replace(heading, heading + '\n- 새 파일이 있으면 해당 파일도 확인')],
  ['English new-file fallback', text => text.replace(heading, heading + '\n- Read all untracked files')],
  ['move approval before inventory', text => text.replace('git status --short --branch', requiredRules[0] + '\ngit status --short --branch')],
  ['move content before approval', text => text.replace('git status --short --branch', trackedCommand + '\ngit status --short --branch')],
];
for (const [name, mutate] of mutations) {
  test(`in-memory document mutation is rejected: ${name}`, () => {
    assertDocumentContract(document);
    const mutated = mutate(document);
    assert.notEqual(mutated, document, 'Mutation must actually change the approved document');
    assert.throws(() => assertDocumentContract(mutated), assert.AssertionError);
    assert.deepEqual(planContentReads(mutated, { allowlist: [safe.path], changes: [safe] }), { reads: [], holds: ['invalid-document'] });
    assert.deepEqual(planContentReads(mutated, { allowlist: [safe.path], changes: [safe], whitespaceOnly: true }), { reads: [], holds: ['invalid-document'] });
  });
}
