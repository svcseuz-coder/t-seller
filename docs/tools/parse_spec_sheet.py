"""Parse the TECNO/Infinix/ITEL spec workbook into product dicts (shared by diff + build)."""
import re
import openpyxl

ROW_MAP = {
    "release": {"announced": 4, "status": 5},
    "body": {"dimensions": 7, "weight": 8, "build": 9, "sim": 10, "ip_rating": 11, "colors": 12},
    "display": {"type": 14, "size": 15, "resolution": 16, "refresh_rate": 17, "brightness": 18, "protection": 19},
    "platform": {"chipset": 21, "cpu": 22, "gpu": 23, "os": 24},
    "memory": {"ram": 26, "storage": 27, "card_slot": 28},
    "main_camera": {"modules": 30, "features": 31, "video": 32, "ois": 33, "eis": 34},
    "selfie_camera": {"modules": 36, "features": 37, "video": 38},
    "battery": {"capacity": 40, "type": 41, "wired_charging": 42, "wireless_charging": 43,
                "reverse_charging": 44, "bypass_charging": 45},
    "comms": {"network_5g": 47, "nfc": 48, "wlan": 49, "bluetooth": 50, "gps": 51, "usb": 52},
    "sound": {"speakers": 54, "jack": 55},
    "features": {"fingerprint": 57, "face_unlock": 58, "sensors": 59},
    "software": {"android": 61, "custom_ui": 62, "updates": 63},
}
MISSING = {"не подтверждено", "not verified", "unspecified", "-", "—", ""}


def cell_text(value):
    if value is None:
        return None
    if isinstance(value, float) and value.is_integer():
        value = int(value)
    text = str(value).replace("\u00a0", " ").strip()
    if text.lower() in MISSING:
        return None
    return re.sub(r"\s{2,}", " ", text)


def read_specs(ws, col):
    specs = {}
    for group, fields in ROW_MAP.items():
        block = {}
        for key, row in fields.items():
            block[key] = cell_text(ws.cell(row, col).value)
        specs[group] = block
    return specs


def read_model(ws, col):
    name = cell_text(ws.cell(1, col).value)
    if not name or name == "Комментарий специалиста":
        return None
    series_cell = cell_text(ws.cell(2, col).value) or ""
    parts = [p.strip() for p in series_cell.split("·")]
    series = parts[0].upper() if parts and parts[0] else ""
    year = None
    for part in parts[1:]:
        match = re.fullmatch(r"(\d{4})", part)
        if match:
            year = int(match.group(1))
    variant = None
    match = re.search(r"вариант\s*(4G|5G)", series_cell, re.IGNORECASE)
    if match:
        variant = match.group(1).upper()
    if year is None:
        announced = cell_text(ws.cell(4, col).value) or ""
        match = re.search(r"(20\d{2})", announced)
        if match:
            year = int(match.group(1))
    return {
        "name": name,
        "series": series,
        "year": year,
        "variant": variant,
        "specs": read_specs(ws, col),
    }


def read_sheet(wb, sheet):
    ws = wb[sheet]
    models = []
    for col in range(2, ws.max_column + 1):
        model = read_model(ws, col)
        if model:
            models.append(model)
    return models
