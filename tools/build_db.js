const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const dataDir = path.join(root, 'data');
const specsDir = path.join(dataDir, 'specs');
const raw = JSON.parse(fs.readFileSync(path.join(dataDir, 'models_raw.json'), 'utf8'));

const seriesOf = (brand, model) => {
  const m = model.toLowerCase();
  if (brand === 'TECNO') {
    if (m.startsWith('camon')) return 'CAMON';
    if (m.startsWith('spark')) return 'SPARK';
    if (m.startsWith('pova')) return 'POVA';
    if (m.startsWith('phantom')) return 'PHANTOM';
    if (m.startsWith('pop')) return 'POP';
    return 'OTHER';
  }
  if (m.startsWith('gt')) return 'GT';
  if (m.startsWith('note')) return 'NOTE';
  if (m.startsWith('hot')) return 'HOT';
  if (m.startsWith('zero')) return 'ZERO';
  if (m.startsWith('smart')) return 'SMART';
  if (m.startsWith('xpad')) return 'XPAD';
  return 'OTHER';
};

const matching = JSON.parse(fs.readFileSync(path.join(dataDir, 'matching.json'), 'utf8'));
const idByCatalog = {};
for (const [id, entry] of Object.entries(matching.mapping)) {
  idByCatalog[entry.catalog] = id;
}

const idOf = (brand, model) => {
  const id = idByCatalog[brand + ' ' + model];
  if (!id) throw new Error('No id mapping for ' + brand + ' ' + model);
  return id;
};

const CRITICAL = [
  ['platform', 'chipset'],
  ['platform', 'os'],
  ['display', 'type'],
  ['display', 'size'],
  ['memory', 'ram'],
  ['memory', 'storage'],
  ['main_camera', 'modules'],
  ['battery', 'capacity'],
];

const filled = (v) => v !== null && v !== undefined && String(v).trim() !== '';

const countFilled = (obj, prefix = '') => {
  let total = 0;
  let have = 0;
  for (const [k, v] of Object.entries(obj)) {
    if (v === null || v === undefined) { total += 1; continue; }
    if (typeof v === 'object' && !Array.isArray(v)) {
      const sub = countFilled(v, prefix + k + '.');
      total += sub.total;
      have += sub.have;
    } else {
      total += 1;
      if (Array.isArray(v) ? v.length : filled(v)) have += 1;
    }
  }
  return { total, have };
};

const items = [];
let quality = [];

for (const m of raw.models) {
  const id = idOf(m.brand, m.model);
  const file = path.join(specsDir, id + '.json');
  let spec = null;
  if (fs.existsSync(file)) {
    try { spec = JSON.parse(fs.readFileSync(file, 'utf8')); }
    catch (e) { spec = { error: 'invalid_json' }; }
  }
  const status = spec
    ? (spec.match_status || 'requires_manual_verification')
    : 'not_collected';

  // Preserve alternative capacities listed by GSMArena (e.g. "6160 mAh or 6500 mAh")
  if (spec && spec.battery && spec.battery.type && / or /i.test(spec.battery.type)) {
    spec.battery.capacity = spec.battery.type;
  }
  const missingCritical = [];
  if (spec) {
    for (const [group, field] of CRITICAL) {
      const g = spec[group];
      if (!g || !filled(g[field])) missingCritical.push(group + '.' + field);
    }
  }

  const record = {
    id,
    brand: m.brand,
    series: seriesOf(m.brand, m.model),
    model: m.model,
    year: m.year,
    status,
    gsmarena_url: spec ? spec.gsmarena_url : null,
    gsmarena_name: spec ? spec.gsmarena_name : null,
    match_note: spec ? spec.match_note || null : null,
    fetched_at: spec ? spec.fetched_at : null,
    specs: spec
      ? {
          release: spec.release,
          body: spec.body,
          display: spec.display,
          platform: spec.platform,
          memory: spec.memory,
          main_camera: spec.main_camera,
          selfie_camera: spec.selfie_camera,
          sound: spec.sound,
          comms: spec.comms,
          features: spec.features,
          battery: spec.battery,
          software: spec.software,
        }
      : null,
    regional_variants: [],
    images: spec && Array.isArray(spec.images) ? spec.images : [],
    verification: {
      source: 'GSMArena',
      status_gsmarena: spec ? 'reference' : null,
      verified_fields: [],
      checked_at: spec ? spec.fetched_at : null,
    },
    missing_critical: missingCritical,
  };

  const altFiles = fs.existsSync(specsDir)
    ? fs.readdirSync(specsDir).filter((f) => f.startsWith(id + '_') && f.endsWith('.json'))
    : [];  for (const f of altFiles) {
    try {
      const alt = JSON.parse(fs.readFileSync(path.join(specsDir, f), 'utf8'));
      record.regional_variants.push({
        label: alt.gsmarena_url && alt.gsmarena_url.includes('_5g') ? '5G' : alt.gsmarena_name,
        url: alt.gsmarena_url,
        specs: alt,
      });
    } catch (e) {
      record.regional_variants.push({ file: f, error: 'invalid_json' });
    }
  }
  if (record.regional_variants.length) {
    record.variant_labels = ['4G'].concat(
      record.regional_variants.map((v) => v.label).filter((l) => l && l !== '4G')
    );
    record.match_note = record.match_note
      ? record.match_note + ' Both variants are sold in Uzbekistan (confirmed 2026-11-05).'
      : 'Both variants are sold in Uzbekistan (confirmed 2026-11-05).';
  }
  items.push(record);

  const completeness = spec && spec.platform
    ? countFilled({ ...record.specs })
    : { total: 0, have: 0 };
  quality.push({
    id,
    brand: m.brand,
    model: m.model,
    status,
    completeness_pct: completeness.total
      ? Math.round((completeness.have / completeness.total) * 100)
      : 0,
    missing_critical: missingCritical,
  });
}

const byStatus = (s) => items.filter((x) => x.status === s).length;
const BRAND_RANK = { TECNO: 0, Infinix: 1 };
const SERIES_RANK = ['CAMON', 'SPARK', 'POVA', 'POP', 'PHANTOM', 'GT', 'NOTE', 'HOT', 'SMART', 'ZERO', 'XPAD', 'OTHER'];
const rank = (s) => (SERIES_RANK.includes(s) ? SERIES_RANK.indexOf(s) : 99);
const qById = Object.fromEntries(quality.map((x) => [x.id, x]));
items.sort(
  (a, b) =>
    (BRAND_RANK[a.brand] ?? 9) - (BRAND_RANK[b.brand] ?? 9) ||
    rank(a.series) - rank(b.series) ||
    a.model.localeCompare(b.model)
);
quality = items.map((x) => qById[x.id]);
const missingFreq = {};
for (const q of quality) {
  for (const f of q.missing_critical) missingFreq[f] = (missingFreq[f] || 0) + 1;
}
const report = {
  generated_at: new Date().toISOString(),
  source_list: raw.source,
  source_generated: raw.sourceGenerated,
  totals: {
    catalog_models: items.length,
    tecno: items.filter((x) => x.brand === 'TECNO').length,
    infinix: items.filter((x) => x.brand === 'Infinix').length,
    matched: byStatus('matched'),
    multiple_matches: byStatus('multiple_matches'),
    requires_manual_verification: byStatus('requires_manual_verification'),
    not_collected: byStatus('not_collected'),
  },
  by_series: items.reduce((acc, x) => {
    const k = x.brand + ' / ' + x.series;
    acc[k] = (acc[k] || 0) + 1;
    return acc;
  }, {}),
  missing_critical_frequency: missingFreq,
  low_completeness: quality.filter((q) => q.completeness_pct > 0 && q.completeness_pct < 70),
  models: quality,
};

fs.writeFileSync(path.join(dataDir, 'products.json'), JSON.stringify({ items }, null, 2));
fs.writeFileSync(path.join(dataDir, 'quality_report.json'), JSON.stringify(report, null, 2));
console.log(JSON.stringify(report.totals, null, 2));
console.log('by series:', JSON.stringify(report.by_series));
console.log('written: data/products.json, data/quality_report.json');
