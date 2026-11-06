# TECNO & Infinix Sales Assistant — Proposed Architecture

Status: Phases 1–2 delivered (catalog data + Android foundation) and smoke-tested on hardware (Samsung SM-S936B, Android 17; ru/uz). Phases 3–7 pending.

## Platform and stack

- Kotlin 2.0.21, Jetpack Compose (Material 3, BOM 2024.10.01), minSdk 29 (Android 10), compileSdk/targetSdk 35.
- MVVM + unidirectional data flow; manual DI through `AppContainer` (Hilt deferred until feature modules exist); Navigation Compose.
- Room (SQLite) as the single local source of truth; `org.json` parsing of the bundled seed bundle; shared preferences/DataStore for settings in a later phase.
- Retrofit/OkHttp for sync and AI proxy; WorkManager for background updates; Coil for images; Paging 3 for catalog lists.
- CSV/Excel: import/export through the Storage Access Framework (Apache POI for XLSX on a background worker).

## Module layout

```
:app                navigation, screens, view models, AppContainer wiring
:core:design        theme, typography, brand accents (TECNO cyan / Infinix green), components
:core:data          Room entities/DAO, seed importer, catalog repository, assets/products.json
:core:network       sync client, AI proxy client, manifest/etag handling        (Phase 5–6)
:core:model         spec field metadata (source, status, date, region)         (Phase 5)
:feature:catalog    brand/series browsing, filters, smart search               (Phase 3)
:feature:details    product page, 3-level explanations                        (Phase 3)
:feature:compare    up to 3 devices, diff highlighting, share/export          (Phase 4)
:feature:guide      technology knowledge base                                  (Phase 4)
:feature:assistant  AI Q&A and recommendations                                 (Phase 6)
:feature:quiz       training module                                            (Phase 6)
:feature:admin      CRUD, import/export, review/approve workflow               (Phase 5)
```

## Data model (core tables)

- `products` — id, brand, series, model, year, status (active/archived), image URLs, region scope.
- `spec_values` — product_id, group, field, value, unit, source, verification_status, last_updated, region_variant.
- `explanations` — spec/technology key, locale, level 1 (technical), level 2 (simple), level 3 (customer pitch), approval status.
- `technologies` — name, definition, benefits, related models.
- `quiz_questions` — generated from verified specs, locale, options, answer, explanation.
- `prices` — product_id, region, amount, currency, source, verified_at (recommendations that use price require this row).
- `sync_meta` — bundle version, etag, applied_at, rollback snapshot id.
- `service_centers` — id, code, name, city, address, phones, brands, coordinates, weekday/saturday/sunday hours; seeded from `assets/service_centers.json` (source: carlcare.com/uz/service-center, checked 2026-11-05).
- `support_info` — hotline, dial number, email, source URL, checked date (single row).

Field-level metadata is mandatory: every spec value carries source, verification status (`gsmarena_reference`, `official`, `manual`, `unverified`), date and region variant. The UI renders null as “Not verified” — never a guessed value.

## Offline-first and updates

- The APK ships a seed database built from the Phase 1 JSON (no product data hardcoded in screens).
- Room is authoritative at runtime; sync fetches a signed/versioned JSON bundle + image manifest, validates, then applies in one transaction. A failed or partial download leaves local data untouched; previous bundle is retained for rollback.
- Delta updates by `updated_at`/bundle version; images cached by content hash. No APK rebuild needed for data updates.

## AI assistant (modular, grounded)

- `AiProvider` interface; the app talks to a backend proxy (Cloudflare Worker or equivalent), so API keys never ship in the APK.
- Retrieval-first pipeline: intent detection → local SQL/ORM query (catalog, specs, guide) → compact grounded context → LLM with a strict system prompt: use only supplied facts, mark missing data “Not verified”, no invented specs, no merging regional variants.
- Offline/without AI: rule-based search, comparison and recommendation engine continue to work; a clear “AI unavailable” state is shown.
- Languages: en / ru / uz for UI and answers; product content stored per locale with English fallback.

## Recommendation engine

Deterministic scoring over verified specs (camera, battery, charging, display, 5G, NFC, gaming proxies such as chipset tier/refresh rate). Each recommendation returns the exact fields that triggered it. Price criteria are enabled only when verified local prices exist in the `prices` table.

## Comparison and sharing

Up to three products, sectioned table, differences highlighted, missing values shown explicitly. Share as formatted text/image via Android sharesheet; export to CSV.

## Admin and data quality

Hidden admin entry (long-press on About/version). CRUD for models, specs, technologies, explanations and FAQ; CSV/XLSX import with validation, duplicate detection and dry-run preview; export; review queue for unverified fields and AI-generated explanations; image upload; automatic backup/restore of the Room file.

## Security

No customer PII; keys only on the backend; HTTPS only; optional SQLCipher for admin-edited data; error log ring buffer exportable by the admin; local backup/restore.

## Testing and delivery

Unit tests for repositories/search/recommendation, Room migration tests, Compose UI tests for catalog/compare/quiz, instrumentation tests for offline mode and import. Deliverables per spec: APK, source, DB files, build instructions, user guide.
