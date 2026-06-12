import { access, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
export const scriptDir = path.dirname(__filename);
export const projectRoot = path.resolve(scriptDir, '..', '..');
export const generatedDir = path.join(projectRoot, 'docs', 'ai', 'fine-tuning', 'generated');

export const trainJsonlPath = path.join(generatedDir, 'work-order-analysis-train.jsonl');
export const validationJsonlPath = path.join(generatedDir, 'work-order-analysis-validation.jsonl');
export const fileIdsPath = path.join(generatedDir, 'openai-file-ids.json');
export const jobResultPath = path.join(generatedDir, 'openai-finetune-job.json');

export function parseArgs(argv) {
  const args = {
    _: []
  };

  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];

    if (!value.startsWith('--')) {
      args._.push(value);
      continue;
    }

    const [key, inlineValue] = value.slice(2).split('=', 2);

    if (inlineValue !== undefined) {
      args[key] = inlineValue;
      continue;
    }

    const next = argv[index + 1];

    if (!next || next.startsWith('--')) {
      args[key] = true;
      continue;
    }

    args[key] = next;
    index += 1;
  }

  return args;
}

export async function loadDotEnvIfPresent() {
  const envPaths = [
    path.join(projectRoot, '.env'),
    path.join(projectRoot, 'mes_backserver', '.env'),
    path.join(projectRoot, 'mcs_backserver', '.env')
  ];

  for (const envPath of envPaths) {
    try {
      const raw = await readFile(envPath, 'utf8');
      applyDotEnv(raw);
    } catch {
      // Ignore missing optional .env files.
    }
  }
}

function applyDotEnv(raw) {
  for (const line of raw.split(/\r?\n/)) {
    const trimmed = line.trim();

    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }

    const separatorIndex = trimmed.indexOf('=');

    if (separatorIndex < 1) {
      continue;
    }

    const key = trimmed.slice(0, separatorIndex).trim();
    let value = trimmed.slice(separatorIndex + 1).trim();

    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }

    if (!process.env[key]) {
      process.env[key] = value;
    }
  }
}

export function requireOpenAiApiKey() {
  const apiKey = process.env.OPENAI_API_KEY;

  if (!apiKey) {
    throw new Error(
      [
        'OPENAI_API_KEY is not set.',
        'Set it in PowerShell before running this script:',
        '$env:OPENAI_API_KEY="sk-..."'
      ].join('\n')
    );
  }

  return apiKey;
}

export async function ensureFileExists(filePath) {
  await access(filePath);
}

export async function readJson(filePath) {
  return JSON.parse(await readFile(filePath, 'utf8'));
}

export async function writeJson(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

export async function countJsonlLines(filePath) {
  const raw = await readFile(filePath, 'utf8');
  const trimmed = raw.trim();
  return trimmed ? trimmed.split(/\r?\n/).length : 0;
}

export async function callOpenAiJson({ apiKey, method, url, body }) {
  const response = await fetch(url, {
    method,
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json'
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });

  const text = await response.text();
  let parsed;

  try {
    parsed = text ? JSON.parse(text) : null;
  } catch {
    parsed = { raw: text };
  }

  if (!response.ok) {
    throw new Error(
      [
        `OpenAI API request failed: HTTP ${response.status}`,
        JSON.stringify(parsed, null, 2)
      ].join('\n')
    );
  }

  return parsed;
}

export function relative(filePath) {
  return path.relative(projectRoot, filePath);
}
