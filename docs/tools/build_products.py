"""Rebuild core/data assets/products.json from the vendor spec workbook.

Usage: python docs/tools/build_products.py <workbook.xlsx> [--out products.json]

Existing products keep their id, image URLs and GSMArena links; new models get
fresh ids. Hot 60i's 4G/5G columns are merged back into one product with a
regional variant, matching the catalog schema.
"""
import argparse
import json
import re
import sys
from pathlib import Path

import openpyxl

sys.path.insert(0, str(Path(__file__).parent))
from parse_spec_sheet import read_sheet  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_OUT = ROOT / "android/core/data/src/main/assets/products.json"
SEED_VERSION = "2026-11-19.1"
CHECKED_AT = "2026-11-19"
MODELS_JSON = Path(__file__).with_name("gsmarena_models.json")

# Official product shots from the GSMArena pages whose specs match the sheet.
PHOTOS = {
    ("Infinix", "Xpad 20"): "https://fdn2.gsmarena.com/vv/bigpic/infinix-xpad20.jpg",
    ("Infinix", "Xpad 20 Pro"): "https://fdn2.gsmarena.com/vv/bigpic/infinix-xpad-20-pro.jpg",
    ("iTel", "A100C"): "https://fdn2.gsmarena.com/vv/bigpic/itel-a100c.jpg",
    ("iTel", "A200"): "https://fdn2.gsmarena.com/vv/bigpic/itel-a200.jpg",
    ("iTel", "A200+"): "https://fdn2.gsmarena.com/vv/bigpic/itel-a200-plus.jpg",
    ("iTel", "A300C"): "https://fdn2.gsmarena.com/vv/bigpic/itel-a300c.jpg",
    ("iTel", "A300+"): "https://fdn2.gsmarena.com/vv/bigpic/itel-a300.jpg",
    ("iTel", "City 200"): "https://fdn2.gsmarena.com/vv/bigpic/itel-city-200-.jpg",
    ("iTel", "City 200s"): "https://fdn2.gsmarena.com/vv/bigpic/itel-city-200s.jpg",
    ("iTel", "Power 80"): "https://fdn2.gsmarena.com/vv/bigpic/itel-power80.jpg",
    ("iTel", "S26 Ultra"): "https://fdn2.gsmarena.com/vv/bigpic/itel-super-26-ultra.jpg",
}

SOURCES = {
    "TECNO": "GSMArena; TECNO vendor sheet",
    "Infinix": "GSMArena + Infinix sales dept",
    "iTel": "Infinix sales dept (iTel sheet)",
}
NEW_IDS = {
    ("Infinix", "Xpad 20"): "I12",
    ("Infinix", "Xpad 20 Pro"): "I13",
    ("iTel", "A200"): "L08",
    ("iTel", "A200+"): "L09",
}


def clean(value):
    if value is None:
        return None
    text = re.sub(r"\s+", " ", value.replace("：", ": ").replace("，", ", ").replace("；", "; ")).strip()
    text = text.lstrip(":;, ")
    text = re.sub(r"(?<=\d),(?=\d)", ".", text)
    text = re.sub(r"(?<=\d)\s+(Hz|GB|TB|W)\b", r"\1", text)
    text = re.sub(r"(?<=\d)(mAh)\b", r" \1", text)
    return text or None


def tidy(specs):
    return {group: {key: clean(value) for key, value in fields.items()} for group, fields in specs.items()}


def model_key(brand, model):
    return brand.lower(), model.lower()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("workbook")
    parser.add_argument("--out", default=str(DEFAULT_OUT))
    args = parser.parse_args()

    out_path = Path(args.out)
    current = json.loads(out_path.read_text(encoding="utf-8"))
    existing = {model_key(p["brand"], p["model"]): p for p in current["items"]}
    model_codes = json.loads(MODELS_JSON.read_text(encoding="utf-8"))["items"]

    wb = openpyxl.load_workbook(args.workbook, data_only=True)
    sheets = (("TECNO", "TECNO"), ("Infinix", "Infinix"), ("ITEL", "iTel"))

    items = [p for p in current["items"]]
    index = {p["id"]: i for i, p in enumerate(items)}
    added, updated = [], 0

    def base_id(brand, model, series):
        key = model_key(brand, model)
        if key in existing:
            return existing[key]["id"]
        new = NEW_IDS.get((brand, model))
        if new:
            return new
        prefix = {"TECNO": "T", "Infinix": "I", "iTel": "L"}[brand]
        taken = {p["id"] for p in items}
        n = 1
        while f"{prefix}{n:02d}" in taken:
            n += 1
        return f"{prefix}{n:02d}"

    for sheet, brand in sheets:
        models = read_sheet(wb, sheet)
        if not models:
            raise SystemExit(f"no model columns in sheet {sheet}")

        # Merge split variants (Hot 60i · 4G / · 5G) into one product.
        merged, order = {}, []
        for model in models:
            base = re.sub(r"\s*·\s*(4G|5G)$", "", model["name"]).strip()
            key = (brand, base)
            if key not in merged:
                merged[key] = {"base": None, "variants": {}}
                order.append(key)
            entry = merged[key]
            if model["variant"]:
                entry["variants"][model["variant"]] = model
                if model["variant"].upper() == "4G":
                    entry["base"] = model
            else:
                entry["base"] = model

        for key in order:
            brand_name, model_name = key
            entry = merged[key]
            main_model = entry["base"] or next(iter(entry["variants"].values()))
            series = main_model["series"] or re.match(r"[A-Za-z]+", model_name).group(0).upper()
            year = main_model["year"] or next((m["year"] for m in entry["variants"].values() if m["year"]), None)
            if year is None:
                raise SystemExit(f"{brand_name} {model_name}: no year in sheet")

            pid = base_id(brand_name, model_name, series)
            old = existing.get(model_key(brand_name, model_name))
            specs = tidy(main_model["specs"])
            code = (model_codes.get(pid) or {}).get("models")
            if code:
                specs["release"] = {"models": code, **specs["release"]}
            item = {
                "id": pid,
                "brand": brand_name,
                "series": series,
                "model": model_name,
                "year": year,
                "status": (old or {}).get("status") or "vendor_provided",
                "gsmarena_url": (old or {}).get("gsmarena_url"),
                "gsmarena_name": (old or {}).get("gsmarena_name"),
                "match_note": (old or {}).get("match_note"),
                "fetched_at": (old or {}).get("fetched_at") or SEED_VERSION,
                "specs": specs,
                "regional_variants": [],
                "images": (old or {}).get("images") or ([PHOTOS[(brand_name, model_name)]] if (brand_name, model_name) in PHOTOS else []),
                "verification": {
                    "source": SOURCES[brand_name],
                    "status_gsmarena": "reference" if old else "vendor",
                    "verified_fields": [],
                    "checked_at": CHECKED_AT,
                },
                "missing_critical": [],
            }

            old_variants = {v["label"]: v for v in (old or {}).get("regional_variants") or []}
            if entry["variants"]:
                item["variant_labels"] = sorted(entry["variants"].keys())
                for label, model in sorted(entry["variants"].items()):
                    if label == "4G" and entry["base"] is model:
                        continue
                    variant = {
                        "label": label,
                        "url": (old_variants.get(label) or {}).get("url"),
                        "specs": tidy(model["specs"]),
                    }
                    item["regional_variants"].append(variant)

            if pid in index:
                items[index[pid]] = item
                updated += 1
            else:
                index[pid] = len(items)
                items.append(item)
                added.append(item["id"])

        ids = [p["id"] for p in items]
        if len(ids) != len(set(ids)):
            raise SystemExit("duplicate ids: " + ", ".join(sorted({i for i in ids if ids.count(i) > 1})))
        names = [(p["brand"], p["model"]) for p in items]
        if len(names) != len(set(names)):
            raise SystemExit("duplicate brand/model pairs")

    out_path.write_text(
        json.dumps({"seed_version": SEED_VERSION, "items": items}, ensure_ascii=False, indent=1) + "\n",
        encoding="utf-8",
    )
    print(f"items={len(items)} updated={updated} added={added}")


if __name__ == "__main__":
    main()
