'use strict';

const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const { readFileSync, lstatSync } = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.resolve(__dirname, '../..');
const archivePath = 'docs/history/GEMINI_REVIEW_PROMPT_ARCHIVE_2026-09-10.md';
// Only these two approved document bodies are read. Synthetic evidence never
// executes commands or opens paths; this is a contract model, not a sandbox.
const document = readFileSync(path.join(root, 'GEMINI_REVIEW_PROMPT.md'), 'utf8');
const archive = readFileSync(path.join(root, archivePath), 'utf8');
const originalBytes = 89333;
const originalHash = '5ef3b720e8397b70f5f34fed02c571a375cb8d0106e99b1b970060c90ace35bf';
const begin = '<!-- BEGIN VERBATIM ORIGINAL -->\n``````text\n';
const end = '``````\n<!-- END VERBATIM ORIGINAL -->';
const fields = {
  Issue: '<ISSUE>',
  'Base SHA': '<BASE_SHA>',
  'Head SHA': '<HEAD_SHA>',
  'Branch / Worktree': '<BRANCH_WORKTREE>',
  'Review allowlist': '<ALLOWLIST_EXACT_PATHS>',
  'Verification evidence': '<EVIDENCE_OR_NOT_RUN>',
};
const gateRules = [
  'Issue/base/head/branch·worktree/allowlist 입력 누락·빈 값·미치환 placeholder 또는 범위 불명확 시 **HOLD: 검토 시작 보류**',
  '역사 자료에서 값을 채우지 않는다',
  '검증 근거가 없거나 `NOT_RUN`이면 검증 성공으로 기록하지 않는다',
  '완료 판정을 보류한다',
  '부모가 확인한 checkout/PR의 실제 head와 입력 head, 비교 base를 내용 조회 전에 대조한다',
  'SHA가 다르거나 확인되지 않으면 HOLD한다',
  'head/base 또는 작업 트리 내용이 바뀌면 기존 검수 판정을 무효화',
  '미커밋 변경은 head 검증에 포함된 것으로 간주하지 않는다',
  '현재 head SHA와 일치하는 검증 근거만 현재 결과로 인정한다',
  '다른 SHA·출처 불명·과거 archive의 결과는 재사용하지 않고 `NOT_RUN`으로 보고한다',
];

function inputBlock(text) {
  const match = text.match(/## Current Review Input\n\n```text\n([\s\S]*?)\n```/);
  assert.ok(match, 'Current input must be a distinct copyable block');
  return match[1];
}

function assertTemplateContract(text) {
  assert.ok(text.split('\n').length <= 150, 'Reusable contract must stay short');
  for (const [label, placeholder] of Object.entries(fields)) {
    assert.ok(inputBlock(text).includes(`${label}: ${placeholder}`), `Missing blank field ${label}`);
  }
  const activeGate = text.slice(text.indexOf('## Current Review Input'), text.indexOf('## Review Content Read Scope'));
  for (const rule of gateRules) assert.ok(activeGate.includes(rule), `Missing current gate: ${rule}`);
  assert.ok(text.includes('명령·실행 시각·환경·대상 head SHA·결과·로그 위치'), 'Evidence provenance is explicit');
  assert.ok(text.includes('과거 성공·NO-SOURCE·SKIPPED는 현재 테스트 PASS가 아니다'));
  assert.ok(text.includes('archive는 자동 열람/실행하지 않는다'));
  assert.ok(text.includes(`[과거 요청·결과 원문 보관](${archivePath})`));
  assert.ok(text.includes('읽기 전용 독립 리뷰어'));
  assert.ok(text.includes('별도 계정/권한 격리나 GitHub APPROVED를 뜻하지 않는다'));
  assert.doesNotMatch(text, /Issue #20\b|2026-06-19|최신 main|`git diff --check` passed\.|## Current Review Request/);
}

function assertArchivePreserved(text) {
  assert.equal(text.split(begin).length, 2, 'One original start boundary');
  assert.equal(text.split(end).length, 2, 'One original end boundary');
  const from = text.indexOf(begin) + begin.length;
  const to = text.indexOf(end);
  assert.ok(to > from);
  const bytes = Buffer.from(text.slice(from, to), 'utf8');
  assert.equal(bytes.length, originalBytes, 'Original byte length');
  assert.equal(createHash('sha256').update(bytes).digest('hex'), originalHash, 'Original UTF-8 bytes must not change');
  const wrapper = text.slice(0, from - begin.length) + text.slice(to + end.length);
  assert.ok(wrapper.includes('비활성 역사 자료'));
  assert.ok(wrapper.includes('현재 지시, 읽기/실행 승인 또는 현재 검증 증거가 아니다'));
  assert.ok(wrapper.includes('20bb8e48b1a4d34bc6a3ba0ee12202614c385590:GEMINI_REVIEW_PROMPT.md'));
  return wrapper;
}

function renderInput(values) {
  return inputBlock(document).replace(/^(.*?): .*$/gm, (line, label) =>
    Object.hasOwn(values, label) ? `${label}: ${values[label]}` : '');
}

function reviewDecision(text, renderedInput, observed, evidence) {
  // Validate the actual document first so fixtures cannot stay green after its
  // current gate is deleted. Observations are synthetic parent-supplied facts.
  try { assertTemplateContract(text); } catch { return { start: 'HOLD', result: 'NOT_RUN' }; }
  const input = Object.fromEntries(renderedInput.split('\n').filter(Boolean).map(line => {
    const at = line.indexOf(':');
    return [line.slice(0, at), line.slice(at + 1).trim()];
  }));
  const required = ['Issue', 'Base SHA', 'Head SHA', 'Branch / Worktree', 'Review allowlist'];
  if (required.some(key => !input[key] || /<[^>]+>/.test(input[key]))
      || !/^(?:#\d+|https:\/\/github\.com\/[^/]+\/[^/]+\/issues\/\d+)$/.test(input.Issue)
      || !/^[a-f0-9]{40}$/.test(input['Base SHA']) || !/^[a-f0-9]{40}$/.test(input['Head SHA'])
      || input['Review allowlist'] === '[]'
      || observed.verified !== true || observed.dirty !== false
      || observed.head !== input['Head SHA'] || observed.base !== input['Base SHA']) {
    return { start: 'HOLD', result: 'NOT_RUN' };
  }
  const completeEvidence = evidence && ['command', 'time', 'environment', 'head', 'result', 'log'].every(key => evidence[key]);
  const result = input['Verification evidence'] && !/^NOT_RUN\b|<[^>]+>/.test(input['Verification evidence'])
    && completeEvidence && evidence.head === input['Head SHA'] && evidence.origin === 'current'
    && evidence.result === 'PASS' ? 'PASS' : 'NOT_RUN';
  return { start: 'REVIEW', result };
}

const fresh = { Issue: '#999999', 'Base SHA': 'a'.repeat(40), 'Head SHA': 'b'.repeat(40),
  'Branch / Worktree': 'agent/999999-example /tmp/synthetic', 'Review allowlist': 'docs/synthetic-review.md',
  'Verification evidence': 'synthetic current evidence' };
const observed = { head: fresh['Head SHA'], base: fresh['Base SHA'], verified: true, dirty: false };
const evidence = { command: 'synthetic-check', time: '2030-01-01T00:00:00Z', environment: 'synthetic',
  head: fresh['Head SHA'], result: 'PASS', log: 'synthetic-log', origin: 'current' };

test('actual root is a blank, short current contract', () => assertTemplateContract(document));
test('archive preserves every original byte without recertifying historical results', () => assertArchivePreserved(archive));
test('fresh synthetic Issue accepts only its current scope and matching evidence', () => {
  const input = renderInput(fresh);
  assert.equal(reviewDecision(document, input, observed, evidence).result, 'PASS');
  assert.doesNotMatch(input, /Issue #20|2026-06-19|최신 main/);
  assert.equal(input.split('Review allowlist: ')[1].split('\n')[0], fresh['Review allowlist']);
  assert.deepEqual(reviewDecision(document, input, observed, evidence), reviewDecision(document, input, observed, evidence));
});
for (const field of ['Issue', 'Base SHA', 'Head SHA', 'Branch / Worktree', 'Review allowlist']) {
  for (const kind of ['missing', 'empty', 'whitespace', 'placeholder']) {
    test(`${field} ${kind} holds before review without historical defaults`, () => {
      const values = { ...fresh, [field]: kind === 'placeholder' ? fields[field] : kind === 'whitespace' ? '   ' : '' };
      if (kind === 'missing') delete values[field];
      assert.deepEqual(reviewDecision(document, renderInput(values), observed, evidence), { start: 'HOLD', result: 'NOT_RUN' });
    });
  }
}
for (const [name, changes] of [
  ['unverified checkout', { verified: false }], ['mismatched actual head', { head: 'c'.repeat(40) }],
  ['changed base', { base: 'c'.repeat(40) }], ['uncommitted changes', { dirty: true }], ['unknown dirty state', { dirty: undefined }],
]) test(`${name} invalidates current review`, () => {
  assert.deepEqual(reviewDecision(document, renderInput(fresh), { ...observed, ...changes }, evidence), { start: 'HOLD', result: 'NOT_RUN' });
});
for (const [name, values] of [['short SHA', { 'Head SHA': 'bbb' }], ['invalid Issue', { Issue: 'old work' }], ['empty list', { 'Review allowlist': '[]' }]]) {
  test(`${name} cannot start review`, () => assert.equal(reviewDecision(document, renderInput({ ...fresh, ...values }), observed, evidence).start, 'HOLD'));
}
for (const [name, sample] of [
  ['missing', undefined], ['stale SHA', { ...evidence, head: 'c'.repeat(40) }],
  ['archive origin', { ...evidence, origin: 'archive' }], ['unknown origin', { ...evidence, origin: undefined }],
  ...['NOT_RUN', 'FAIL', 'SKIPPED', 'NO-SOURCE'].map(result => [result, { ...evidence, result }]),
  ...['command', 'time', 'environment', 'head', 'result', 'log'].map(key => [`missing ${key}`, { ...evidence, [key]: '' }]),
]) test(`${name} evidence never becomes a current PASS`, () => {
  assert.deepEqual(reviewDecision(document, renderInput(fresh), observed, sample), { start: 'REVIEW', result: 'NOT_RUN' });
});
for (const value of ['', 'NOT_RUN', 'NOT_RUN: no execution', '<EVIDENCE_OR_NOT_RUN>']) {
  test(`unverified evidence input ${value || '(empty)'} overrides supplied synthetic success`, () => {
    assert.equal(reviewDecision(document, renderInput({ ...fresh, 'Verification evidence': value }), observed, evidence).result, 'NOT_RUN');
  });
}

for (const rule of gateRules) test(`removing actual gate fails closed: ${rule}`, () => {
  const changed = document.replace(rule, '');
  assert.notEqual(changed, document);
  assert.throws(() => assertTemplateContract(changed));
  assert.deepEqual(reviewDecision(changed, renderInput(fresh), observed, evidence), { start: 'HOLD', result: 'NOT_RUN' });
});
for (const [name, mutate] of [
  ['byte replacement', text => text.replace('# Review Content Read Scope', '# Review Content Read ScopE')],
  ['line ending rewrite', text => text.replaceAll('\n', '\r\n')],
  ['missing boundary', text => text.replace(end, '')],
  ['duplicate source', text => text + begin + 'duplicate' + end],
  ['inactive boundary removed', text => text.replace('**비활성 역사 자료**', '**자료**')],
]) test(`archive mutation rejected: ${name}`, () => {
  const changed = mutate(archive);
  assert.notEqual(changed, archive);
  assert.throws(() => assertArchivePreserved(changed));
});

const permittedLinks = new Set(['AGENTS.md', 'docs/ai-harness/42-code-documentation-quality.md', archivePath, 'GEMINI_REVIEW_PROMPT.md']);
function assertLinks(text, directory) {
  for (const [, target] of text.matchAll(/\[[^\]\n]+\]\(([^)\s]+)\)/g)) {
    if (target.startsWith('#')) {
      const headings = [...text.matchAll(/^#+ (.+)$/gm)].map(match => match[1].toLowerCase().replace(/[^\p{L}\p{N} _-]/gu, '').replace(/ /g, '-'));
      assert.ok(headings.includes(target.slice(1)), `Broken anchor ${target}`);
      continue;
    }
    const relative = path.posix.normalize(path.posix.join(directory, target));
    // Validate against fixed approved targets before touching filesystem metadata;
    // never traverse a link from archive prose or open an arbitrary linked body.
    assert.ok(permittedLinks.has(relative), `Unapproved link ${target}`);
    const components = relative.split('/');
    for (let count = 1; count <= components.length; count++) {
      const component = components.slice(0, count).join('/');
      const stat = lstatSync(path.join(root, component));
      assert.ok(!stat.isSymbolicLink(), `Unresolved link identity ${component}`);
      assert.ok(count === components.length ? stat.isFile() : stat.isDirectory(), `Invalid target type ${component}`);
    }
  }
}
test('active Markdown links exist; historical fenced links remain inactive', () => {
  assertLinks(document, '.');
  assertLinks(assertArchivePreserved(archive), 'docs/history');
});
for (const target of ['#missing-heading', 'docs/missing.md', '../outside.md', '.env', 'https://example.invalid']) {
  test(`link check rejects ${target} before any unapproved target read`, () => {
    assert.throws(() => assertLinks(`${document}\n[invalid](${target})`, '.'));
  });
}
