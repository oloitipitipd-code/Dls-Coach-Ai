const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return Response.json({ error: "Method not allowed" }, { status: 405, headers: corsHeaders });

  // Supabase's default verify_jwt=true protects this function before the handler runs.
  if (!req.headers.get("Authorization")?.startsWith("Bearer ")) {
    return Response.json({ error: "Sign in is required." }, { status: 401, headers: corsHeaders });
  }

  let payload: { question?: unknown; squad?: unknown; matches?: unknown };
  try {
    const raw = await req.text();
    if (raw.length > 16000) return Response.json({ error: "Request is too large." }, { status: 413, headers: corsHeaders });
    payload = JSON.parse(raw);
  } catch {
    return Response.json({ error: "Invalid JSON request." }, { status: 400, headers: corsHeaders });
  }

  if (typeof payload.question !== "string" || payload.question.trim().length < 3 || payload.question.length > 2000) {
    return Response.json({ error: "Question must contain 3–2000 characters." }, { status: 400, headers: corsHeaders });
  }

  const apiKey = Deno.env.get("GEMINI_API_KEY");
  const model = Deno.env.get("GEMINI_MODEL") ?? "gemini-2.5-flash";
  if (!apiKey) return Response.json({ error: "AI provider is not configured on the server." }, { status: 503, headers: corsHeaders });

  const safeContext = JSON.stringify({
    squad: Array.isArray(payload.squad) ? payload.squad.slice(0, 40) : [],
    recentMatches: Array.isArray(payload.matches) ? payload.matches.slice(0, 10) : [],
  }).slice(0, 10000);

  const prompt = [
    "User question:",
    payload.question.trim(),
    "",
    "User-provided club context (unverified; do not invent missing data):",
    safeContext,
    "",
    "Give practical, concise DLS 26 football-management advice. Separate observed/user-entered facts from assumptions. If player attributes, opponent information, or match events are missing, say so. Never claim you can observe or control the game. Never recommend bypassing anti-cheat, reverse engineering, private endpoints, game-memory access, or unauthorised automation. Purchases, releases, and premium-currency spending always require explicit human approval.",
  ].join("\n");

  try {
    const response = await fetch("https://generativelanguage.googleapis.com/v1beta/models/" + encodeURIComponent(model) + ":generateContent?key=" + encodeURIComponent(apiKey), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: "You are DLS Ultimate AI, an evidence-led football squad and tactics analyst. Never fabricate DLS mechanics, player statistics, opponent data, or test results." }] },
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.25, maxOutputTokens: 900 },
      }),
    });
    const result = await response.json();
    if (!response.ok) {
      return Response.json({ error: "AI provider request failed.", providerStatus: response.status }, { status: 502, headers: corsHeaders });
    }
    const answer = result?.candidates?.[0]?.content?.parts?.map((part: { text?: string }) => part.text ?? "").join("").trim();
    if (!answer) return Response.json({ error: "The AI provider returned no text." }, { status: 502, headers: corsHeaders });
    return Response.json({ answer, model }, { headers: corsHeaders });
  } catch {
    return Response.json({ error: "Could not reach the AI provider." }, { status: 502, headers: corsHeaders });
  }
});
