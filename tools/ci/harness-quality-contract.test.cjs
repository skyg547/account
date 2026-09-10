const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve, posix } = require('node:path');
const test = require('node:test');

const qualityPath = 'docs/ai-harness/42-code-documentation-quality.md';
const entryPoints = [
  ['AGENTS.md', 'Code Review Rules'],
  ['docs/ai-harness/10-rules.md', 'Code Quality Rules'],
  ['docs/ai-harness/40-test-checklist.md', 'Quality Completion Gate'],
];

// 입력은 승인된 네 문서뿐이다. 경로가 잘못된 링크를 따라 파일을 읽지 않는다.
// 새 계약 문서가 아직 없는 RED 단계도 명확한 assertion으로 보여준다.
const documents = Object.fromEntries([...entryPoints.map(([path]) => path), qualityPath].map(path => {
  try {
    return [path, readFileSync(resolve(__dirname, '../..', path), 'utf8')];
  } catch (error) {
    if (path === qualityPath && error.code === 'ENOENT') return [path, ''];
    throw error;
  }
}));

function sections(markdown) {
  return [...markdown.matchAll(/^## ([^\r\n]+)\r?\n([\s\S]*?)(?=^## |$(?![\s\S]))/gm)]
    .map(match => ({ title: match[1], body: match[2], whole: match[0] }));
}

function entryErrors(path, heading, markdown) {
  const body = sections(markdown).find(section => section.title === heading)?.body ?? '';
  const links = [...body.matchAll(/\[[^\]]+\]\(([^)]+)\)/g)].map(match => match[1]);
  const linked = links.some(target => posix.normalize(posix.join(posix.dirname(path), target)) === qualityPath);
  return [
    ...(!linked ? ['missing canonical quality link'] : []),
    ...(!/필수/.test(body) ? ['missing mandatory gate'] : []),
    ...(!/독립 리뷰/.test(body) ? ['missing independent review'] : []),
  ];
}

// 제목과 해당 절의 요구사항을 함께 검사한다. 다른 절에 같은 단어가 남아도
// 핵심 품질 항목이 제거된 것을 숨길 수 없다. 표현 자체의 품질 판단은 리뷰어 몫이다.
const requirements = [
  ['Q1.', [/명확한 이름/, /작은 함수/, /단일 책임/, /중복/, /불필요한 추상화/, /헥사고날/, /성능/, /금융 정합성/]],
  ['Q2.', [/입력.*처리.*출력/, /설계 이유/, /예외/, /경계/, /재실행/, /주석.*또는.*기능 문서/, /중복.*강요하지 않/]],
  ['Q3.', [/검증된 코드.*명령/, /모듈.*문서/, /전제/, /실행 예/, /기대 결과/, /제약/, /WORKLOG.*대체하지 않/]],
  ['Q4.', [/비자명.*로직.*가까이/, /의도.*설계 이유/, /유익한 주석.*보존/, /오래된 주석.*수정/, /매줄.*강요하지 않/, /문서 전용.*N\/A/]],
  ['판정과 증거', [/PASS/, /FAIL/, /N\/A/, /구체.*사유/, /독립 리뷰/, /미실행.*PASS.*아니/, /FAIL.*근거.*완료.*않/, /원래 작성자/]],
  ['적용 범위', [/승인된 변경/, /전수 리팩터링.*금지/, /일괄 주석.*금지/, /범위 밖.*금지/, /Ready.*merge.*변경하지 않/]],
  ['로컬 검사와 한계', [/node --test tools\/ci\/harness-quality-contract\.test\.cjs/, /CI.*미연결/, /#672/, /의미.*검증.*대체하지 않/]],
];

function qualityErrors(markdown) {
  const parts = sections(markdown);
  const errors = requirements.flatMap(([prefix, rules]) => {
    const matches = parts.filter(section => section.title.startsWith(prefix));
    if (matches.length !== 1) return [`missing/duplicate section: ${prefix}`];
    return rules.filter(rule => !rule.test(matches[0].body)).map(rule => `${prefix} missing ${rule}`);
  });
  const template = parts.find(section => section.title === '완료표 템플릿')?.body ?? '';
  const rows = template.split(/\r?\n/).filter(line => line.startsWith('|')).map(line =>
    line.split('|').slice(1, -1).map(cell => cell.trim()));
  const header = ['항목', '판정 (PASS/FAIL/N/A)', '파일·테스트 근거', 'N/A 사유', '위험·다음 검증 게이트', '독립 리뷰 확인'];
  if (JSON.stringify(rows[0]) !== JSON.stringify(header)) errors.push('missing evidence columns');
  for (const id of ['Q1', 'Q2', 'Q3', 'Q4']) {
    const matching = rows.filter(row => row[0] === id);
    if (matching.length !== 1 || matching[0].length !== header.length || matching[0].some(cell => !cell)) {
      errors.push(`missing evidence row: ${id}`);
    }
  }
  return errors;
}

for (const [path, heading] of entryPoints) {
  test(`entry contract: ${path}`, () => assert.deepEqual(entryErrors(path, heading, documents[path]), []));
  for (const target of ['', '42-missing-quality.md', '40-test-checklist.md', 'https://example.invalid/42-code-documentation-quality.md']) {
    test(`reject removed/wrong link: ${path}: ${target || 'removed'}`, () => {
      const mutated = documents[path].replace(/\[([^\]]+)\]\(([^)]*42-code-documentation-quality\.md)\)/g,
        (_, label) => target ? `[${label}](${target})` : label);
      assert.notEqual(mutated, documents[path], 'fixture must modify a real link');
      assert.ok(entryErrors(path, heading, mutated).includes('missing canonical quality link'));
    });
  }
  test(`reject optional instead of mandatory gate: ${path}`, () => {
    const mutated = documents[path].replaceAll('필수', '선택');
    assert.notEqual(mutated, documents[path]);
    assert.ok(entryErrors(path, heading, mutated).includes('missing mandatory gate'));
  });
}

for (const column of ['파일·테스트 근거', '독립 리뷰 확인']) {
  test(`reject removed evidence column: ${column}`, () => {
    assert.ok(documents[qualityPath].includes(`| ${column} |`));
    const mutated = documents[qualityPath].replace(`| ${column} |`, '|');
    assert.ok(qualityErrors(mutated).includes('missing evidence columns'));
  });
}

test('quality sections and evidence template', () => assert.deepEqual(qualityErrors(documents[qualityPath]), []));
for (const [prefix] of requirements) {
  test(`reject removed required section: ${prefix}`, () => {
    const original = sections(documents[qualityPath]).find(section => section.title.startsWith(prefix));
    assert.ok(original, 'fixture must remove an actual section');
    assert.ok(qualityErrors(documents[qualityPath].replace(original.whole, '')).includes(`missing/duplicate section: ${prefix}`));
  });
}
for (const id of ['Q1', 'Q2', 'Q3', 'Q4']) {
  test(`reject missing evidence row: ${id}`, () => {
    const row = documents[qualityPath].split(/\r?\n/).find(line => line.startsWith(`| ${id} |`));
    assert.ok(row, 'fixture must remove an actual completion row');
    assert.ok(qualityErrors(documents[qualityPath].replace(row, '')).includes(`missing evidence row: ${id}`));
  });
}

// 인메모리 negative fixtures는 파일을 바꾸지 않는다. 이 검사는 CI 미연결(#672)이며
// 문서 구조 회귀만 잡는다. 코드 품질이나 설명의 사실성에 대한 의미 검증을 대체하지 않는다.
