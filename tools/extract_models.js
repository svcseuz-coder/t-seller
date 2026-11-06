const fs = require('fs');
const path = require('path');

const src = 'C:/Users/admin/Documents/Qoder/youtube_tecno_infinix_complaints.html';
const s = fs.readFileSync(src, 'utf8');
const i = s.indexOf('const D = ');
if (i < 0) {
  console.error('Dataset const D not found');
  process.exit(1);
}

let depth = 0, end = -1, inStr = false, esc = false, q = '';
const start = s.indexOf('{', i);
for (let k = start; k < s.length; k++) {
  const c = s[k];
  if (inStr) {
    if (esc) esc = false;
    else if (c === '\\') esc = true;
    else if (c === q) inStr = false;
    continue;
  }
  if (c === '"' || c === "'") { inStr = true; q = c; continue; }
  if (c === '{') depth++;
  else if (c === '}') {
    depth--;
    if (depth === 0) { end = k; break; }
  }
}

console.log('span', start, end, end - start);
const D = JSON.parse(s.slice(start, end + 1));
console.log('top keys:', Object.keys(D));
const list = D.stats && D.stats.models;
if (!Array.isArray(list)) {
  console.error('Model array not found. Keys:', Object.keys(D));
  process.exit(2);
}
const models = list.map((m) => ({ brand: m.brand, model: m.model, year: m.year }));

const outDir = path.join(__dirname, '..', 'data');
fs.mkdirSync(outDir, { recursive: true });
const out = {
  source: 'youtube_tecno_infinix_complaints.html',
  sourceGenerated: D.stats && D.stats.generated,
  total: models.length,
  models,
};
fs.writeFileSync(path.join(outDir, 'models_raw.json'), JSON.stringify(out, null, 2));
console.log('total', models.length);
models.forEach((m, n) => console.log(n + 1, m.brand, '|', m.model, '|', m.year));
