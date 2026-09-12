'use strict';

// Offline only: the candidate trust policy below is a synthetic fixture, not a
// repository decision. Run: node --test tools/ci/agent-merge-guard.test.cjs
const test = require('node:test');
const assert = require('node:assert/strict');
const { prSnapshot, metadataDigest, evaluate, runGuard } = require('./agent-merge-guard.cjs');

const NOW = Date.parse('2026-09-12T00:00:00Z');
const HEAD = 'a'.repeat(40);
const BASE = 'b'.repeat(40);
const OTHER = 'c'.repeat(40);
const IDENTITY = { repository: 'fixture/account', number: 671 };
const POLICY = { mode: 'github-review-v1', reviewerIds: [202], maxAgeMs: 60_000 };
const BODY = 'Verification:\n```text\nnode --test\nPASS\n```\nMerge authority: independent integrator';
const clone = value => structuredClone(value);

function pullRequest() {
  return {
    number: 671, state: 'open', draft: false, title: 'Synthetic guard change', body: BODY,
    user: { id: 101 }, head: { sha: HEAD, ref: 'agent/671-test' },
    base: { sha: BASE, ref: 'main', repo: { full_name: IDENTITY.repository } },
    labels: [{ name: 'agent:codex' }, { name: 'status:needs-review' }],
  };
}

function approval(snapshot = prSnapshot(pullRequest(), IDENTITY), changes = {}) {
  return {
    id: 1, user: { id: 202 }, state: 'APPROVED', commit_id: HEAD,
    submitted_at: new Date(NOW - 1000).toISOString(),
    body: `Agent-Merge-Guard: v1 ${snapshot.repository}#${snapshot.number} ${snapshot.head} ${snapshot.base} ${metadataDigest(snapshot)}`,
    ...changes,
  };
}

function fixture() {
  const snapshot = prSnapshot(pullRequest(), IDENTITY);
  return { snapshot, reviews: [approval(snapshot)], policy: clone(POLICY), now: NOW };
}

function denied(input, problem) {
  const result = evaluate(input);
  assert.equal(result.ok, false);
  assert.ok(result.problems.includes(problem), JSON.stringify(result));
}

// Only the two allowed GET endpoints exist. Unexpected endpoints throw rather
// than silently succeed, and every attempted call is recorded for assertions.
function api({ prs = [pullRequest(), pullRequest()], reviewReads = [[approval()], [approval()]], failAt } = {}) {
  const calls = [];
  let prIndex = 0;
  let reviewIndex = -1;
  const pulls = new Proxy({
    get: async params => {
      calls.push({ method: 'get', params });
      if (calls.length === failAt) throw new Error('synthetic private diagnostic');
      const data = prs[prIndex++];
      return clone(data?.response || { status: 200, data });
    },
    listReviews: async params => {
      calls.push({ method: 'listReviews', params });
      if (calls.length === failAt) throw new Error('synthetic private diagnostic');
      if (params.page === 1) reviewIndex++;
      const reviews = reviewReads[reviewIndex];
      return clone(reviews?.response || {
        status: 200, data: reviews.slice((params.page - 1) * 100, params.page * 100),
      });
    },
  }, { get(target, property) {
    if (!(property in target)) {
      calls.push({ method: String(property) });
      throw new Error('Unexpected GitHub operation');
    }
    return target[property];
  } });
  return { github: { rest: { pulls } }, calls };
}

function run(fake, overrides = {}) {
  return runGuard({ github: fake.github, identity: IDENTITY, expectedHead: HEAD,
    expectedBase: BASE, policy: clone(POLICY), now: () => NOW, ...overrides });
}

const UNVERIFIED = { ok: false, problems: ['UNVERIFIED_API_OR_SNAPSHOT'] };

test('exact current head, base and metadata binding approves without mutating input', () => {
  const input = fixture();
  const before = clone(input);
  assert.deepEqual(evaluate(input), { ok: true, problems: [], head: HEAD, base: BASE,
    metadataDigest: metadataDigest(input.snapshot) });
  assert.deepEqual(input, before);
});

test('absent and unknown trust policies deny even a valid approval', () => {
  for (const policy of [undefined, null, {}, { mode: 'unapproved-model' }]) {
    denied({ ...fixture(), policy }, 'TRUST_POLICY_UNCONFIGURED');
  }
});

for (const [name, reviews] of [
  ['no review', []],
  ['old head', [approval(undefined, { commit_id: OTHER })]],
  ['dismissed', [approval(undefined, { state: 'DISMISSED' })]],
  ['unknown reviewer', [approval(undefined, { user: { id: 303 } })]],
  ['ordinary comment', [approval(undefined, { state: 'COMMENTED' })]],
  ['pending review', [approval(undefined, { state: 'PENDING' })]],
  ['missing binding', [approval(undefined, { body: 'LGTM' })]],
  ['binding embedded in another line', [approval(undefined, { body: `prefix ${approval().body}` })]],
]) {
  test(`${name} cannot approve`, () => denied({ ...fixture(), reviews }, 'CURRENT_TRUSTED_APPROVAL_REQUIRED'));
}

test('self approval denies even when author is allowlisted and another reviewer approves', () => {
  const input = fixture();
  input.policy.reviewerIds.push(101);
  input.reviews.push(approval(undefined, { id: 2, user: { id: 101 } }));
  denied(input, 'SELF_APPROVAL');
});

for (const state of ['CHANGES_REQUESTED', 'DISMISSED']) {
  test(`later ${state} overrides approval regardless of API ordering`, () => {
    const later = approval(undefined, { id: 2, state, submitted_at: new Date(NOW).toISOString() });
    for (const reviews of [[approval(), later], [later, approval()]]) {
      denied({ ...fixture(), reviews }, state === 'CHANGES_REQUESTED' ? state : 'CURRENT_TRUSTED_APPROVAL_REQUIRED');
    }
  });
}

test('later approval resolves the same reviewer rejection but not another reviewer rejection', () => {
  const rejection = approval(undefined, { id: 2, state: 'CHANGES_REQUESTED', submitted_at: new Date(NOW - 2000).toISOString() });
  assert.equal(evaluate({ ...fixture(), reviews: [approval(), rejection] }).ok, true);
  rejection.user.id = 303;
  denied({ ...fixture(), reviews: [approval(), rejection] }, 'CHANGES_REQUESTED');
});

test('equal timestamps use review ID and comments/pending do not erase decisions', () => {
  const rejection = approval(undefined, { id: 2, state: 'CHANGES_REQUESTED' });
  for (const reviews of [[approval(), rejection], [rejection, approval()]]) {
    denied({ ...fixture(), reviews }, 'CHANGES_REQUESTED');
  }
  for (const state of ['COMMENTED', 'PENDING']) {
    const comment = { id: 3, user: { id: 202 }, state };
    assert.equal(evaluate({ ...fixture(), reviews: [comment, approval()] }).ok, true);
    denied({ ...fixture(), reviews: [rejection, comment] }, 'CHANGES_REQUESTED');
  }
});

for (const [age, expected] of [[0, true], [59_999, true], [60_000, false], [60_001, false], [-1, false]]) {
  test(`approval age ${age} ms enforces TTL and future boundary`, () => {
    const input = fixture();
    input.reviews[0].submitted_at = new Date(NOW - age).toISOString();
    assert.equal(evaluate(input).ok, expected);
  });
}

for (const [name, labels] of [
  ['absent', []], ['removed', [{ name: 'status:needs-review' }]],
  ['role only', [{ name: 'role:test' }]],
  ['multiple', [{ name: 'agent:codex' }, { name: 'agent:gemini' }]],
]) {
  test(`implementation owner ${name} denies even freshly bound approval`, () => {
    const pr = pullRequest();
    pr.labels = labels;
    const snapshot = prSnapshot(pr, IDENTITY);
    denied({ ...fixture(), snapshot, reviews: [approval(snapshot)] }, 'IMPLEMENTATION_OWNER_REQUIRED');
  });
}

for (const [name, mutate, problem] of [
  ['draft', pr => { pr.draft = true; }, 'DRAFT_NOT_MERGE_READY'],
  ['verification absent', pr => { pr.body = 'Merge authority: integrator'; }, 'VERIFICATION_REQUIRED'],
  ['authority absent', pr => { pr.body = '```text\nPASS\n```'; }, 'MERGE_AUTHORITY_REQUIRED'],
]) {
  test(`${name} denies even freshly bound approval`, () => {
    const pr = pullRequest();
    mutate(pr);
    const snapshot = prSnapshot(pr, IDENTITY);
    denied({ ...fixture(), snapshot, reviews: [approval(snapshot)] }, problem);
  });
}

for (const [field, value] of [
  ['head', OTHER], ['base', OTHER], ['body', `${BODY}\nEdited`], ['title', 'Edited title'],
  ['labels', ['agent:codex', 'new-label']], ['repository', 'fixture/another'], ['number', 672],
  ['authorId', 303], ['headRef', 'agent/other'],
]) {
  test(`changed ${field} invalidates old metadata binding`, () => {
    const input = fixture();
    input.snapshot[field] = value;
    denied(input, 'CURRENT_TRUSTED_APPROVAL_REQUIRED');
  });
}

test('PR body cannot impersonate authenticated review binding', () => {
  const input = fixture();
  input.snapshot.body += `\n${input.reviews[0].body}`;
  input.reviews[0].body = 'APPROVED';
  denied(input, 'CURRENT_TRUSTED_APPROVAL_REQUIRED');
});

test('label order and repository case normalize; review binding accepts CRLF', () => {
  const pr = pullRequest();
  pr.labels.reverse();
  pr.base.repo.full_name = 'FIXTURE/ACCOUNT';
  const snapshot = prSnapshot(pr, { ...IDENTITY, repository: 'FIXTURE/ACCOUNT' });
  assert.deepEqual(snapshot, fixture().snapshot);
  const reviews = [approval(snapshot, { body: `Review\r\n${approval(snapshot).body}\r\nDone` })];
  assert.equal(evaluate({ ...fixture(), snapshot, reviews }).ok, true);
});

test('adapter makes only exact repository PR/review GET calls and repeats safely', async () => {
  for (let repeat = 0; repeat < 2; repeat++) {
    const fake = api();
    assert.equal((await run(fake)).ok, true);
    assert.deepEqual(fake.calls, [
      { method: 'get', params: { owner: 'fixture', repo: 'account', pull_number: 671 } },
      { method: 'listReviews', params: { owner: 'fixture', repo: 'account', pull_number: 671, per_page: 100, page: 1 } },
      { method: 'listReviews', params: { owner: 'fixture', repo: 'account', pull_number: 671, per_page: 100, page: 1 } },
      { method: 'get', params: { owner: 'fixture', repo: 'account', pull_number: 671 } },
    ]);
  }
});

for (const failAt of [1, 2, 3, 4]) {
  test(`API read ${failAt} throws: fail closed with no private error leakage or later calls`, async () => {
    const fake = api({ failAt });
    assert.deepEqual(await run(fake), UNVERIFIED);
    assert.equal(fake.calls.length, failAt);
  });
}

for (const [name, mutate] of [
  ['head', pr => { pr.head.sha = OTHER; }], ['base', pr => { pr.base.sha = OTHER; }],
  ['body', pr => { pr.body += '\nChanged'; }], ['labels', pr => { pr.labels = []; }],
  ['draft', pr => { pr.draft = true; }], ['title', pr => { pr.title = 'Changed'; }],
]) {
  test(`${name} changes during API reads deny`, async () => {
    const changed = pullRequest();
    mutate(changed);
    assert.deepEqual(await run(api({ prs: [pullRequest(), changed] })), UNVERIFIED);
  });
}

test('review dismissal/change during reread denies', async () => {
  for (const reviews of [[], [approval(undefined, { state: 'DISMISSED' })], [approval(undefined, { body: 'changed' })]]) {
    assert.deepEqual(await run(api({ reviewReads: [[approval()], reviews] })), UNVERIFIED);
  }
});

for (const overrides of [
  { expectedHead: OTHER }, { expectedBase: OTHER }, { expectedHead: '' },
  { identity: { ...IDENTITY, number: 0 } }, { identity: { ...IDENTITY, repository: '../account' } },
  { identity: { ...IDENTITY, repository: 'other/account' } }, { identity: { ...IDENTITY, number: 672 } },
]) {
  test(`stale or invalid event ${JSON.stringify(overrides)} denies`, async () => {
    assert.deepEqual(await run(api(), overrides), UNVERIFIED);
  });
}

for (const [name, mutate] of [
  ['missing PR', () => undefined], ['closed', pr => ({ ...pr, state: 'closed' })],
  ['missing draft', pr => ({ ...pr, draft: undefined })], ['malformed head', pr => ({ ...pr, head: { sha: 'invalid' } })],
  ['invalid author', pr => ({ ...pr, user: { id: 0 } })], ['invalid labels', pr => ({ ...pr, labels: [{}] })],
  ['wrong base ref', pr => ({ ...pr, base: { ...pr.base, ref: 'develop' } })],
  ['non-string body', pr => ({ ...pr, body: {} })], ['non-string title', pr => ({ ...pr, title: null })],
  ['non-200 PR', () => ({ response: { status: 403, data: pullRequest() } })],
]) {
  test(`invalid PR snapshot: ${name}`, async () => {
    assert.deepEqual(await run(api({ prs: [mutate(pullRequest())] })), UNVERIFIED);
  });
}

for (const [name, reviews] of [
  ['duplicate review IDs', [approval(), approval()]], ['null review', [null]],
  ['unknown state', [approval(undefined, { state: 'UNKNOWN' })]],
  ['invalid review ID', [approval(undefined, { id: 0 })]], ['invalid reviewer ID', [approval(undefined, { user: { id: '202' } })]],
  ['invalid timestamp', [approval(undefined, { submitted_at: 'invalid' })]],
  ['invalid decision commit', [approval(undefined, { commit_id: 'invalid' })]],
  ['non-200 reviews', { response: { status: 500, data: [] } }],
  ['non-array reviews', { response: { status: 200, data: {} } }],
  ['oversized page', { response: { status: 200, data: Array(101).fill(approval()) } }],
]) {
  test(`invalid review response: ${name}`, async () => {
    assert.deepEqual(await run(api({ reviewReads: [reviews, reviews] })), UNVERIFIED);
  });
}

function paginatedReviews() {
  return [approval(), ...Array.from({ length: 100 }, (_, index) => ({
    id: index + 2, user: { id: 303 }, state: 'COMMENTED',
  }))];
}

test('more than 100 reviews reads every page twice, including later rejection', async () => {
  const reviews = paginatedReviews();
  let fake = api({ reviewReads: [reviews, reviews] });
  assert.equal((await run(fake)).ok, true);
  assert.deepEqual(fake.calls.filter(call => call.method === 'listReviews').map(call => call.params.page), [1, 2, 1, 2]);
  reviews.push(approval(undefined, { id: 102, state: 'CHANGES_REQUESTED', submitted_at: new Date(NOW).toISOString() }));
  fake = api({ reviewReads: [reviews, reviews] });
  const result = await run(fake);
  assert.equal(result.ok, false);
  assert.ok(result.problems.includes('CHANGES_REQUESTED'));
});

test('page two errors in either pass cannot hide later reviews', async () => {
  const reviews = paginatedReviews();
  for (const failAt of [3, 5]) {
    const fake = api({ reviewReads: [reviews, reviews], failAt });
    assert.deepEqual(await run(fake), UNVERIFIED);
    assert.equal(fake.calls.length, failAt);
  }
});

test('duplicate review crossing page boundary denies', async () => {
  const reviews = paginatedReviews();
  reviews[100] = approval();
  assert.deepEqual(await run(api({ reviewReads: [reviews, reviews] })), UNVERIFIED);
});

test('repeated full pages terminate at the pagination limit', async () => {
  const reviews = { response: { status: 200, data: paginatedReviews().slice(0, 100) } };
  const fake = api({ reviewReads: [reviews] });
  assert.deepEqual(await run(fake), UNVERIFIED);
  assert.equal(fake.calls.filter(call => call.method === 'listReviews').length, 100);
});

for (const policy of [
  { ...POLICY, reviewerIds: [] }, { ...POLICY, reviewerIds: ['202'] },
  { ...POLICY, maxAgeMs: 0 }, { ...POLICY, maxAgeMs: 86_400_001 },
]) {
  test(`invalid trust policy ${JSON.stringify(policy)} denies in adapter`, async () => {
    assert.deepEqual(await run(api(), { policy }), UNVERIFIED);
  });
}

test('invalid or failing clock denies in adapter', async () => {
  for (const now of [() => NaN, () => { throw new Error('clock failure'); }]) {
    assert.deepEqual(await run(api(), { now }), UNVERIFIED);
  }
});

test('exactly 100 reviews require an empty terminal page in both passes', async () => {
  const reviews = paginatedReviews().slice(0, 100);
  const fake = api({ reviewReads: [reviews, reviews] });
  assert.equal((await run(fake)).ok, true);
  assert.deepEqual(fake.calls.filter(call => call.method === 'listReviews').map(call => call.params.page), [1, 2, 1, 2]);
});

test('adapter with no configured policy denies and performs no writes', async () => {
  const fake = api();
  const result = await run(fake, { policy: undefined });
  assert.equal(result.ok, false);
  assert.deepEqual(result.problems, ['TRUST_POLICY_UNCONFIGURED']);
  assert.deepEqual(fake.calls.map(call => call.method), ['get', 'listReviews', 'listReviews', 'get']);
});

test('empty review history is a verified denial, not an API success bypass', async () => {
  const fake = api({ reviewReads: [[], []] });
  const result = await run(fake);
  assert.equal(result.ok, false);
  assert.deepEqual(result.problems, ['CURRENT_TRUSTED_APPROVAL_REQUIRED']);
  assert.equal(fake.calls.length, 4);
});
