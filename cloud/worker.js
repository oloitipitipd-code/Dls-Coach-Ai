/**
 * DLS Coach AI cloud gateway.
 *
 * Deploy as a Cloudflare Worker. Configure OPENAI_API_KEY as a Worker secret.
 * Never put the provider key in the Android APK.
 */
const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Content-Type": "application/json; charset=utf-8"
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: cors });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    if (request.method !== "POST") return json({ error: "POST required." }, 405);
    if (!env.OPENAI_API_KEY) return json({ error: "Cloud AI provider is not configured." }, 503);
    if (!env.APP_ACCESS_TOKEN) return json({ error: "Cloud AI access token is not configured." }, 503);
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
      "You are DLS Coach AI, an evidence-led Dream League Soccer 2026 coaching assistant.",
      "Use the supplied squad and match records as the user's available data. Do not invent players, ratings, positions, match events, balances, or game state.",
      "Independently recommend the best formation, starting XI, bench, tactical approach, squad development and transfer priorities from available evidence.",
      "Clearly label uncertainty and explain trade-offs. Do not guarantee wins.",
      "You are not connected to the user's DLS account and cannot read live inventory, alter lineups, purchase players, upgrade players, claim rewards, or control gameplay.",
      "Never claim an in-game action was completed. Follow the game's published rules; do not advise cheats, bots, hacks, reverse engineering, traffic interception, or unauthorised automation.",
      "If the roster is missing or incomplete, explain what is unknown and provide the best possible analysis from available data. Do not demand screenshots; use any existing records."
    ].join(" ");

    const upstream = await fetch(env.AI_API_URL || "https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${env.OPENAI_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: env.AI_MODEL || "gpt-4o-mini",
        temperature: 0.3,
        messages: [
          { role: "system", content: system + "\n\nCURRENT USER DATA (JSON):\n" + JSON.stringify(context).slice(0, 24000) },
          ...messages.slice(-20).map(m => ({
            role: m.role === "assistant" ? "assistant" : "user",
            content: String(m.content || "").slice(0, 6000)
          }))
        ]
      })
    });

    const result = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      return json({ error: result.error?.message || "AI provider request failed.", provider_status: upstream.status }, 502);
    }
    const answer = result.choices?.[0]?.message?.content;
    if (!answer) return json({ error: "The AI provider returned no answer." }, 502);
    return json({ answer, model: result.model || env.AI_MODEL || "configured-model" });
  }
};
