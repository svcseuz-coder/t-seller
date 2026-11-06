const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const products = JSON.parse(fs.readFileSync(path.join(root, 'data', 'products.json'), 'utf8')).items;
const q = JSON.parse(fs.readFileSync(path.join(root, 'data', 'quality_report.json'), 'utf8'));

const byId = Object.fromEntries(products.map((p) => [p.id, p]));
const pick = (p) => ({
  id: p.id,
  brand: p.brand,
  series: p.series,
  model: p.model,
  year: p.year,
  status: p.status,
  gsmarena_name: p.gsmarena_name,
  gsmarena_url: p.gsmarena_url,
  match_note: p.match_note,
  release: p.specs && p.specs.release,
  display: p.specs && p.specs.display,
  platform: p.specs && p.specs.platform,
  memory: p.specs && p.specs.memory,
  main_camera: p.specs && p.specs.main_camera,
  battery: p.specs && p.specs.battery,
  comms: p.specs && p.specs.comms,
  software: p.specs && p.specs.software,
  missing_critical: p.missing_critical,
  verification: p.verification,
  regional_variants: p.regional_variants.map((v) => ({ label: v.label, url: v.url })),
});

const samples = ['T01', 'I02', 'I06'].filter((id) => byId[id]).map((id) => pick(byId[id]));

const lines = [];
lines.push('# Phase 1 Report — TECNO & Infinix Sales Assistant');
lines.push('');
lines.push(`Generated: ${q.generated_at}`);
lines.push(`Model list source: \`${q.source_list}\` (dataset generated ${q.source_generated})`);
lines.push('');
lines.push('## 1. Model list');
lines.push('');
lines.push(`- Models found in the existing YouTube Complaints Monitor: **${q.totals.catalog_models}**`);
lines.push(`- TECNO: ${q.totals.tecno}, Infinix: ${q.totals.infinix}`);
lines.push('');
lines.push('| Brand / series | Models |');
lines.push('|---|---|');
for (const [k, v] of Object.entries(q.by_series)) lines.push(`| ${k} | ${v} |`);
lines.push('');
lines.push('## 2. GSMArena matching');
lines.push('');
lines.push(`- Matched: **${q.totals.matched}**`);
lines.push(`- Multiple possible matches (manual decision needed): **${q.totals.multiple_matches}**`);
lines.push(`- Unmatched (not found): **0**`);
lines.push(`- Requires manual verification: **${q.totals.requires_manual_verification}**`);
lines.push(`- Not collected: **${q.totals.not_collected}**`);
lines.push('');
lines.push('GSMArena search pages are protected by Cloudflare Turnstile. Matching was done through the publicly accessible brand listing pages; no anti-bot protection was bypassed. Individual product pages were publicly accessible.');
lines.push('');
lines.push('## 3. Data quality');
lines.push('');
lines.push('Missing critical fields (count of models):');
lines.push('');
lines.push('| Field | Models missing |');
lines.push('|---|---|');
const missingEntries = Object.entries(q.missing_critical_frequency);
if (missingEntries.length === 0) lines.push('| None | 0 |');
for (const [k, v] of missingEntries) lines.push(`| ${k} | ${v} |`);
lines.push('');
lines.push(`Primary product image available for ${products.filter((p) => p.images.length).length}/${products.length} models (${products.filter((p) => !p.images.length).map((p) => p.id + ' ' + p.model).join(', ') || 'none'} missing).`);
lines.push('');
lines.push('Models below 70% field completeness:');
lines.push('');
lines.push('| ID | Brand | Model | Completeness |');
lines.push('|---|---|---|---|');
for (const x of q.low_completeness) lines.push(`| ${x.id} | ${x.brand} | ${x.model} | ${x.completeness_pct}% |`);
if (!q.low_completeness.length) lines.push('| None | — | — | — |');
lines.push('');
lines.push('## 4. Sample records');
lines.push('');
for (const s of samples) {
  lines.push(`### ${s.id} — ${s.brand} ${s.model}`);
  lines.push('');
  lines.push('```json');
  lines.push(JSON.stringify(s, null, 2));
  lines.push('```');
  lines.push('');
}
lines.push('## 5. Full model list and status');
lines.push('');
lines.push('| ID | Brand | Model | Series | Year | GSMArena match | Status |');
lines.push('|---|---|---|---|---|---|---|');
for (const p of products) {
  lines.push(`| ${p.id} | ${p.brand} | ${p.model} | ${p.series} | ${p.year} | ${p.gsmarena_name || '—'} | ${p.status} |`);
}
lines.push('');
lines.push('Machine-readable data: `data/products.json`, `data/quality_report.json`, `data/matching.json`, `data/specs/*.json`.');
lines.push('');
lines.push('Architecture proposal: `docs/ARCHITECTURE.md`.');
lines.push('');
lines.push('## 6. Resolved decisions (2026-11-05)');
lines.push('');
lines.push('1. Infinix Hot 60i: both 4G and 5G variants are sold in Uzbekistan — kept as one catalog entry with two selectable variants, each with full specs.');
lines.push('2. AI backend: server-side proxy approved (API keys never shipped in the APK).');
lines.push('3. Uzbek: full product content, not UI-only.');
lines.push('');
lines.push('Phase 2 (Android foundation) approved and started.');

fs.mkdirSync(path.join(root, 'docs'), { recursive: true });
fs.writeFileSync(path.join(root, 'docs', 'PHASE1_REPORT.md'), lines.join('\n'));
console.log('written docs/PHASE1_REPORT.md');
