# DLS Coach AI — free-tier Gemini cloud gateway

This is the server-side gateway for the Android app's internet-connected AI manager. It uses Google's Gemini Developer API free tier where the selected model and account are eligible. The Android APK never contains the Gemini API key.

## Set up the free AI provider
1. Open [Google AI Studio](https://aistudio.google.com/).
2. Sign in with your Google account and create an API key in a project on the **Free** billing tier. Do not attach billing or upgrade the project.
3. Check the model's availability and current request/token quotas in AI Studio. The default model is `gemini-3.8-flash`; if it is not enabled on your free-tier project, set `GEMINI_MODEL` to a model that AI Studio explicitly lists as available for your free tier.
4. Copy the API key. Never paste it into the Android app, GitHub, or a public repository.

## Deploy the personal gateway
1. Create a free Cloudflare account and a Worker.
2. Paste `worker.js` into the Worker editor and deploy it.
3. In Worker Settings → Variables and Secrets, add these secrets:
   - `GEMINI_API_KEY`: the key created in Google AI Studio.
   - `APP_ACCESS_TOKEN`: a long random private token used to protect your personal gateway.
4. Optionally add `GEMINI_MODEL` as a variable if you need a different model enabled on your free tier.
5. Copy the Worker HTTPS URL.
6. In DLS Coach AI → More → AI Chat, enter the Worker URL and the same `APP_ACCESS_TOKEN`. Accept the data-sharing consent before sending a prompt.

## Cost, limits and privacy
- The intended setup is **$0**: Gemini free-tier model use within the account's quota, plus Cloudflare Workers Free within its limits. Free quotas can change, vary by model/account/region and be temporarily unavailable. The app must stop with a clear quota error rather than silently switch to a paid tier.
- Do not attach a billing account or add paid credits. This project does not configure automatic paid fallback.
- Google's published Gemini free-tier terms state that submitted content may be used to improve Google products. Do not send private personal information. Review the current provider terms before use.
- The provider receives the conversation and saved squad/match context when you send a message. The API key stays on the server, not in the APK.
- Cloudflare Worker Free has its own request limits. The Worker is a relay, not an AI model host.
- This is not an official First Touch Games integration and cannot access or modify the DLS account/game.
- The example CORS policy is permissive for a personal prototype. The bearer token provides a basic access gate; do not expose it publicly or reuse it as a password.
