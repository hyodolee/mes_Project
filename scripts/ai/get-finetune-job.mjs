import {
  callOpenAiJson,
  jobResultPath,
  loadDotEnvIfPresent,
  parseArgs,
  readJson,
  requireOpenAiApiKey
} from './openai-finetune-utils.mjs';

const args = parseArgs(process.argv.slice(2));

if (args.help) {
  console.log(`
Usage:
  node scripts\\ai\\get-finetune-job.mjs [job_id]

If job_id is omitted, the script reads:
  docs\\ai\\fine-tuning\\generated\\openai-finetune-job.json

Required:
  OPENAI_API_KEY
`);
  process.exit(0);
}

await loadDotEnvIfPresent();

let jobId = args._[0];

if (!jobId) {
  const savedJob = await readJson(jobResultPath);
  jobId = savedJob.id;
}

if (!jobId) {
  throw new Error('Fine-tuning job id is required.');
}

const apiKey = requireOpenAiApiKey();
const job = await callOpenAiJson({
  apiKey,
  method: 'GET',
  url: `https://api.openai.com/v1/fine_tuning/jobs/${encodeURIComponent(jobId)}`
});

console.log(`Job id: ${job.id}`);
console.log(`Status: ${job.status}`);

if (job.fine_tuned_model) {
  console.log(`Fine-tuned model: ${job.fine_tuned_model}`);
}

if (job.error) {
  console.log('Error:');
  console.log(JSON.stringify(job.error, null, 2));
}
