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

The Edge Function requires a valid Supabase user JWT, enforces a database-backed limit of 10 AI requests per user per UTC day, and reads the Gemini key only on the server. AI provider quota and pricing depend on the account and selected model; verify current terms before use. The endpoint receives only the question plus the entered squad and match records supplied by the client. It does not capture DLS gameplay.


## Independent offline coaching engine

The AI Coach tab includes a built-in deterministic, rules-based analysis engine in `src/lib/localCoach.ts`. It runs entirely in the browser against the user's locally stored squad and match records. It does not call Gemini, ChatGPT, Supabase, or any network service. It reports squad depth, formation role coverage, and descriptive trends from the latest five logged results. It explicitly labels missing data and does not claim to predict wins or observe live gameplay.

The optional cloud AI action remains separate. It requires Supabase sign-in, a deployed Edge Function, and a server-side Gemini key. The app's core offline coaching report does not require those services. This offline engine is a specialised decision-support rules engine, not a locally trained foundation language model.

## Offline backup and restore

The Settings page can export the locally stored squad and match history as a versioned JSON backup. Restore validates the schema, record limits, IDs, positions, ratings, dates and goal counts before replacing local records. Restore requires explicit confirmation because it replaces the current local squad and match history. The backup contains only these records; it does not include Supabase credentials or private AI keys.

## Offline coaching behaviour

The built-in coach uses maximum-cardinality role matching across formation slots, preferring exact positions and higher ratings while avoiding avoidable role conflicts. Match summaries sort records by date before selecting the latest five. These are explainable heuristics, not a trained model or a win predictor.
