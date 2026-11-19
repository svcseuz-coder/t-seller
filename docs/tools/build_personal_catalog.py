"""Builds the personal (non-official) catalog: official 41 models + TECNO/Infinix/iTel
devices announced in 2025-2026 on GSMArena that are not in the official catalog.

Writes android/app/src/personal/assets/products.json (used only by the "personal" build type).
All spec values are copied as-is from GSMArena product pages; nothing is guessed.
"""

import gzip
import html
import json
import re
import ssl
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OFFICIAL = ROOT / "android/core/data/src/main/assets/products.json"
OUT = ROOT / "android/app/src/personal/assets/products.json"
REPORT = Path(__file__).with_name("personal_extras.json")
CACHE = Path(__file__).with_name(".gsmarena_cache")

SEED_VERSION = "2026-11-19.p1"
TODAY = "2026-11-18"
MIN_YEAR = 2025
UA = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/126.0 Safari/537.36"
    )
}

BRANDS = [
    ("TECNO", "T", "https://www.gsmarena.com/tecno-phones-120.php",
     "https://www.gsmarena.com/tecno-phones-f-120-0-p{n}.php"),
    ("Infinix", "I", "https://www.gsmarena.com/infinix-phones-119.php",
     "https://www.gsmarena.com/infinix-phones-f-119-0-p{n}.php"),
    ("iTel", "L", "https://www.gsmarena.com/itel-phones-131.php",
     "https://www.gsmarena.com/itel-phones-f-131-0-p{n}.php"),
]

LIST_ITEM = re.compile(
    r'<li><a href="([^"]+\.php)"><img src=https://fdn2\.gsmarena\.com/vv/bigpic/([^ "]+) title="([^"]*)"'
)


def fetch(url: str, cache_key: str | None = None, delay: float = 1.2) -> str:
    if cache_key:
        path = CACHE / cache_key
        if path.exists():
            return path.read_text(encoding="utf-8", errors="replace")
    last = None
    for attempt in range(6):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=45) as resp:
                text = resp.read().decode("utf-8", errors="replace")
            if len(text) < 5000 or "<html" not in text.lower():
                raise RuntimeError(f"blocked or truncated response ({len(text)} bytes)")
            if cache_key:
                CACHE.mkdir(exist_ok=True)
                (CACHE / cache_key).write_text(text, encoding="utf-8")
            time.sleep(delay)
            return text
        except Exception as exc:  # noqa: BLE001 - network flakiness and anti-bot throttling
            last = exc
            time.sleep(15 + attempt * 15)
    raise RuntimeError(f"failed to fetch {url}: {last}")


def wayback_snapshot(url: str):
    api = "https://archive.org/wayback/available?url=" + urllib.parse.quote(url.replace("https://", ""))
    try:
        data = json.loads(urllib.request.urlopen(urllib.request.Request(api, headers=UA), timeout=40).read())
        snap = data.get("archived_snapshots", {}).get("closest")
        if snap and snap.get("status") == "200":
            return snap["timestamp"]
    except Exception:  # noqa: BLE001 - archive lookup is best-effort
        return None
    return None


def fetch_archived(url: str, timestamp: str, cache_key: str) -> tuple[str, str]:
    path = CACHE / cache_key
    ts_file = CACHE / (cache_key + ".ts")
    if path.exists():
        ts = ts_file.read_text(encoding="utf-8").strip() if ts_file.exists() else ""
        if not ts:
            ts = wayback_snapshot(url) or timestamp or "unknown"
            ts_file.write_text(ts, encoding="utf-8")
        return path.read_text(encoding="utf-8", errors="replace"), ts
    raw = f"https://web.archive.org/web/{timestamp}id_/{url}"
    last = None
    for attempt in range(4):
        try:
            body = urllib.request.urlopen(urllib.request.Request(raw, headers=UA), timeout=60).read()
            if body[:2] == b"\x1f\x8b":
                body = gzip.decompress(body)
            text = body.decode("utf-8", errors="replace")
            if len(text) < 5000 or "data-spec" not in text:
                raise RuntimeError(f"bad archived page ({len(text)} bytes)")
            CACHE.mkdir(exist_ok=True)
            path.write_text(text, encoding="utf-8")
            ts_file.write_text(timestamp, encoding="utf-8")
            time.sleep(1.0)
            return text, timestamp
        except Exception as exc:  # noqa: BLE001 - archive hiccups, retried
            last = exc
            time.sleep(5 + attempt * 5)
    raise RuntimeError(f"failed to fetch archived {url}: {last}")


def norm_name(name: str) -> str:
    name = re.sub(r"^(tecno|infinix|itel)\s+", "", name.strip(), flags=re.I)
    name = name.replace("’", "'").replace("`", "'")
    name = re.sub(r"[^a-z0-9+]+", " ", name.lower())
    return re.sub(r"\s+", " ", name).strip()


def url_id(url: str) -> str:
    m = re.search(r"-(\d+)\.php$", url)
    return m.group(1) if m else url


def listing_candidates(no_live: bool):
    seen = set()
    for brand, prefix, base, page_pat in BRANDS:
        for page in range(1, 6):
            url = base if page == 1 else page_pat.format(n=page)
            key = f"list_{prefix.lower()}_{page}.html"
            if no_live and not (CACHE / key).exists():
                print(f"  {brand}: page {page} not cached, stop", flush=True)
                break
            text = fetch(url, key)
            items = LIST_ITEM.findall(text)
            if not items:
                print(f"  {brand}: empty page {page}, stop", flush=True)
                break
            old = 0
            for slug, img, title in items:
                title = html.unescape(title)
                name = title.split(" Android ")[0].split(" (")[0].strip()
                m = re.search(r"Announced (\w+) (\d{4})", title)
                year = int(m.group(2)) if m else None
                foldable = bool(re.search(r"fold|flip", name, re.I))
                if year is None or (year < MIN_YEAR and not foldable):
                    old += 1
                    continue
                key = (brand, norm_name(name))
                if key in seen:
                    continue
                seen.add(key)
                yield {
                    "brand": brand,
                    "prefix": prefix,
                    "name": name,
                    "url": "https://www.gsmarena.com/" + slug,
                    "photo": "https://fdn2.gsmarena.com/vv/bigpic/" + img,
                    "announced": m.group(0).replace("Announced ", ""),
                    "year": year,
                }
            print(f"  {brand}: page {page}, {len(items)} items", flush=True)
            if old == len(items):
                break


def parse_rows(text: str):
    rows = []
    section = None
    for m in re.finditer(r"<tr>(.*?)</tr>", text, re.S):
        row = m.group(1)
        th = re.search(r"<th[^>]*>(.*?)</th>", row, re.S)
        if th:
            label = re.sub(r"<[^>]+>", "", th.group(1)).strip()
            if label:
                section = label
        ttl = re.search(r'<td class="ttl"[^>]*>(.*?)</td>', row, re.S)
        nfo = re.search(r'<td class="nfo"([^>]*)>(.*?)</td>', row, re.S)
        if not nfo:
            continue
        spec = None
        sa = re.search(r'data-spec="([^"]+)"', nfo.group(1) or "")
        if sa:
            spec = sa.group(1)
        value = re.sub(r"<br\s*/?>", "; ", nfo.group(2))
        value = re.sub(r"<[^>]+>", "", value)
        value = re.sub(r"\s+", " ", html.unescape(value)).strip()
        label = re.sub(r"<[^>]+>", "", ttl.group(1)).strip() if ttl else ""
        rows.append((section, label.lower(), spec, value))
    return rows


def clean(value: str | None) -> str | None:
    if value is None:
        return None
    value = value.strip()
    if not value or value == "-" or value.lower() == "unspecified":
        return None
    return value


def lookup(rows, section=None, label=None, spec=None):
    for sec, lab, sp, val in rows:
        if section and (sec or "").lower() != section.lower():
            continue
        if label and lab != label:
            continue
        if spec and sp != spec:
            continue
        v = clean(val)
        if v:
            return v
    return None


def parse_specs(rows, brand, name):
    tech = lookup(rows, "Network", "technology", "nettech") or ""
    announced = lookup(rows, "Launch", "announced", "year")
    status = lookup(rows, "Launch", "status", "status")

    dims = lookup(rows, "Body", "dimensions", "dimensions")
    weight = lookup(rows, "Body", "weight", "weight")
    build = lookup(rows, "Body", "build")
    sim = lookup(rows, "Body", "sim", "sim")
    ip = lookup(rows, "Body", "ip rating")
    if not ip:
        ip = lookup(rows, "Body", spec="bodyother")

    dtype = lookup(rows, "Display", "type", "displaytype")
    dsize = lookup(rows, "Display", "size", "displaysize")
    dres = lookup(rows, "Display", "resolution", "displayresolution")
    dprot = lookup(rows, "Display", "protection", "displayprotection")
    dextra = lookup(rows, "Display", "extra features", "displayother")

    os_ = lookup(rows, "Platform", "os", "os")
    chipset = lookup(rows, "Platform", "chipset", "chipset")
    cpu = lookup(rows, "Platform", "cpu", "cpu")
    gpu = lookup(rows, "Platform", "gpu", "gpu")

    card = lookup(rows, "Memory", "card slot", "memoryslot")
    internal = lookup(rows, "Memory", "internal", "internalmemory")

    cam1 = lookup(rows, "Main Camera", spec="cam1modules")
    cam1f = lookup(rows, "Main Camera", "features", "cam1features")
    cam1v = lookup(rows, "Main Camera", "video", "cam1video")
    cam2 = lookup(rows, "Selfie camera", spec="cam2modules")
    cam2f = lookup(rows, "Selfie camera", "features", "cam2features")
    cam2v = lookup(rows, "Selfie camera", "video", "cam2video")

    speakers = lookup(rows, "Sound", "loudspeaker")
    jack = lookup(rows, "Sound", "3.5mm jack") or lookup(rows, "Sound", "3.5 mm jack")

    wlan = lookup(rows, "Comms", "wlan", "wlan")
    bt = lookup(rows, "Comms", "bluetooth", "bluetooth")
    gps = lookup(rows, "Comms", "positioning", "gps") or lookup(rows, "Comms", "gps", "gps")
    nfc = lookup(rows, "Comms", "nfc", "nfc")
    usb = lookup(rows, "Comms", "usb", "usb")

    sensors = lookup(rows, "Features", "sensors", "sensors")
    battery_type = lookup(rows, "Battery", "type", "batdescription1")
    charging = lookup(rows, "Battery", "charging")

    colors = lookup(rows, "Misc", "colors", "colors")
    models = lookup(rows, "Misc", "models", "models")
    if models and "see all" in models.lower():
        models = None

    ram, storage = None, None
    if internal:
        stor = []
        rams = []
        for part in internal.split(","):
            m = re.search(r"(\d+(?:\.\d+)?\s*(?:GB|TB))\s+(\d+(?:\.\d+)?\s*GB)\s+RAM", part.strip(), re.I)
            if m:
                if m.group(1).upper() not in stor:
                    stor.append(m.group(1).upper())
                if m.group(2).upper() not in rams:
                    rams.append(m.group(2).upper())
        ram = ", ".join(rams) if rams else None
        storage = ", ".join(stor) if stor else None

    refresh = None
    for text in (dtype, dsize, dextra):
        if text:
            m = re.search(r"(\d{2,3})\s*Hz", text)
            if m:
                refresh = m.group(1) + "Hz"
                break
    brightness = None
    if dextra:
        m = re.search(r"(\d[\d,]*)\s*nits", dextra, re.I)
        if m:
            brightness = m.group(0)

    capacity = None
    if battery_type:
        m = re.search(r"(\d{3,5})\s*mAh", battery_type)
        capacity = f"{m.group(1)} mAh" if m else None
    wired = charging if charging and "wired" in charging.lower() else None
    wireless = charging if charging and "wireless" in charging.lower() else None

    fingerprint = None
    if sensors:
        m = re.search(r"Fingerprint\s*\(([^)]+)\)", sensors)
        if m:
            fingerprint = m.group(1).strip()

    software = {}
    if os_:
        parts = [p.strip() for p in os_.split(",") if p.strip()]
        for part in parts:
            if part.lower().startswith("android"):
                software["android"] = part
            elif "update" in part.lower() and "updates" not in software:
                software["updates"] = part
            elif "android" not in part.lower() and part.lower() not in ("updates",):
                software.setdefault("custom_ui", part)
    software = {k: v for k, v in software.items() if v}

    release = {
        "announced": announced or None,
        "status": status,
    }
    if models:
        release = {"models": models, **release}

    specs = {
        "release": release,
        "body": {
            "dimensions": dims,
            "weight": weight,
            "build": build,
            "sim": sim,
            "ip_rating": ip,
            "colors": colors,
        },
        "display": {
            "type": dtype,
            "size": dsize,
            "resolution": dres,
            "refresh_rate": refresh,
            "brightness": brightness,
            "protection": dprot,
        },
        "platform": {"chipset": chipset, "cpu": cpu, "gpu": gpu, "os": os_},
        "memory": {"ram": ram, "storage": storage, "card_slot": card},
        "main_camera": {
            "modules": cam1,
            "features": cam1f,
            "video": cam1v,
            "ois": "Yes" if cam1 and re.search(r"\bOIS\b", cam1) else None,
            "eis": "Yes" if cam1 and re.search(r"\bEIS\b", cam1) else None,
        },
        "selfie_camera": {"modules": cam2, "features": cam2f, "video": cam2v},
        "battery": {
            "capacity": capacity,
            "type": None,
            "wired_charging": wired,
            "wireless_charging": wireless,
            "reverse_charging": None,
            "bypass_charging": None,
        },
        "comms": {
            "network_5g": "Yes" if "5G" in tech else None,
            "nfc": nfc,
            "wlan": wlan,
            "bluetooth": bt,
            "gps": gps,
            "usb": usb,
        },
        "sound": {"speakers": speakers, "jack": jack},
        "features": {"fingerprint": fingerprint, "face_unlock": None, "sensors": sensors},
    }
    if software:
        specs["software"] = software

    # Drop empty groups and empty values.
    result = {}
    for group, fields in specs.items():
        kept = {k: v for k, v in fields.items() if v}
        if kept:
            result[group] = kept
    return result


def series_of(name: str, brand: str) -> str:
    token = re.split(r"\s+", name.strip())[0]
    if re.search(r"\d", token):
        return brand.upper()
    token = token.upper()
    return token


def main():
    cached_only = "--cached-only" in sys.argv
    wayback = "--wayback" in sys.argv
    official = json.loads(OFFICIAL.read_text(encoding="utf-8"))
    items = official["items"]
    official_ids = {url_id(i["gsmarena_url"]) for i in items if i.get("gsmarena_url")}
    official_names = {norm_name(i["model"]) for i in items}
    variant_bases = {norm_name(i["model"]) for i in items if i.get("variant_labels")}

    print("Collecting GSMArena candidates (2025-2026)...")
    candidates = []
    for cand in listing_candidates(cached_only or wayback):
        name = norm_name(cand["name"])
        if url_id(cand["url"]) in official_ids or name in official_names:
            continue
        if re.sub(r" (4g|5g)$", "", name) in variant_bases:
            continue
        candidates.append(cand)

    print(f"Candidates after de-duplication: {len(candidates)}", flush=True)
    extras = []
    counters = {}
    skipped = []
    for cand in candidates:
        brand = cand["brand"]
        slug = cand["url"].rsplit("/", 1)[-1]
        page_key = "page_" + slug + ".html"
        arch_key = "arch_" + slug + ".html"
        archived_at = None
        if (CACHE / page_key).exists():
            try:
                text = fetch(cand["url"], page_key)
            except RuntimeError as exc:
                skipped.append((cand["name"], str(exc)))
                continue
        elif (CACHE / arch_key).exists():
            text, archived_at = fetch_archived(cand["url"], "", arch_key)
        elif cached_only:
            skipped.append((cand["name"], "not cached (GSMArena 429)"))
            continue
        elif wayback:
            ts = wayback_snapshot(cand["url"])
            if not ts:
                skipped.append((cand["name"], "no archive snapshot"))
                continue
            try:
                text, archived_at = fetch_archived(cand["url"], ts, arch_key)
            except RuntimeError as exc:
                skipped.append((cand["name"], str(exc)))
                continue
        else:
            try:
                text = fetch(cand["url"], page_key)
            except RuntimeError as exc:
                skipped.append((cand["name"], str(exc)))
                continue
        rows = parse_rows(text)
        specs = parse_specs(rows, brand, cand["name"])
        release = specs.get("release", {})
        announced = release.get("announced") or cand["announced"]
        m = re.search(r"(\d{4})", announced or "")
        year = int(m.group(1)) if m else cand["year"]
        foldable = re.search(r"fold|flip", cand["name"], re.I) is not None
        if year < MIN_YEAR and not foldable:
            continue
        status = release.get("status") or ""
        if status.lower().startswith("cancelled"):
            skipped.append((cand["name"], "cancelled"))
            continue
        counters[brand] = counters.get(brand, 0) + 1
        pid = f"{cand['prefix']}X{counters[brand]:02d}"
        model = re.sub(r"^(tecno|infinix|itel)\s+", "", cand["name"], flags=re.I)
        verification = {"source": "GSMArena", "checked_at": TODAY}
        note = "GSMArena-only model (personal catalog)"
        if archived_at:
            verification["archived_at"] = archived_at
            note += f"; page read from the Internet Archive snapshot {archived_at}"
        extras.append({
            "id": pid,
            "brand": brand,
            "series": series_of(model, brand),
            "model": model,
            "year": year,
            "status": "gsmarena",
            "gsmarena_url": cand["url"],
            "gsmarena_name": cand["name"],
            "match_note": note,
            "fetched_at": TODAY,
            "verification": verification,
            "specs": specs,
            "images": [cand["photo"]],
        })

    extras.sort(key=lambda e: (e["brand"], -e["year"], e["model"]))
    out = {
        "seed_version": SEED_VERSION,
        "items": items + extras,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    REPORT.write_text(json.dumps({
        "source": "https://www.gsmarena.com/",
        "fetched_at": TODAY,
        "min_year": MIN_YEAR,
        "official_count": len(items),
        "extra_count": len(extras),
        "skipped": skipped,
        "items": [
            {"id": e["id"], "brand": e["brand"], "model": e["model"], "year": e["year"],
             "url": e["gsmarena_url"], "models": e["specs"].get("release", {}).get("models")}
            for e in extras
        ],
    }, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")

    print(f"Official: {len(items)}, extras: {len(extras)} -> total {len(out['items'])}")
    for brand in ("TECNO", "Infinix", "iTel"):
        print(f"  {brand}: +{counters.get(brand, 0)}")
    if skipped:
        print("Skipped:")
        for name, why in skipped:
            print(f"  {name}: {why}")
    with_models = sum(1 for e in extras if e["specs"].get("release", {}).get("models"))
    print(f"With GSMArena model code: {with_models}/{len(extras)}")


if __name__ == "__main__":
    sys.exit(main())
