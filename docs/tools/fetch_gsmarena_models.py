"""Fetch official model codes (GSMArena "Models" row) for every catalog item.

Usage: python docs/tools/fetch_gsmarena_models.py
Writes docs/tools/gsmarena_models.json keyed by product id. Product URLs come
from products.json; models without one (iTel line, Xpad 20/20 Pro) are resolved
through the brand listing page by their official photo file name.
"""
import json
import re
import sys
import time
import urllib.request
from html import unescape
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from build_products import PHOTOS  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
PRODUCTS = ROOT / "android/core/data/src/main/assets/products.json"
OUT = ROOT / "docs/tools/gsmarena_models.json"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/131.0 Safari/537.36")
LISTINGS = {
    "Infinix": "https://www.gsmarena.com/infinix-phones-119.php",
    "iTel": "https://www.gsmarena.com/itel-phones-131.php",
}


def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as response:
        return response.read().decode("utf-8", "ignore")


def models_of(html):
    match = re.search(r'data-spec="models">(.*?)</td>', html, re.S)
    if not match:
        return None
    value = unescape(re.sub(r"<[^>]+>", "", match.group(1)))
    value = re.sub(r"\s+", " ", value).strip()
    return value or None


def listing_links(url):
    html = get(url)
    links = {}
    for href, img in re.findall(r'<a href="([^"]+)"><img src=(https?://[^ >]+)', html):
        links[img.rsplit("/", 1)[-1].lower()] = "https://www.gsmarena.com/" + href
    return links


def main():
    items = json.loads(PRODUCTS.read_text(encoding="utf-8"))["items"]
    cache = {}
    resolved = {}
    for item in items:
        key = (item["brand"], item["model"])
        if key in PHOTOS:
            if item["brand"] not in cache:
                cache[item["brand"]] = listing_links(LISTINGS[item["brand"]])
                time.sleep(1.0)
            name = PHOTOS[key].rsplit("/", 1)[-1].lower()
            url = cache[item["brand"]].get(name)
            resolved[item["id"]] = url
        else:
            resolved[item["id"]] = item.get("gsmarena_url")

    result = {}
    missing = []
    for item in items:
        url = resolved.get(item["id"])
        label = f'{item["id"]} {item["brand"]} {item["model"]}'
        if not url:
            missing.append((label, "no url"))
            continue
        for attempt in (1, 2, 3):
            try:
                value = models_of(get(url))
                break
            except Exception as exc:  # noqa: BLE001
                if attempt == 3:
                    value = f"ERROR {exc}"
                else:
                    time.sleep(5)
        if value and not value.startswith("ERROR"):
            result[item["id"]] = {"url": url, "models": value}
            print(f"{label}: {value}")
        else:
            missing.append((label, value or "no Models row"))
            print(f"{label}: MISSING ({value})")
        time.sleep(1.0)

    OUT.write_text(
        json.dumps(
            {"source": "GSMArena product pages (data-spec models)",
             "fetched_at": "2026-11-18", "items": result},
            ensure_ascii=False, indent=1,
        ) + "\n",
        encoding="utf-8",
    )
    print(f"\nfetched={len(result)} missing={len(missing)} -> {OUT.relative_to(ROOT)}")
    for label, why in missing:
        print("MISSING:", label, "-", why)


if __name__ == "__main__":
    main()
