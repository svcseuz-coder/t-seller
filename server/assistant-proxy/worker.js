// T-Seller AI proxy. The API key lives only in a Worker secret; the APK never sees it.
const JSON_HEADERS = { "content-type": "application/json; charset=utf-8" };

const DEFAULTS = {
  LLM_BASE_URL: "https://generativelanguage.googleapis.com/v1beta/openai",
  LLM_MODEL: "gemini-3.1-flash-lite",
  APP_TOKEN: "33eb43d762d3b71467d4e96fd52b839e",
};

export default {
  async fetch(request, env) {
    const baseUrl = env.LLM_BASE_URL || DEFAULTS.LLM_BASE_URL;
    const model = env.LLM_MODEL || DEFAULTS.LLM_MODEL;
    const appToken = env.APP_TOKEN ?? DEFAULTS.APP_TOKEN;
    const apiKey = env.LLM_API_KEY;

    if (request.method === "OPTIONS") return new Response(null, { status: 204 });
    if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

    if (appToken && request.headers.get("x-app-token") !== appToken) {
      return json({ error: "unauthorized" }, 401);
    }
    if (!apiKey) {
      console.log("missing_llm_api_key");
      return json({ error: "llm_unavailable" }, 502);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "bad_json" }, 400);
    }

    const question = typeof body.question === "string" ? body.question.trim() : "";
    const context = typeof body.context === "string" ? body.context.trim() : "";
    const language = ["ru", "en", "uz"].includes(body.language) ? body.language : "en";
    if (!question || !context) return json({ error: "missing_fields" }, 400);
    if (question.length > 2000 || context.length > 12000) return json({ error: "too_large" }, 413);

    const history = Array.isArray(body.history)
      ? body.history
          .filter((m) => m && (m.role === "user" || m.role === "assistant") && typeof m.content === "string")
          .slice(-6)
          .map((m) => ({ role: m.role, content: m.content.slice(0, 2000) }))
      : [];

    const messages = [
      { role: "system", content: systemPrompt(language) },
      ...history,
      { role: "user", content: `CONTEXT:\n${context}\n\nQUESTION:\n${question}` },
    ];

    try {
      const response = await fetch(`${baseUrl.replace(/\/$/, "")}/chat/completions`, {
        method: "POST",
        headers: {
          "content-type": "application/json",
          authorization: `Bearer ${apiKey}`,
        },
        body: JSON.stringify({
          model,
          messages,
          temperature: 0.2,
          max_tokens: 600,
        }),
      });
      if (!response.ok) {
        console.log("llm_error", response.status);
        return json({ error: "llm_unavailable" }, 502);
      }
      const data = await response.json();
      const answer = data?.choices?.[0]?.message?.content?.trim();
      if (!answer) return json({ error: "empty_answer" }, 502);
      return json({ answer });
    } catch (error) {
      console.log("llm_exception", error.message);
      return json({ error: "llm_unavailable" }, 502);
    }
  },
};

function json(payload, status = 200) {
  return new Response(JSON.stringify(payload), { status, headers: JSON_HEADERS });
}

function systemPrompt(language) {
  const languageName = { ru: "Russian", en: "English", uz: "Uzbek" }[language];
  return `You are T-Seller, an in-store sales assistant for TECNO and Infinix phones in Uzbekistan.
Answer in ${languageName}.
Rules:
- Use ONLY facts from CONTEXT. Do not add outside knowledge, prices, promotions or availability.
- If CONTEXT does not contain the answer, say that the data is not verified in the app and suggest checking the catalog or Carlcare.
- Never invent or merge specifications. Mention exact model names.
- A warranty decision is made only after diagnostics at an authorized Carlcare center.
- Keep the answer to 3-6 short sentences, plain text, no markdown.
- Be practical: what to tell the customer.`;
}
