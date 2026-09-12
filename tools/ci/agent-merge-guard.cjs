'use strict';

// Metadata only. See docs/ai-harness/87-spec-driven-delegation.md section 7.
const { createHash } = require('node:crypto');
const SHA = /^[0-9a-f]{40}$/;
const OWNER_LABELS = new Set(['agent:codex', 'agent:gemini', 'agent:claude-code']);
const MAX_PAGES = 100;

function requireValue(condition, code) {
  if (!condition) throw new Error(code);
}

function prSnapshot(pr, identity) {
  requireValue(pr && pr.number === identity.number && pr.state === 'open' &&
    typeof pr.draft === 'boolean' && typeof pr.title === 'string' &&
    (pr.body === null || typeof pr.body === 'string') &&
    Number.isSafeInteger(pr.user?.id) && pr.user.id > 0 &&
    SHA.test(pr.head?.sha) && SHA.test(pr.base?.sha) &&
    typeof pr.head?.ref === 'string' && pr.base?.ref === 'main' &&
    pr.base?.repo?.full_name?.toLowerCase() === identity.repository.toLowerCase() &&
    Array.isArray(pr.labels) && pr.labels.every(label => typeof label.name === 'string'),
  'INVALID_PR_SNAPSHOT');
  return {
    number: pr.number, repository: identity.repository.toLowerCase(),
    authorId: pr.user.id, head: pr.head.sha, base: pr.base.sha,
    headRef: pr.head.ref, baseRef: pr.base.ref, draft: pr.draft,
    title: pr.title, body: pr.body || '', labels: pr.labels.map(label => label.name).sort(),
  };
}

function metadataDigest(snapshot) {
  return createHash('sha256').update(JSON.stringify(snapshot)).digest('hex');
}

function latestReviews(reviews) {
  requireValue(Array.isArray(reviews), 'INVALID_REVIEWS');
  const latest = new Map();
  const ids = new Set();
  for (const review of reviews) {
    requireValue(Number.isSafeInteger(review?.id) && review.id > 0 && !ids.has(review.id) &&
      Number.isSafeInteger(review.user?.id) && review.user.id > 0 &&
      ['APPROVED', 'CHANGES_REQUESTED', 'DISMISSED', 'COMMENTED', 'PENDING'].includes(review.state),
    'INVALID_REVIEW');
    ids.add(review.id);
    // Draft reviews and ordinary comments neither approve nor erase a decision.
    if (['PENDING', 'COMMENTED'].includes(review.state)) continue;
    const submitted = Date.parse(review.submitted_at);
    requireValue(Number.isFinite(submitted) && SHA.test(review.commit_id), 'INVALID_REVIEW_DECISION');
    const previous = latest.get(review.user.id);
    if (!previous || submitted > previous.submitted ||
        (submitted === previous.submitted && review.id > previous.id)) {
      latest.set(review.user.id, { ...review, submitted });
    }
  }
  return [...latest.values()];
}

// A transport-independent evaluator. No token, clock, filesystem, or API access.
// A trust policy must be explicitly supplied by trusted code; absence denies.
function evaluate({ snapshot, reviews, policy, now }) {
  const problems = [];
  const owners = snapshot.labels.filter(label => OWNER_LABELS.has(label));
  if (owners.length !== 1) problems.push('IMPLEMENTATION_OWNER_REQUIRED');
  if (!/```[\s\S]+```/.test(snapshot.body)) problems.push('VERIFICATION_REQUIRED');
  if (!/^Merge authority:\s*\S.+$/im.test(snapshot.body)) problems.push('MERGE_AUTHORITY_REQUIRED');
  if (snapshot.draft) problems.push('DRAFT_NOT_MERGE_READY');
  const decisions = latestReviews(reviews);
  if (decisions.some(review => review.state === 'CHANGES_REQUESTED')) problems.push('CHANGES_REQUESTED');
  if (decisions.some(review => review.state === 'APPROVED' && review.user.id === snapshot.authorId)) {
    problems.push('SELF_APPROVAL');
  }
  // Until the repository owner approves a trust model, even an APPROVED record
  // is only metadata. Never upgrade same-account comments to independent proof.
  if (!policy || policy.mode !== 'github-review-v1') {
    problems.push('TRUST_POLICY_UNCONFIGURED');
  } else {
    requireValue(Array.isArray(policy.reviewerIds) && policy.reviewerIds.length > 0 &&
      policy.reviewerIds.every(id => Number.isSafeInteger(id) && id > 0) &&
      Number.isSafeInteger(policy.maxAgeMs) && policy.maxAgeMs > 0 &&
      policy.maxAgeMs <= 24 * 60 * 60 * 1000 && Number.isFinite(now), 'INVALID_TRUST_POLICY');
    const digest = metadataDigest(snapshot);
    const approved = decisions.some(review => {
      if (review.state !== 'APPROVED' || review.user.id === snapshot.authorId ||
          !policy.reviewerIds.includes(review.user.id) || review.commit_id !== snapshot.head ||
          review.submitted > now || now - review.submitted >= policy.maxAgeMs) return false;
      // The review's own authenticated body binds base and mutable PR metadata;
      // a matching string in the PR body or an issue comment cannot substitute.
      const binding = `Agent-Merge-Guard: v1 ${snapshot.repository}#${snapshot.number} ${snapshot.head} ${snapshot.base} ${digest}`;
      return typeof review.body === 'string' && review.body.split(/\r?\n/).includes(binding);
    });
    if (!approved) problems.push('CURRENT_TRUSTED_APPROVAL_REQUIRED');
  }
  return { ok: problems.length === 0, problems, head: snapshot.head, base: snapshot.base,
    metadataDigest: metadataDigest(snapshot) };
}

async function listReviews(github, params) {
  const reviews = [];
  // Explicit pagination fails on truncated/error responses and has a bound so
  // a repeated full page cannot hang the runner or hide a later rejection.
  for (let page = 1; page <= MAX_PAGES; page++) {
    const response = await github.rest.pulls.listReviews({ ...params, per_page: 100, page });
    requireValue(response?.status === 200 && Array.isArray(response.data) && response.data.length <= 100,
      'INVALID_REVIEWS_RESPONSE');
    reviews.push(...response.data);
    if (response.data.length < 100) return reviews;
  }
  throw new Error('REVIEW_PAGE_LIMIT');
}

async function runGuard({ github, identity, expectedHead, expectedBase, policy, now = Date.now }) {
  try {
    requireValue(identity && /^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(identity.repository) &&
      Number.isSafeInteger(identity.number) && identity.number > 0 &&
      SHA.test(expectedHead) && SHA.test(expectedBase), 'INVALID_EVENT_IDENTITY');
    const [owner, repo] = identity.repository.split('/');
    const params = { owner, repo, pull_number: identity.number };
    const readPR = async () => {
      const response = await github.rest.pulls.get(params);
      requireValue(response?.status === 200, 'INVALID_PR_RESPONSE');
      const snapshot = prSnapshot(response.data, identity);
      requireValue(snapshot.head === expectedHead && snapshot.base === expectedBase, 'STALE_EVENT_OR_CHECKOUT');
      return snapshot;
    };
    const before = await readPR();
    const reviews = await listReviews(github, params);
    // Read twice to detect edits/dismissals during pagination as well as head,
    // base, labels and body races. GitHub does not provide a transactional API.
    const repeatedReviews = await listReviews(github, params);
    requireValue(JSON.stringify(reviews) === JSON.stringify(repeatedReviews), 'REVIEWS_CHANGED_DURING_RUN');
    const after = await readPR();
    requireValue(metadataDigest(before) === metadataDigest(after), 'PR_CHANGED_DURING_RUN');
    return evaluate({ snapshot: after, reviews, policy, now: now() });
  } catch {
    // API exceptions may include response bodies or request details. Emit only
    // a fixed diagnostic; inability to verify must never become a green skip.
    return { ok: false, problems: ['UNVERIFIED_API_OR_SNAPSHOT'] };
  }
}

module.exports = { prSnapshot, metadataDigest, latestReviews, evaluate, listReviews, runGuard };
