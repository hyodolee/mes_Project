import { readFile } from 'node:fs/promises';
import path from 'node:path';
import {
  countJsonlLines,
  ensureFileExists,
  fileIdsPath,
  loadDotEnvIfPresent,
  parseArgs,
  relative,
  requireOpenAiApiKey,
  trainJsonlPath,
  validationJsonlPath,
  writeJson
} from './openai-finetune-utils.mjs';

const args = parseArgs(process.argv.slice(2));

if (args.help) {
  console.log(`
Usage:
  node scripts\\ai\\upload-finetune-files.mjs [--dry-run]

Uploads:
  docs\\ai\\fine-tuning\\generated\\work-order-analysis-train.jsonl
  docs\\ai\\fine-tuning\\generated\\work-order-analysis-validation.jsonl

Required when not using --dry-run:
  OPENAI_API_KEY
`);
  process.exit(0);
}

await loadDotEnvIfPresent();

await ensureFileExists(trainJsonlPath);
await ensureFileExists(validationJsonlPath);

const trainLineCount = await countJsonlLines(trainJsonlPath);
const validationLineCount = await countJsonlLines(validationJsonlPath);

console.log(`Train file: ${relative(trainJsonlPath)} (${trainLineCount} examples)`);
console.log(`Validation file: ${relative(validationJsonlPath)} (${validationLineCount} examples)`);

if (args['dry-run']) {
  console.log('Dry run only. No files were uploaded.');
  process.exit(0);
}

const apiKey = requireOpenAiApiKey();

async function uploadFineTuneFile(filePath) {
  const bytes = await readFile(filePath);
  const formData = new FormData();

  formData.append('purpose', 'fine-tune');
  formData.append('file', new Blob([bytes]), path.basename(filePath));

  const response = await fetch('https://api.openai.com/v1/files', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`
    },
    body: formData
  });

  const text = await response.text();
  const parsed = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(
      [
        `OpenAI file upload failed: HTTP ${response.status}`,
        JSON.stringify(parsed, null, 2)
      ].join('\n')
    );
  }

  return parsed;
}

const trainFile = await uploadFineTuneFile(trainJsonlPath);
const validationFile = await uploadFineTuneFile(validationJsonlPath);

const result = {
  createdAt: new Date().toISOString(),
  trainingFile: {
    id: trainFile.id,
    filename: trainFile.filename,
    bytes: trainFile.bytes,
    purpose: trainFile.purpose
  },
  validationFile: {
    id: validationFile.id,
    filename: validationFile.filename,
    bytes: validationFile.bytes,
    purpose: validationFile.purpose
  }
};

await writeJson(fileIdsPath, result);

console.log(`Training file id: ${result.trainingFile.id}`);
console.log(`Validation file id: ${result.validationFile.id}`);
console.log(`Saved: ${relative(fileIdsPath)}`);
