import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..', '..');

const fineTuningDir = path.join(
  projectRoot,
  'docs',
  'ai',
  'fine-tuning'
);
const casesFilePattern = /cases.*\.json$/;
const outputDir = path.join(projectRoot, 'docs', 'ai', 'fine-tuning', 'generated');

const systemPrompt = [
  '당신은 MES/MCS 운영 분석 AI입니다.',
  'MES 작업오더, MCS 자재 이동오더, LOT, Location, 경로, PLC 이벤트 근거만 사용해서 답변합니다.',
  '근거 데이터에 없는 내용을 꾸며내지 않습니다.',
  '응답은 반드시 JSON object 한 개로만 작성합니다.',
  '필드는 summary, facts, inference, impact, recommendedActions 순서로 작성합니다.'
].join(' ');

function toTrainingExample(testCase) {
  const userContent = [
    '다음 MES/MCS 운영 컨텍스트를 분석하세요.',
    '',
    `시나리오 유형: ${testCase.scenarioType}`,
    '',
    JSON.stringify(testCase.evidence, null, 2)
  ].join('\n');

  const assistantContent = JSON.stringify(testCase.analysis);

  return {
    messages: [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userContent },
      { role: 'assistant', content: assistantContent }
    ]
  };
}

function toJsonl(examples) {
  return examples.map((example) => JSON.stringify(example)).join('\n') + '\n';
}

const caseFiles = (await readdir(fineTuningDir, { withFileTypes: true }))
  .filter((entry) => entry.isFile() && casesFilePattern.test(entry.name))
  .map((entry) => path.join(fineTuningDir, entry.name))
  .sort();

if (caseFiles.length === 0) {
  throw new Error('At least one *-cases.json file is required.');
}

const cases = [];

for (const caseFile of caseFiles) {
  const raw = await readFile(caseFile, 'utf8');
  const parsed = JSON.parse(raw);

  if (!Array.isArray(parsed)) {
    throw new Error(`${path.relative(projectRoot, caseFile)} must be a JSON array.`);
  }

  cases.push(...parsed);
}

const duplicateIds = cases
  .map((item) => item.id)
  .filter((id, index, ids) => ids.indexOf(id) !== index);

if (duplicateIds.length > 0) {
  throw new Error(`Duplicate case ids: ${[...new Set(duplicateIds)].join(', ')}`);
}

const trainCases = cases.filter((item) => item.split === 'train');
const validationCases = cases.filter((item) => item.split === 'validation');

if (trainCases.length === 0) {
  throw new Error('At least one train case is required.');
}

if (validationCases.length === 0) {
  throw new Error('At least one validation case is required.');
}

await mkdir(outputDir, { recursive: true });

const trainOutputPath = path.join(outputDir, 'work-order-analysis-train.jsonl');
const validationOutputPath = path.join(outputDir, 'work-order-analysis-validation.jsonl');

await writeFile(trainOutputPath, toJsonl(trainCases.map(toTrainingExample)), 'utf8');
await writeFile(validationOutputPath, toJsonl(validationCases.map(toTrainingExample)), 'utf8');

console.log(`Loaded ${caseFiles.length} case files (${cases.length} cases)`);
console.log(`Created ${path.relative(projectRoot, trainOutputPath)} (${trainCases.length} cases)`);
console.log(`Created ${path.relative(projectRoot, validationOutputPath)} (${validationCases.length} cases)`);
