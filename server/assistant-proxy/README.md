# T-Seller AI proxy (Cloudflare Worker / Pages)

The app sends only a compact context built from the verified catalog plus the seller's question. The Worker adds the provider API key and forwards the request to an OpenAI-compatible `/chat/completions` endpoint. The key is stored as a Worker secret and never ships in the APK.

## Deployed instance (2026-11-06)

- URL: `https://t-seller-assistant.pages.dev` (Cloudflare Pages + `_worker.js`)
- Provider: Gemini API free tier, model `gemini-3.1-flash-lite`
- `LLM_API_KEY` is a Pages secret on project `t-seller-assistant`
- `*.workers.dev` is blocked at the TLS level by the local ISP, so the same code is served from the Pages project; the Worker deployment `t-seller-assistant.svcseuz.workers.dev` exists but is unreachable here.

Redeploy after editing `worker.js`:

```bash
mkdir -p dist && cp worker.js dist/_worker.js
npx wrangler pages deploy dist --project-name t-seller-assistant --branch main
```

Update the key without touching the APK:

```bash
printf '%s' "<new key>" | npx wrangler pages secret put LLM_API_KEY --project-name t-seller-assistant
```

## Deploy from scratch

```bash
npm install -g wrangler
wrangler login
cd server/assistant-proxy
wrangler pages project create t-seller-assistant --production-branch main
printf '%s' "<provider key>" | wrangler pages secret put LLM_API_KEY --project-name t-seller-assistant
wrangler deploy                      # optional Worker variant (workers.dev)
```

The Worker default constants in `worker.js` (`DEFAULTS`) hold a Gemini endpoint, model and the app token, so a Pages deployment needs only the `LLM_API_KEY` secret. Set `LLM_BASE_URL` / `LLM_MODEL` in `wrangler.toml` (Worker) or as Pages environment variables to switch providers (OpenAI, DeepSeek, Groq, OpenRouter and Gemini's OpenAI-compatible endpoint all work). The endpoint must accept `POST /chat/completions` with `{ model, messages, temperature, max_tokens }`.

Optional shared token: put a random string in `APP_TOKEN` (`wrangler deploy` with the var set) and build the app with the same `-PassistantToken=...`. The token is embedded in the APK, so treat it as a speed bump, not a secret; enable Cloudflare rate limiting for real protection.

## Wire the app

```bash
gradlew :app:assembleRelease \
  -PassistantProxyUrl=https://t-seller-assistant.<subdomain>.workers.dev \
  -PassistantToken=<token-if-used>
```

Without `assistantProxyUrl` the assistant screen shows a clear “not connected” state; the rest of the app is unaffected.

## Contract

Request:

```json
{
  "question": "Camon 50 Pro 5G или Spark 50 4G — что лучше?",
  "language": "ru",
  "context": "MODEL: ...\n- battery: capacity=5200 mAh; ...",
  "history": [{ "role": "user", "content": "..." }]
}
```

Response: `{ "answer": "..." }`. Errors: `{ "error": "llm_unavailable" }` with a non-2xx status.

## Guardrails

- Context and question sizes are capped (12 000 / 2 000 chars) and history is limited to the last six messages.
- The system prompt forbids facts outside the context, invented specs, prices and warranty decisions.
- No customer data is sent: the app never collects names, phone numbers or IMEI.
- Rotate `LLM_API_KEY` in the Worker if it leaks; no app rebuild is needed for that.
