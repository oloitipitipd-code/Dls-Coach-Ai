# DLS Ultimate AI — Web companion

## Run locally
Requires Node.js 20 or newer.
```powershell
cd web
npm install
npm run dev
```
Open the local URL printed by Vite.

## Optional Supabase setup
1. Create a Supabase project.
2. Run `supabase/migrations/0001_dls_core.sql` in the Supabase SQL Editor.
3. Copy `web/.env.example` to `web/.env.local`.
4. Set `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` using the project's public values.
5. Restart Vite. Open Settings and request an email sign-in link.
6. Select **Sync data** to upload and retrieve the signed-in user's squad and match records.

Only the public anon key belongs in browser configuration. Never expose a service-role key or private AI-provider key. Row-level security policies restrict rows to the authenticated owner.

## Scope and limitations
The web companion currently provides local squad entry, formation-fit heuristics, match logging and optional Supabase authentication/synchronisation. It does not connect to DLS gameplay, recognise live match events, automate inputs, or make purchases. Android and browser local storage are separate until the same Supabase project is configured and the user signs in and syncs.

## Optional AI Coach setup
1. Create a Google AI Studio API key. Do not paste it into the web app or commit it to Git.
2. Set the Supabase Edge Function secret `GEMINI_API_KEY` in the Supabase Dashboard.
3. Optionally set `GEMINI_MODEL` to a model available to your Google AI Studio project.
4. Deploy the function from the repository root with the Supabase CLI: `supabase functions deploy football-coach`.
5. Sign in from the web app Settings screen, then open AI Coach.

The Edge Function requires a valid Supabase user JWT and reads the Gemini key only on the server. AI provider quota and pricing depend on the account and selected model; verify current terms before use. The endpoint receives only the question plus the entered squad and match records supplied by the client. It does not capture DLS gameplay.
