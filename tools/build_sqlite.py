import json
import sqlite3
from pathlib import Path

root = Path(__file__).resolve().parent.parent
data = json.loads((root / "data" / "products.json").read_text(encoding="utf-8"))["items"]
db_path = root / "data" / "sales_assistant.db"
if db_path.exists():
    db_path.unlink()

con = sqlite3.connect(db_path)
cur = con.cursor()
cur.executescript(
    """
PRAGMA foreign_keys = ON;
CREATE TABLE products (
  id TEXT PRIMARY KEY,
  brand TEXT NOT NULL,
  series TEXT NOT NULL,
  model TEXT NOT NULL,
  year INTEGER,
  status TEXT NOT NULL,
  gsmarena_url TEXT,
  gsmarena_name TEXT,
  match_note TEXT,
  fetched_at TEXT,
  missing_critical TEXT
);
CREATE TABLE product_images (
  product_id TEXT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  url TEXT NOT NULL,
  is_primary INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (product_id, url)
);
CREATE TABLE specs (
  product_id TEXT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  group_name TEXT NOT NULL,
  field_name TEXT NOT NULL,
  value TEXT,
  source TEXT,
  verification_status TEXT,
  last_updated TEXT,
  region_variant TEXT NOT NULL DEFAULT 'global',
  PRIMARY KEY (product_id, region_variant, group_name, field_name)
);
CREATE TABLE regional_variants (
  product_id TEXT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  label TEXT,
  url TEXT,
  specs_json TEXT,
  PRIMARY KEY (product_id, url)
);
CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT);
CREATE INDEX idx_products_brand_series ON products(brand, series);
CREATE INDEX idx_specs_field ON specs(field_name, value);
"""
)


def flatten(obj, prefix=""):
    out = []
    for k, v in obj.items():
        key = f"{prefix}{k}"
        if isinstance(v, dict):
            out.extend(flatten(v, key + "."))
        elif isinstance(v, list):
            out.append((key, json.dumps(v, ensure_ascii=False)))
        elif v is not None and str(v).strip() != "":
            out.append((key, str(v)))
    return out


for p in data:
    cur.execute(
        "INSERT INTO products VALUES (?,?,?,?,?,?,?,?,?,?,?)",
        (
            p["id"], p["brand"], p["series"], p["model"], p["year"], p["status"],
            p["gsmarena_url"], p["gsmarena_name"], p["match_note"], p["fetched_at"],
            json.dumps(p.get("missing_critical", [])),
        ),
    )
    for i, url in enumerate(p.get("images", [])):
        cur.execute(
            "INSERT INTO product_images VALUES (?,?,?)",
            (p["id"], url, 1 if i == 0 else 0),
        )
    if p.get("specs"):
        for group, fields in p["specs"].items():
            if not isinstance(fields, dict):
                continue
            for field, value in fields.items():
                if value is None or str(value).strip() == "":
                    continue
                if isinstance(value, (dict, list)):
                    value = json.dumps(value, ensure_ascii=False)
                cur.execute(
                    "INSERT OR REPLACE INTO specs VALUES (?,?,?,?,?,?,?,?)",
                    (
                        p["id"], group, field, str(value), "GSMArena",
                        "gsmarena_reference", p["fetched_at"], "global",
                    ),
                )
    for idx, v in enumerate(p.get("regional_variants", [])):
        variant = v.get("label") or v.get("url") or f"variant-{idx}"
        for field, value in flatten(v.get("specs") or {}):
            group, _, name = field.partition(".")
            cur.execute(
                "INSERT OR REPLACE INTO specs VALUES (?,?,?,?,?,?,?,?)",
                (
                    p["id"], group, name, value, "GSMArena",
                    "gsmarena_reference", p["fetched_at"], variant,
                ),
            )
        cur.execute(
            "INSERT OR REPLACE INTO regional_variants VALUES (?,?,?,?)",
            (p["id"], v.get("label"), v.get("url"), json.dumps(v.get("specs"), ensure_ascii=False)),
        )

cur.executemany(
    "INSERT INTO meta VALUES (?,?)",
    [
        ("schema_version", "1"),
        ("data_source", "GSMArena (public product pages)"),
        ("catalog_source", "YouTube Complaints Monitor model list"),
        ("checked_at", "2026-11-05"),
        ("notes", "GSMArena is a reference source, not official regional specification authority. Fields absent on GSMArena are stored as NULL and must render as Not verified."),
    ],
)
con.commit()

print("products:", cur.execute("SELECT COUNT(*) FROM products").fetchone()[0])
print("specs rows:", cur.execute("SELECT COUNT(*) FROM specs").fetchone()[0])
print("images:", cur.execute("SELECT COUNT(*) FROM product_images").fetchone()[0])
print("variants:", cur.execute("SELECT COUNT(*) FROM regional_variants").fetchone()[0])
con.close()
print("written", db_path)
