import json, os
from pathlib import Path
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.properties import PageSetupProperties

BASE = Path(__file__).resolve().parents[2]
ROOT = BASE / "android"
OUT = BASE / "docs" / "TECNO_Infinix_характеристики_для_проверки.xlsx"

data = json.load(open(os.path.join(ROOT, "core/data/src/main/assets/products.json"), encoding="utf-8"))
items = data["items"]

GROUPS = [
    ("release", "Общая информация"),
    ("body", "Корпус и габариты"),
    ("display", "Экран"),
    ("platform", "Процессор и производительность"),
    ("memory", "ОЗУ и память"),
    ("main_camera", "Основная камера"),
    ("selfie_camera", "Фронтальная камера"),
    ("battery", "Батарея и зарядка"),
    ("comms", "Сеть и подключения"),
    ("sound", "Звук"),
    ("features", "Безопасность и датчики"),
    ("software", "Операционная система"),
]

FIELD_ORDER = {
    "display": ["type", "size", "resolution", "refresh_rate", "brightness", "protection"],
    "platform": ["chipset", "cpu", "gpu", "os"],
    "memory": ["ram", "storage", "card_slot"],
    "main_camera": ["modules", "features", "video", "ois", "eis"],
    "selfie_camera": ["modules", "features", "video"],
    "battery": ["capacity", "type", "wired_charging", "wireless_charging", "reverse_charging", "bypass_charging"],
    "comms": ["network_5g", "nfc", "wlan", "bluetooth", "gps", "usb"],
    "features": ["fingerprint", "face_unlock", "sensors"],
    "body": ["dimensions", "weight", "build", "sim", "ip_rating", "colors"],
    "sound": ["speakers", "jack"],
    "software": ["android", "custom_ui", "updates"],
    "release": ["announced", "status"],
}

LABEL = {
    "announced": "Анонс", "status": "Статус", "dimensions": "Габариты", "weight": "Вес",
    "build": "Материалы", "sim": "SIM", "ip_rating": "Защита IP", "colors": "Цвета",
    "size": "Размер", "resolution": "Разрешение", "refresh_rate": "Частота обновления",
    "brightness": "Яркость", "protection": "Защита экрана", "os": "ОС", "chipset": "Чипсет",
    "cpu": "CPU", "gpu": "GPU", "ram": "ОЗУ", "storage": "Память", "card_slot": "Слот карты",
    "video": "Видео", "ois": "OIS", "eis": "EIS", "capacity": "Ёмкость",
    "wired_charging": "Проводная зарядка", "wireless_charging": "Беспроводная зарядка",
    "reverse_charging": "Реверсивная зарядка", "bypass_charging": "Bypass-зарядка",
    "network_5g": "5G", "nfc": "NFC", "wlan": "Wi-Fi", "bluetooth": "Bluetooth",
    "gps": "GPS", "usb": "USB", "sensors": "Датчики", "fingerprint": "Отпечаток пальца",
    "face_unlock": "Face unlock", "speakers": "Динамики", "jack": "Разъём 3,5 мм",
    "android": "Android", "custom_ui": "Оболочка", "updates": "Обновления",
}


def field_label(group, field):
    if field == "type":
        return "Тип батареи" if group == "battery" else "Тип экрана"
    if field == "features":
        return "Функции камеры"
    if field == "modules":
        return "Модули камеры"
    return LABEL.get(field, "Другое")


MISSING = "Не подтверждено"


def specmap(obj):
    out = {}
    for group, fields in (obj.get("specs") or {}).items():
        if not isinstance(fields, dict):
            continue
        for field, value in fields.items():
            if value is None or isinstance(value, (dict, list)):
                continue
            text = str(value).strip()
            if not text or text.lower() in ("null", "unspecified", "n/a", "—", "-"):
                continue
            out.setdefault(group, {})[field] = text
    return out


def columns(brand):
    result = []
    for item in items:
        if item["brand"] != brand:
            continue
        variants = [str(v) for v in (item.get("variant_labels") or [])]
        if not variants:
            result.append((item, None, specmap(item), False))
            continue
        for index, label in enumerate(variants):
            if index == 0:
                result.append((item, label, specmap(item), False))
            else:
                regional = next(
                    (r for r in (item.get("regional_variants") or []) if r.get("label") == label),
                    None,
                )
                result.append((item, label, specmap({"specs": (regional or {}).get("specs") or {}}), True))
    return result


def field_rows(group, cols):
    seen = []
    for _, _, specs, _ in cols:
        for field in specs.get(group) or {}:
            if field not in seen:
                seen.append(field)
    ordered = list(FIELD_ORDER.get(group, []))
    return ordered + [f for f in seen if f not in ordered]


BRAND = {"TECNO": ("00A8E8", "0077B6"), "Infinix": ("22C55E", "0E9F6E")}


def tint(hex6, amount=0.84):
    r, g, b = (int(hex6[i:i + 2], 16) for i in (0, 2, 4))
    mix = lambda c: round(c + (255 - c) * amount)
    return "%02X%02X%02X" % (mix(r), mix(g), mix(b))


thin = Side(style="thin", color="D9DEE6")
border = Border(left=thin, right=thin, top=thin, bottom=thin)
checked_at = data.get("source", {}).get("checked_at", "2026-11-05") if isinstance(data.get("source"), dict) else "2026-11-05"

wb = Workbook()
wb.remove(wb.active)

for brand in ("TECNO", "Infinix"):
    main, dark = BRAND[brand]
    ws = wb.create_sheet(brand)
    cols = columns(brand)
    first_model = 2
    comment_col = first_model + len(cols)
    last_model = comment_col - 1

    ws.cell(1, 1, "Характеристика")
    for idx, (item, variant, _, _) in enumerate(cols, start=first_model):
        title = item["model"]
        if variant:
            title = f"{title} · {variant}"
        ws.cell(1, idx, title)
    ws.cell(1, comment_col, "Комментарий специалиста")

    for idx, (item, variant, _, _) in enumerate(cols, start=first_model):
        sub = f'{item["series"]} · {item["year"]}'
        if variant:
            sub += f" · вариант {variant}"
        ws.cell(2, idx, sub)

    for c in range(1, comment_col + 1):
        top = ws.cell(1, c)
        top.font = Font(bold=True, size=11, color="FFFFFF")
        top.fill = PatternFill("solid", fgColor=main)
        top.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
        top.border = border

        sub = ws.cell(2, c)
        sub.fill = PatternFill("solid", fgColor="EEF2F6" if c in (1, comment_col) else tint(main))
        sub.font = Font(bold=True, size=9, color="4A5A6A" if c in (1, comment_col) else dark)
        sub.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
        sub.border = border

    ws.cell(1, 1).fill = PatternFill("solid", fgColor="101820")
    ws.cell(1, comment_col).fill = PatternFill("solid", fgColor="4A5A6A")
    ws.row_dimensions[1].height = 44
    ws.row_dimensions[2].height = 30

    row = 3
    value_font = Font(size=9, color="101820")
    missing_font = Font(italic=True, size=9, color="9AA6B2")

    for group, group_title in GROUPS:
        fields = field_rows(group, cols)
        if not fields:
            continue

        band = tint(main, 0.9)
        ws.merge_cells(start_row=row, start_column=1, end_row=row, end_column=last_model)
        heading = ws.cell(row, 1, group_title)
        heading.font = Font(bold=True, size=10, color=dark)
        heading.alignment = Alignment(vertical="center")
        for c in range(1, comment_col + 1):
            cell = ws.cell(row, c)
            cell.fill = PatternFill("solid", fgColor=band) if c <= last_model else PatternFill("solid", fgColor="EEF2F6")
            cell.border = border
        ws.row_dimensions[row].height = 20
        row += 1

        for field in fields:
            label = ws.cell(row, 1, field_label(group, field))
            label.font = Font(bold=True, size=10, color="101820")
            label.alignment = Alignment(vertical="top", wrap_text=True)
            label.fill = PatternFill("solid", fgColor="F7F9FB")
            label.border = border

            for idx, (_, _, specs, _) in enumerate(cols, start=first_model):
                value = (specs.get(group) or {}).get(field)
                cell = ws.cell(row, idx, value if value else MISSING)
                cell.font = value_font if value else missing_font
                cell.alignment = Alignment(vertical="top", wrap_text=True, horizontal="left")
                cell.border = border

            note = ws.cell(row, comment_col, "")
            note.alignment = Alignment(vertical="top", wrap_text=True)
            note.border = border
            row += 1

    link_font = Font(size=9, color="0B63CE", underline="single")
    source = ws.cell(row, 1, "Страница источника")
    source.font = Font(bold=True, size=9, color="4A5A6A")
    source.fill = PatternFill("solid", fgColor="F7F9FB")
    source.border = border
    for idx, (item, variant, _, is_regional) in enumerate(cols, start=first_model):
        url = item.get("gsmarena_url")
        if is_regional:
            regional = next(
                (r for r in (item.get("regional_variants") or []) if r.get("label") == variant),
                None,
            )
            url = (regional or {}).get("url") or url
        cell = ws.cell(row, idx, "GSMArena" if url else "—")
        cell.font = link_font if url else Font(size=9, color="9AA6B2")
        cell.alignment = Alignment(horizontal="left", vertical="center")
        cell.border = border
        if url:
            cell.hyperlink = url
    ws.cell(row, comment_col, "").border = border
    row += 1

    note_text = (
        f"Источник характеристик: GSMArena, сверено {checked_at}. "
        f"«{MISSING}» — значение отсутствует в проверенном источнике; уточните у вендора. "
        "Колонки соответствуют режиму сравнения приложения; варианты 4G/5G показаны отдельными колонками."
    )
    ws.merge_cells(start_row=row, start_column=1, end_row=row, end_column=comment_col)
    footer = ws.cell(row, 1, note_text)
    footer.font = Font(size=8, italic=True, color="4A5A6A")
    footer.alignment = Alignment(vertical="top", wrap_text=True)
    ws.row_dimensions[row].height = 34

    ws.column_dimensions["A"].width = 30
    for c in range(first_model, comment_col):
        ws.column_dimensions[get_column_letter(c)].width = 25
    ws.column_dimensions[get_column_letter(comment_col)].width = 32
    ws.freeze_panes = "B3"
    ws.sheet_properties.tabColor = main
    ws.sheet_view.zoomScale = 80
    ws.page_setup.orientation = "landscape"
    ws.page_setup.paperSize = ws.PAPERSIZE_A3
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 0
    ws.sheet_properties.pageSetUpPr = PageSetupProperties(fitToPage=True)
    ws.print_title_rows = "1:2"
    ws.print_title_cols = "A:A"
    ws.page_margins.left = 0.3
    ws.page_margins.right = 0.3
    ws.page_margins.top = 0.4
    ws.page_margins.bottom = 0.4

wb.save(OUT)
print("saved", OUT)

from openpyxl import load_workbook

check = load_workbook(OUT)
for sheet in check.worksheets:
    print(sheet.title, "rows", sheet.max_row, "cols", sheet.max_column, "freeze", sheet.freeze_panes)
    print("  header:", [sheet.cell(1, c).value for c in range(1, sheet.max_column + 1)])
