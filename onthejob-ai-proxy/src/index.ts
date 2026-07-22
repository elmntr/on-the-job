interface Env {
  GEMINI_API_KEY: string;
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    // Only accept POST requests
    if (request.method !== "POST") {
      return new Response("Method not allowed", { status: 405 });
    }

    // CORS preflight (Android app calls this directly, but keep it safe if you ever test from a browser)
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type",
        },
      });
    }

    let rawText: string;
    try {
      const body = await request.json() as { rawText?: string };
      rawText = body.rawText ?? "";
      if (!rawText.trim()) {
        return Response.json({ success: false, reason: "failed_other" }, { status: 400 });
      }
   } catch (err) {
         console.error("Gemini proxy error:", err);
         return Response.json({ success: false, reason: "failed_other", debug: String(err) });
       }

    try {
      const geminiRes = await fetch(
       `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=${env.GEMINI_API_KEY}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [
              {
                parts: [
                  {
                    text: `Rewrite this OJT (On-the-Job Training) daily log into exactly ONE polished, professional narrative entry, written in first person past tense. Output ONLY the rewritten entry itself — no options, no headers, no markdown formatting, no explanations, no preamble. Do not invent details that weren't mentioned in the raw description.\n\nRaw description: ${rawText}`,
                  },
                ],
              },
            ],
          }),
        }
      );

      // Rate limit or quota exhaustion
      if (geminiRes.status === 429) {
        const errBody = await geminiRes.json().catch(() => ({}));
        const isQuotaExhausted = JSON.stringify(errBody).includes("RESOURCE_EXHAUSTED");
        return Response.json({
          success: false,
          reason: isQuotaExhausted ? "quota_exhausted" : "rate_limited",
        });
      }

      if (!geminiRes.ok) {
              const errText = await geminiRes.text();
              console.error("Gemini API error:", geminiRes.status, errText);
              return Response.json({ success: false, reason: "failed_other", debug: errText });
      }

      const data = await geminiRes.json() as any;
      const formattedText: string =
        data?.candidates?.[0]?.content?.parts?.[0]?.text ?? "";

      if (!formattedText.trim()) {
        return Response.json({ success: false, reason: "failed_other" });
      }

      return Response.json({ success: true, formattedText });
    } catch (err) {
      return Response.json({ success: false, reason: "failed_other" });
    }
  },
} satisfies ExportedHandler<Env>;
