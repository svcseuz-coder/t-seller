"""Build core/data assets/post_branches.json from the EMU Express branch workbook.

Usage: python docs/tools/build_post_branches.py "C:/FILES/emu-branches-*.xlsx"
The workbook is produced by the EMU Express branch export (sheet Filiallar);
the script validates the rows and keeps the values verbatim (no invented data).
"""

import json
import re
import sys
from pathlib import Path

import openpyxl

OUT = Path(__file__).resolve().parents[2] / "android/core/data/src/main/assets/post_branches.json"


def main(xlsx_path: str) -> None:
    wb = openpyxl.load_workbook(xlsx_path, data_only=True)
    ws = wb["Filiallar"]
    header = [c for c in next(ws.iter_rows(min_row=1, max_row=5, values_only=True)) if c]
    checked_at = next(
        (str(r[0]) for r in ws.iter_rows(min_row=1, max_row=3, values_only=True)
         if r[0] and "olingan vaqt" in str(r[0])),
        "",
    )
    rows = [r for r in ws.iter_rows(min_row=6, values_only=True) if r[0] not in (None, "")]

    branches = []
    ids, names = set(), set()
    for r in rows:
        number, name, region, city, address, phones, hours, _, _, _, lat, lng, _ = r
        number = int(number)
        assert number not in ids, f"duplicate id {number}"
        ids.add(number)
        assert name not in names, f"duplicate name {name}"
        names.add(name)
        phone_list = [re.sub(r"\D", "", p) for p in re.split(r"[,;\s]+", str(phones or ""))]
        phone_list = [p for p in phone_list if p]
        assert phone_list, f"no phone for {name}"
        for p in phone_list:
            if len(p) not in (9, 12):
                print(f"warning: {name}: phone '{p}' has {len(p)} digits (kept as is)", file=sys.stderr)
        assert 37 <= float(lat) <= 46 and 55 <= float(lng) <= 74, f"bad coords for {name}"
        branches.append({
            "id": number,
            "name": str(name).strip(),
            "region": str(region).strip(),
            "city": str(city).strip(),
            "address": str(address).strip(),
            "phones": phone_list,
            "hours": str(hours).strip(),
            "lat": round(float(lat), 6),
            "lng": round(float(lng), 6),
        })

    payload = {
        "checkedAt": "2026-11-16",
        "source": "EMU Express filiallar ro'yxati (16.11.2026, 12:54, Asia/Tashkent)",
        "exportNote": checked_at,
        "branches": branches,
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{len(branches)} branches -> {OUT}")
    print("regions:", len({b['region'] for b in branches}), "cities:", len({b['city'] for b in branches}))


if __name__ == "__main__":
    main(sys.argv[1])
