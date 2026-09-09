const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const test = require('node:test');

// Local documentation regression checks, not an authorization engine or YAML parser.
// Read exactly these four policy inputs; fixtures never write files or call Git/GitHub.
const paths = [
  'docs/ai-harness/20-workflow.md',
  'docs/ai-harness/87-spec-driven-delegation.md',
  'docs/ai-harness/88-pr-review-and-merge-runbook.md',
  '.github/ISSUE_TEMPLATE/agent-implementation-spec.yml',
];
const docs = paths.map(path => readFileSync(resolve(__dirname, '../..', path), 'utf8'));

const required = [
  [/부모 Integrator만/, /동결 Draft 사전리뷰/, /부모 Ready/, /최신 head\/base/, /사람 리뷰.*기본/, /명시.*승인/, /cleanup approval/],
  [/PR 본문 초안/, /Reviewer.*읽기 전용/, /부모 Integrator만/, /티어.*권한.*아니/, /동결 Draft 사전리뷰/, /최신 head\/base/],
  [/부모 Integrator만/, /읽기 전용/, /명시.*동결 Draft/, /작업 중 Draft.*자동.*금지/,
    /사전리뷰[\s\S]*부모 Ready[\s\S]*최종 독립 검토[\s\S]*별도 승인.*merge commit/,
    /최신 head\/base.*CI/, /head\/base.*변경.*재검토/, /CI.*미실행.*보류/,
    /충돌.*보류/, /티어.*권한.*아니/, /전용 독립 리뷰 자동화.*부모/, /등록된 승인 범위/,
    /사람 리뷰.*기본/, /--merge --match-head-commit/, /cleanup.*별도 승인/, /자동 rebase.*금지/],
  [/구현 담당.*PR 본문 초안/, /Reviewer.*읽기 전용/, /부모 Integrator만/, /티어.*권한.*아니/,
    /merge.*Issue close.*cleanup.*각각.*승인/, /id: sizing/, /id: safety/, /id: ready_checklist/],
];

// Known pre-fix grants and deadlock states. Deny checks also catch a dangerous
// clause added alongside otherwise valid policy, rather than presence-only tests.
const forbidden = [
  /지시된 파일만 수정, 테스트 실행, Draft PR 생성/,
  /구현 담당은 PR 생성까지만 한다/,
  /구현 담당은 PR을 열기만 하고/,
  /\[Implementer\][^\n]*→\s*Draft PR/,
  /\[Implementer\][^\n]*ready for review/,
  /Draft가 아니다\(구현 담당이/,
  /Draft 상태 PR — 아직 작업 중이다/,
  /리뷰어 티어가 병합 권한을 결정한다/,
  /Reasoning 티어[^\n]*라면[^\n]*병합할 수 있다/,
  /Reviewer \/ Integrator[^\n]*병합, Issue 상태/,
  /Reviewer 승인 후 Integrator\(상위 모델 또는 사람\)만 수행/,
  /반려하고 rebase를 요청한다/,
  /게이트 중 하나라도 실패하면 반려한다/,
  /gh pr merge[^\n]*(?:--squash|--delete-branch)/,
  /최신 head\/base 및 Ready 이벤트의 필수 checks 통과/,
  /Draft 사전리뷰는 Ready-only 검사 통과 후/,
];

const stageRequirements = [
  /Draft 단계 필수 검사.*통과/,
  /Ready-only 검사.*미도래\/pending.*PASS.*아니.*사전리뷰.*막지 않/,
  /Draft 단계 필수 검사.*실패.*보류.*반려/,
  /최종 독립 검토.*Ready-only.*모든 필수 CI.*통과/,
];

function violations(index, text) {
  return [
    ...required[index].filter(pattern => !pattern.test(text)).map(pattern => `missing ${pattern}`),
    ...forbidden.filter(pattern => pattern.test(text)).map(pattern => `unsafe ${pattern}`),
    ...(index === 2 ? phaseViolations(text) : []),
    ...(index < 3 ? stageRequirements.filter(pattern => !pattern.test(text))
      .map(pattern => `missing stage rule ${pattern}`) : []),
  ];
}

// Inspect the phase table itself: unrelated correct prose cannot satisfy an
// out-of-order lifecycle or transfer Ready/merge ownership to the Reviewer.
function phaseViolations(text) {
  const flow = text.match(/## 4\. 흐름\r?\n([\s\S]*?)\r?\n## 5\./)?.[1] ?? '';
  const phases = [...flow.matchAll(/^\| ([^|]+) \| ([^|]+) \| ([^|]+) \|/gm)]
    .map(match => [match[1].trim(), match[2].trim(), match[3].trim()])
    .filter(([phase]) => !['단계', '---'].includes(phase));
  const expected = [
    ['구현·검증', 'Implementer'],
    ['Draft 생성·동결', '부모 Integrator'],
    ['동결 Draft 사전리뷰', '독립 Reviewer'],
    ['부모 Ready', '부모 Integrator'],
    ['최종 독립 검토', '비작성자 Reviewer'],
    ['별도 승인 merge commit', '부모 Integrator'],
    ['Issue close / cleanup', '부모 Integrator'],
  ];
  if (JSON.stringify(phases.map(row => row.slice(0, 2))) !== JSON.stringify(expected)) {
    return ['invalid phase order/owner'];
  }
  const conditions = [
    /allowlist diff.*검증 결과.*PR 본문 초안.*부모.*반환/,
    /승인된 commit\/push\/PR 생성.*Refs #.*head\/base 기록/,
    /부모 명시 요청.*동결 diff.*Draft 단계 필수 검사 통과.*Ready-only.*미도래\/pending.*사람 리뷰 기본/,
    /Draft 단계 필수 검사 통과.*사전리뷰 완료.*Ready 승인 확인/,
    /최신 head\/base.*Ready-only 포함 모든 필수 CI 통과.*수용 기준.*변경 시 재검토/,
    /최종 증거.*merge 승인.*병합 직전 상태 재확인/,
    /병합 결과.*종료 조건.*각각의 승인.*보존\/소유 확인/,
  ];
  return phases.every((row, index) => conditions[index].test(row[2]) &&
    !(index < 4 && /Ready-only.*통과/.test(row[2]))) ? [] : ['invalid phase condition'];
}

for (const [index, path] of paths.entries()) {
  test(`current contract: ${path}`, () => assert.deepEqual(violations(index, docs[index]), []));
}

const unsafeFixtures = [
  [1, '| Implementer | 지시된 파일만 수정, 테스트 실행, Draft PR 생성 |'],
  [1, '**구현 담당은 PR 생성까지만 한다.**'],
  [1, '[Implementer] 지시 실행 → 검증 → Draft PR (Refs #N)'],
  [1, '| Reviewer / Integrator | 독립 검토 후 반려 또는 승인, 병합, Issue 상태·라벨 갱신 |'],
  [2, '[Implementer] Draft PR → ready for review'],
  [2, '- Draft가 아니다(구현 담당이 `ready for review`로 올렸다).'],
  [2, '- Draft 상태 PR — 아직 작업 중이다.'],
  [2, '**리뷰어 티어가 병합 권한을 결정한다.**'],
  [2, 'Reasoning 티어(Codex, Claude Code)라면 아래 확인을 마친 뒤 **병합할 수 있다.**'],
  [2, 'gh pr merge <번호> --repo skyg547/account --squash --delete-branch'],
  [2, 'conflict가 있는 PR은 반려하고 rebase를 요청한다.'],
  [2, '게이트 중 하나라도 실패하면 반려한다.'],
  [2, '| CI | 최신 head/base 및 Ready 이벤트의 필수 checks 통과 | 실패 시 보류 |'],
  [2, 'Draft 사전리뷰는 Ready-only 검사 통과 후 진행한다.'],
  [3, 'description: 구현 담당은 PR 생성까지만 한다.'],
  [3, '- label: 구현 담당은 PR을 열기만 하고, `main` 병합을 시도하지 않는다.'],
  [3, '- label: 병합은 Reviewer 승인 후 Integrator(상위 모델 또는 사람)만 수행한다.'],
];
for (const [index, clause] of unsafeFixtures) {
  test(`reject restored pre-fix clause: ${clause}`, () => {
    const findings = violations(index, `${docs[index]}\n${clause}\n`);
    assert.ok(findings.some(finding => finding.startsWith('unsafe ')));
  });
}

test('reject Ready before frozen Draft pre-review', () => {
  const lines = docs[2].split('\n');
  const pre = lines.findIndex(line => line.startsWith('| 동결 Draft 사전리뷰 |'));
  const ready = lines.findIndex(line => line.startsWith('| 부모 Ready |'));
  assert.ok(pre >= 0 && ready > pre, 'fixture requires actual ordered rows');
  [lines[pre], lines[ready]] = [lines[ready], lines[pre]];
  assert.deepEqual(phaseViolations(lines.join('\n')), ['invalid phase order/owner']);
});

test('reject Reviewer owning Ready despite correct surrounding prose', () => {
  const original = '| 부모 Ready | 부모 Integrator |';
  assert.ok(docs[2].includes(original));
  assert.deepEqual(phaseViolations(docs[2].replace(original, '| 부모 Ready | 독립 Reviewer |')),
    ['invalid phase order/owner']);
});

for (const [phase, replacement] of [
  ['동결 Draft 사전리뷰', '부모 명시 요청, Ready-only 검사 통과 후 사전리뷰'],
  ['부모 Ready', 'Ready-only 포함 모든 필수 CI 통과 후 Ready 승인 확인'],
  ['부모 Ready', 'Draft 단계 필수 검사 생략, 사전리뷰 완료 및 Ready 승인 확인'],
  ['최종 독립 검토', '최신 head/base만 확인, Ready-only 검사는 생략'],
]) {
  test(`reject changed next-phase condition: ${phase}: ${replacement}`, () => {
    const row = docs[2].split('\n').find(line => line.startsWith(`| ${phase} |`));
    assert.ok(row, 'fixture must modify an actual phase row');
    const columns = row.split('|');
    columns[3] = ` ${replacement} `;
    assert.deepEqual(phaseViolations(docs[2].replace(row, columns.join('|'))), ['invalid phase condition']);
  });
}

for (const [index, clause] of [
  [0, '동결 Draft 사전리뷰'],
  [2, '부모 Ready'],
  [2, '최종 독립 검토'],
  [2, '등록된 승인 범위'],
  [2, '--merge --match-head-commit'],
]) {
  test(`reject missing lifecycle gate: ${clause}`, () => {
    assert.ok(docs[index].includes(clause), 'fixture must modify an actual policy clause');
    const mutated = docs[index].replaceAll(clause, 'REMOVED_GATE');
    assert.ok(violations(index, mutated).some(finding => finding.startsWith('missing ')));
  });
}
