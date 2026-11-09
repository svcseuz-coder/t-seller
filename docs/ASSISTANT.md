# AI assistant — design and status

## Flow

1. The seller opens **AI assistant** from the home screen and asks a question in Russian, Uzbek or English.
2. `AssistantContextBuilder` (in `:core:data`) runs fully offline:
   - normalizes the question with `SearchText` (Cyrillic → Latin, apostrophes, aliases);
   - scores all 30 catalog models by name and spec matches;
   - applies intent ranking when no model is named (battery capacity, camera megapixels, wired charging, refresh rate);
   - picks at most four models and two technology-guide cards;
   - adds Carlcare support facts (hotline, email, centers, warranty summary) for service questions.
3. The app posts `{ question, language, context, history }` to the proxy. Only the compact context leaves the phone — no database, images or files.
4. The Cloudflare Worker (`server/assistant-proxy`) injects the provider key and calls an OpenAI-compatible endpoint with a strict system prompt: facts from the context only, “not verified” for gaps, no prices, no warranty decisions.
5. The answer is rendered as a chat bubble with the source model names under it. The last six messages are sent as history.

## Configuration

Build-time Gradle properties:

| Property | BuildConfig field | Purpose |
|---|---|---|
| `assistantProxyUrl` | `ASSISTANT_PROXY_URL` | Proxy URL. Empty → the screen shows “not connected” and disables input. |
| `assistantToken` | `ASSISTANT_APP_TOKEN` | `X-App-Token` header checked by the proxy. |

The provider API key exists only as the proxy secret `LLM_API_KEY`. It must never be added to Gradle properties or source.

## Live deployment (2026-11-06)

- Proxy: **https://t-seller-assistant.pages.dev** — Cloudflare Pages project `t-seller-assistant` with `_worker.js` (same code as `worker.js`).
- Provider: Gemini API free tier, model `gemini-3.1-flash-lite`, key stored as the Pages secret `LLM_API_KEY`.
- `wrangler.toml` `[vars]` and the `DEFAULTS` block in `worker.js` hold the endpoint, model and app token; redeploy with `cp worker.js dist/_worker.js && wrangler pages deploy dist --project-name t-seller-assistant`.
- `*.workers.dev` is blocked at the TLS level by the local ISP, so the Worker copy (`t-seller-assistant.svcseuz.workers.dev`) is not used; `*.pages.dev` is reachable.
- App build: `gradlew :app:assembleRelease -PassistantProxyUrl=https://t-seller-assistant.pages.dev -PassistantToken=<token>`, shipped as v1.8.0.
- Verified on-device (SM-S936B, 2026-11-06): RU answer with a source line (“Самую большую ёмкость… 8000 мА·ч”), EN answer via the IME send button, and the same flow in the installed release APK; screenshots `docs/screenshots/89…91`.
- Free-tier caveats: request-per-minute/day quotas apply and Google may change them; on the free tier the prompts may be used to improve Google's products. Only catalog context and the seller's question are sent.

## Failure states

- Not configured → intro notice and disabled input; suggestions are inert.
- Network/proxy error → red banner above the input with a retry hint; the previous answer stays on screen and the input stays usable.
- Empty context (no catalog match) → the model is instructed to say the data is not verified rather than invent an answer.

## Local testing without a deployed Worker

1. Run any HTTP endpoint that accepts the JSON body and returns `{"answer": "…"}`.
2. `adb reverse tcp:8790 tcp:8790` and build the debug variant with `-PassistantProxyUrl=http://127.0.0.1:8790/chat`.
3. The debug source set contains `network_security_config_debug.xml`, which permits cleartext to `127.0.0.1` / `localhost` only. The release build has no such allowance and requires HTTPS.

A Python mock used on 2026-11-06 confirmed the payload (`question`, `language`, context capped at 7 000 chars, last-six-message history) and verified the answer bubble, source line and error banner on a TECNO CL6.

## Privacy and limits

- No customer PII, no analytics, no logging of questions on the device.
- Context ≤ 7 000 chars, question ≤ 2 000 chars enforced on both client and Worker.
- The catalog, comparison, service and quiz keep working offline; only the assistant needs connectivity.
