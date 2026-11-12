# DLS Coach AI — Cloud AI gateway

This folder contains the server-side gateway for the Android app's internet-connected AI chat.

## Why a gateway is required
The Android app must never contain a provider API key. The Worker stores the key as a server secret and forwards limited chat context to an OpenAI-compatible chat-completions endpoint. The app sends only the roster/match records already stored in the app and the conversation messages.

## Deploy (Cloudflare Workers)
1. Create a Cloudflare account and a Worker.
2. Paste `worker.js` into the Worker editor and deploy it.
3. In Worker Settings → Variables and Secrets, add the secret `OPENAI_API_KEY`.
4. Optionally add `AI_MODEL` and `AI_API_URL` as variables. Defaults are `gpt-4o-mini` and the OpenAI chat-completions endpoint.
5. Copy the deployed HTTPS Worker URL.
6. In DLS Coach AI, open More → AI Chat and paste the Worker URL into Cloud AI endpoint.

## Cost and privacy
- This gateway does not include a free AI subscription. Provider API usage may be billable; check the provider's current pricing and limits before adding a key.
- The provider receives the chat messages and the app context (player names, positions, ratings, attributes, match records and formation/style). Do not connect the service unless you consent to that data transfer.
- The provider key stays on the server and is not stored in the APK.
- This service is not an official First Touch Games integration and cannot access or modify the DLS account/game.
- The sample CORS policy is permissive for a personal prototype. Add authentication/rate limiting before public deployment.
