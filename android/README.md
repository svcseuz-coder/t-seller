# TECNO & Infinix Sales Assistant — Android app

Phase 2 foundation. Offline-first catalog built from the Phase 1 database; UI in English, Russian and Uzbek.

## Requirements

- JDK 17
- Android SDK: platform 35 (build-tools 34.0.0 auto-provisioned by AGP)
- Gradle 8.9 — use the bundled wrapper (`gradlew.bat`); a standalone install is not required

On this machine the portable toolchain lives at:

- JDK: `C:\Users\admin\android-tools\jdk17\jdk-17.0.21.0.2+1`
- SDK: `C:\Users\admin\android-sdk` (already referenced in `local.properties`)
- Gradle: `C:\Users\admin\android-tools\gradle-8.9`

## Build

```bat
set JAVA_HOME=C:\Users\admin\android-tools\jdk17\jdk-17.0.21.0.2+1
set ANDROID_HOME=C:\Users\admin\android-sdk
gradlew.bat test
gradlew.bat :app:assembleDebug
```

Unit tests live in `core/data/src/test` and validate the bundled seed bundles (30 models with the agreed 19/11 brand split, Hot 60i 4G/5G variants, 17 service centers with unique ids/codes and support contacts) and the cross-script search normalizer (city, model and brand spellings in Cyrillic and Latin, apostrophes, punctuation, token matching).

`local.properties` already points at the local SDK, so `ANDROID_HOME` is optional when it stays in place.

APK: `app/build/outputs/apk/debug/app-debug.apk` (~57 MB; debug build, not minified — the release build will shrink it).

Install on a connected phone with USB debugging:

```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n uz.carlcare.salesassistant/.MainActivity
```

## Structure

```
app/            Navigation, screens, view models, app theme host
core/data/      Room entities/DAO, seed importer, catalog repository, assets/products.json
core/design/    Material 3 theme, brand accents, shared components
```

Product data is imported from the bundled `products.json` into Room on first launch. Screens read only from Room; there is no hardcoded product content in UI code.

Service support data is bundled the same way in `core/data/src/main/assets/service_centers.json` (hotline, email, 17 service centers with address, phones, hours and coordinates, checked 2026-11-05) and seeded into the `service_centers` / `support_info` tables. Source: the official Carlcare Uzbekistan service-center page, which is also shown in the UI.

## Data rules

Every value originates from GSMArena (reference source, checked 2026-11-05). Missing values are absent from the database and must render as “Not verified” — never inferred. Infinix Hot 60i ships as one catalog entry with 4G and 5G variants (both sold in Uzbekistan).

## Verification status

- Build: `clean :app:assembleDebug` succeeds (Kotlin, KSP/Room, Compose, resources) with no compiler warnings.
- Tests: `gradlew test` — 11/11 JVM unit tests pass (`SeedAssetsTest`, `SearchTextTest`).
- APK contents: `assets/products.json` (30 models), `assets/service_centers.json` (17 centers), ru/uz resource folders, `minSdkVersion 29`, launcher `MainActivity`.
- Seed logic: simulated over the bundled JSON — 19 TECNO + 11 Infinix products, 1256 spec rows, Hot 60i carries a second 5G variant.
- On-device smoke test (Samsung SM-S936B, Android 17 / SDK 37, 1080×2340 @ 450 dpi, font scale 1.0): install and first-launch seeding OK (home shows 19 TECNO / 11 Infinix), GSMArena images load through Coil, live search works by model name and by spec keyword (`AMOLED` → 15 models), product page renders grouped localized specs, Hot 60i variant switch changes the displayed announced/released dates, and the uz locale renders correctly (per-app locale override). Screenshots: `docs/screenshots/01…12`.
- Service section (on-device, ru + uz): home shows the full-width «Сервисная поддержка» tile; the screen renders the hotline/email card, three quick links (wrapped onto two rows, all reachable), the 17 centers sorted by city with phones, hours, brands, route and call actions, and the source footer. Search filters by city and address and matches across scripts: the query is folded to lowercase Latin, apostrophes are dropped and Russian spellings of cities are aliased to the Uzbek ones (`Ташкент`/`Tashkent`/`Toshkent`, `Бухара`→`buxoro`, `Коканд`→`qoqon`…), so `Toshkent` returns 3 centers with their Russian-language addresses, and the plural reads «3 сервисных центра». The uz per-app locale renders the same screen with `Qo'ng'iroq`, `Du–Ju / Sha / Yak` and `17 ta servis markazi`. Screenshots: `docs/screenshots/13…18`. UZSI00061 «IP Mekhtiev Zaur» is catalogued in Toshkent (Sergeli district) — corrected on the user's report from Yangiyo'l, guarded by a seed test; screenshot `docs/screenshots/29`. Database version is 3 with destructive fallback, so assets are re-seeded after updating the app.
- Comparison (on-device, ru + uz): up to three models are picked from a searchable bottom sheet; slot cards show brand/model/variant and can be removed, the grouped spec table is built from Room for every selected device, differing values are tinted, the «Только различия» switch hides equal rows and empty groups, and missing values render as «Не подтверждено» / `Tasdiqlanmagan`. A model with several variants (Hot 60i) carries a 4G/5G switch in its slot that recomputes the table in place — switching Hot 60i from 4G to 5G changed announced/released dates, dimensions, weight and IP rating. Verified with Infinix Hot 60i + TECNO Camon 50 Pro 5G + TECNO Spark 50 5G in both locales. Screenshots: `docs/screenshots/19…22`.
- Cross-script search (catalog, comparison picker, service centers): one normalizer (`SearchText`) lowercases, transliterates Cyrillic, drops apostrophes and splits on punctuation, then requires every query token to appear somewhere in brand + series + model + variant labels + spec values. Russian model and brand spellings resolve to the catalog names through aliases (`Хот`→`hot`, `Спарк`→`spark`, `Камон`→`camon`, `Пова`→`pova`, `Ноте`→`note`, `Техно`/`Текно`→`tecno`, `Инфиникс`→`infinix`, `Эдж`→`edge`, `Гоу`→`go`, `Икс`→`x`). On-device checks with the Russian keyboard: `хот` → 7 models starting with Hot 60i (same as Latin `hot`), `спарк 50` → Spark 50 4G/5G/Pro/50c, the comparison picker returns the same Hot list for `хот`, and the service search still finds 3 Tashkent centers for `ташкент`. The catalog filter now runs in memory over the 30-product Room snapshot (about 1.3k spec rows), so the SQL `LIKE` statement was removed. Screenshots: `docs/screenshots/23…25`.
- Service tile artwork (on-device, ru/uz + dark theme): the home «Сервисная поддержка» tile paints the Carlcare banner (`res/drawable-nodpi/carlcare_service.jpg`, 1400 px wide, 86 KB) edge to edge with `ContentScale.Crop`, 82 % alpha and a surface-colored scrim fading left to right, so the title and subtitle stay legible over the photo. The same layout holds for the longer uz strings, and the scrim follows the dark color scheme when night mode is on. Screenshots: `docs/screenshots/26…28`.
- Spec-review workbook: `docs/TECNO_Infinix_характеристики_для_проверки.xlsx` mirrors comparison mode with products as columns — sheet `TECNO` (19 models) and sheet `Infinix` (11 models; Hot 60i split into 4G and 5G columns), rows grouped and ordered like the product screen with Russian labels, missing values as «Не подтверждено», frozen headers, A3 landscape fit-to-width, a per-model GSMArena source link and a «Комментарий специалиста» column. It is generated from `assets/products.json` by `docs/tools/build_specs_xlsx.py`, so re-run that script after spec corrections. Openpyxl-reserialised file verified to open in Excel without repair.
- Rebrand to T-Seller (2026-11-05): `app_name` is now T-Seller in all three locales (launcher label, home header, crash dialogs), the launcher icon is the user-supplied T-Seller artwork rebuilt as an adaptive icon (`drawable-nodpi/ic_launcher_foreground.png`, corners made transparent and art scaled to the 66 % safe zone over `#003FDF`), the home screen has an in-app language menu (top-right translate icon → English / Русский / O'zbek via `AppCompatDelegate.setApplicationLocales`, persisted per-app and honoured by the system language picker through `android:localeConfig`) and a centered footer «T-Seller · Developed by Carlcare Uzbekistan» in small English type. `MainActivity` now extends `AppCompatActivity`, so `Theme.SalesAssistant` must keep an AppCompat parent (`Theme.AppCompat.DayNight.NoActionBar`). Verified on-device: launcher icon/label, instant switch between all three languages, choice survives a force-stop, footer renders at the bottom. Screenshots: `docs/screenshots/30…36`. Release APK v1.0.0 is signed with the debug key so it installs on any device: `docs/T-Seller-v1.0.0.apk` (45 MB, copy of `app/build/outputs/apk/release/app-release.apk`). A Play-uploadable build needs a proper release keystore later.
- Technology guide (2026-11-05/06): 12 seller-facing cards ship as `assets/guide_cards.json` in `core:data` (no hardcoded card text in screens) in three locales — `ru`, `en` and `uz`; the screen picks the card language from `LocalConfiguration`, so the in-app language menu switches the guide instantly. Each card carries `title`, `what`, `watch`, `explain` (a ready-to-say quote without exaggerations) and three marked callouts: advantage / important / nuance, plus verified model examples from `products.json`. `GuideRepository` parses the asset once into a `StateFlow`; `GuideScreen` shows a numbered list and an in-place detail view («Что это? / На что смотреть? / Как объяснить покупателю?» + ✅⚠️💡 rows + examples). Russian master copy with the audit history: `docs/guide_cards_ru.md`. Release build v1.2.0 (versionCode 3, debug-key signed, installs as an update over 1.0.0/1.1.0) at `docs/T-Seller-v1.2.0.apk`; all three card locales verified inside the APK. On-device check of the RU/EN guide (TECNO CL6, Android 16, 1080×2436 @480 dpi) on 2026-11-06: update installed over v1.0.0, home tile opens the 12-card list, card detail renders all sections and ✅⚠️💡 callouts, examples intact, RU↔EN switch instant. Screenshots `docs/screenshots/37…46`. One defect found and fixed during the test: the quote-header book icon was clipped (`width(16.dp)` → `size(16.dp)`).
- UZ guide pass + back-navigation fix (2026-11-06): the Uzbek guide was verified end-to-end on the same TECNO CL6 — list order and short descriptions, card 1 (AMOLED/IPS) in full including the «Nimaga e'tibor berish kerak?» section, the «Xaridorga qanday tushuntirish kerak?» quote, the «Afzallik / Muhim / Nüans» callouts and the «Model namunalari» examples, and the IP card 11 with its caveat wording («IP68 bo'lsa ham suvda omon qolishni va'da qilmang…»); Uzbek apostrophes render correctly. Screenshots `docs/screenshots/47…50`. Testing exposed a navigation defect: the hardware/gesture back from a card detail popped the whole Guide destination instead of returning to the card list. `GuideScreen` now installs `BackHandler(enabled = selected != null) { selectedId = null }`, so system back matches the toolbar arrow (card → list → home); verified on-device. Fixed build v1.2.1 (versionCode 4, debug-key signed) at `docs/T-Seller-v1.2.1.apk`.
- Not verified: behaviour on Android 10–13 devices, tablets and large font scales; only one API 37 phone was available. The x86_64 emulator image is installed but the host CPU has VT-x disabled in UEFI, so it cannot boot.
