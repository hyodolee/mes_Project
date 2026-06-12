import {
  callOpenAiJson,
  fileIdsPath,
  jobResultPath,
  loadDotEnvIfPresent,
  parseArgs,
  readJson,
  relative,
  requireOpenAiApiKey,
  writeJson
} from './openai-finetune-utils.mjs';

const args = parseArgs(process.argv.slice(2));

if (args.help) {
  console.log(`
Usage:
  node scripts\\ai\\create-finetune-job.mjs [training_file_id] [validation_file_id] [--model gpt-5.4-mini-2026-03-17] [--suffix mes-mcs-work-order-analysis] [--dry-run]

Default file ids:
  docs\\ai\\fine-tuning\\generated\\openai-file-ids.json

Required when not using --dry-run:
  OPENAI_API_KEY
`);
  process.exit(0);
}

await loadDotEnvIfPresent();

let trainingFileId = args._[0];
let validationFileId = args._[1];

if (!trainingFileId || !validationFileId) {
  const uploadedFiles = await readJson(fileIdsPath);
  trainingFileId ||= uploadedFiles.trainingFile?.id;
  validationFileId ||= uploadedFiles.validationFile?.id;
}

if (!trainingFileId) {
  throw new Error('Training file id is required. Run upload-finetune-files.mjs first.');
}

if (!validationFileId) {
  throw new Error('Validation file id is required. Run upload-finetune-files.mjs first.');
}

const model = args.model || process.env.OPENAI_FINE_TUNE_BASE_MODEL || 'gpt-5.4-mini-2026-03-17';
const suffix = args.suffix || process.env.OPENAI_FINE_TUNE_SUFFIX || 'mes-mcs-work-order-analysis';

const payload = {
  model,
  training_file: trainingFileId,
  validation_file: validationFileId,
  suffix,
  method: {
    type: 'supervised'
  }
};

console.log('Fine-tuning job request:');
console.log(JSON.stringify(payload, null, 2));

if (args['dry-run']) {
  console.log('Dry run only. No fine-tuning job was created.');
  process.exit(0);
}

const apiKey = requireOpenAiApiKey();
const job = await callOpenAiJson({
  apiKey,
  method: 'POST',
  url: 'https://api.openai.com/v1/fine_tuning/jobs',
  body: payload
});

await writeJson(jobResultPath, job);

console.log(`Fine-tuning job id: ${job.id}`);
console.log(`Status: ${job.status}`);
console.log(`Saved: ${relative(jobResultPath)}`);
