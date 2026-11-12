/**
 * DLS Coach AI personal cloud gateway.
 * Gemini API key stays in this server-side Worker, never in the Android APK.
 * Default model: gemini-3.8-flash (select a model enabled on the account's free tier).
 */
const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Content-Type": "application/json; charset=utf-8"
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: cors });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    if (request.method !== "POST") return json({ error: "POST required." }, 405);
    if (!env.GEMINI_API_KEY) return json({ error: "Free Gemini API key is not configured on the gateway." }, 503);
    if (!env.APP_ACCESS_TOKEN) return json({ error: "Personal app access token is not configured." }, 503);
    if (request.headers.get("Authorization") !== `Bearer ${env.APP_ACCESS_TOKEN}`) {
      return json({ error: "Unauthorised. Check the app access token." }, 401);
    }

    let body;
    try { body = await request.json(); }
    catch { return json({ error: "Invalid JSON request." }, 400); }

    const messages = Array.isArray(body.messages) ? body.messages : [];
    if (messages.length === 0 || messages.length > 30) return json({ error: "Send 1–30 chat messages." }, 400);

    const context = body.context && typeof body.context === "object" ? body.context : {};
    const system = [
      "You are DLS Coach AI, an independent, evidence-led Dream League Soccer 2026 team manager and coach.",
      "Use supplied squad and match records as the only known user data. Never invent players, ratings, positions, match events, balances or game state.",
      "Independently select the strongest formation, starting XI, bench and tactical approach from available evidence. Explain trade-offs and confidence.",
      "Analyse recorded results, identify recurring weaknesses, and create practical training, development and transfer priorities.",
      "Do not guarantee wins or claim certainty where player data is incomplete.",
      "You are not connected to the user's DLS account. You cannot read live inventory, change the in-game lineup, purchase/sign/release players, upgrade players, claim rewards or control gameplay.",
      "Never claim an in-game action was completed. Do not advise cheats, bots, hacks, reverse engineering, traffic interception or unauthorised automation.",
      "If roster data is incomplete, say exactly what is missing and still give the best provisional analysis. Do not require screenshots; use records already available."
    ].join(" ");

    const contents = messages.slice(-20).map(m => ({
      role: m.role === "assistant" ? "model" : "user",
      parts: [{ text: String(m.content || "").slice(0, 6000) }]
    }));
    if (contents[0]?.role === "model") contents.shift();
    if (contents.length === 0) return json({ error: "At least one user message is required." }, 400);

    const model = env.GEMINI_MODEL || "gemini-3.8-flash";
    const upstream = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`, {
      method: "POST",
      headers: {
        "x-goog-api-key": env.GEMINI_API_KEY,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        systemInstruction: {
          parts: [{ text: system + "\n\nCURRENT USER DATA (JSON):\n" + JSON.stringify(context).slice(0, 24000) }]
        },
        contents,
        generationConfig: { temperature: 0.3, maxOutputTokens: 1800 }
      })
    });

    const result = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      const message = result.error?.message || "Gemini request failed.";
      return json({ error: message, provider_status: upstream.status }, upstream.status === 429 ? 429 : 502);
    }
    const answer = (result.candidates?.[0]?.content?.parts || [])
      .map(part => part.text || "").join("\n").trim();
    if (!answer) return json({ error: "Gemini returned no text. The response may have been blocked or truncated." }, 502);
    return json({ answer, model });
  }
};
