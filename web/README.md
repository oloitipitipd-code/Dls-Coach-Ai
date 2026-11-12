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
