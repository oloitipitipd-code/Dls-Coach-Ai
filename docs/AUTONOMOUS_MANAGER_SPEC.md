# DLS Coach AI — Autonomous Manager Specification

## Product goal
DLS Coach AI is an English-only Android companion that manages the user's DLS 26 squad information and coaching workflow. It analyses evidence supplied by the user and prepares recommendations. It does not modify the game, control matches, spend coins/gems, buy players, or guarantee wins.

## Operating principles
- Evidence first: never invent player names, positions, ratings, attributes, match events, or results.
- Uncertain OCR fields remain unconfirmed; show the source screenshot and ask for correction.
- Keep squad and match data on the device by default. Any future cloud/AI processing must be clearly disclosed and opt-in.
- Preserve user control: recommendations can be accepted, edited, or dismissed.
- English-only interface, reports, notifications, and error messages.
- Make no paid API calls or subscriptions without explicit user consent.

## Autonomous workflow
1. **Capture/import** — accept one or multiple squad/player screenshots and match evidence from the Android gallery or camera.
2. **Extract** — use on-device OCR for visible text; keep image-to-player matching conservative. Never infer unreadable stats.
3. **Review and confirm** — present extracted player fields for confirmation before adding/updating the squad database; detect likely duplicates.
4. **Maintain squad database** — persist players, positions, attributes, preferred roles, availability/status, and development history locally.
5. **Optimise selection** — score player-role fit using documented, configurable coaching weights; prevent duplicate selection; show alternatives and explain trade-offs.
6. **Tactical planning** — compare formations against squad strengths, role coverage, defensive protection, width, midfield balance, and the user's aggressive-attacking/strong-defence preference.
7. **Match logging and analysis** — record opponent/mode/result/score/formation/starting XI/substitutions and user-observed issues. Analyse trends only from recorded evidence.
8. **Development planning** — identify role-relevant low attributes, set one or two realistic training priorities per player, and track before/after values. Do not promise rating gains.
9. **Continuous improvement** — compare outcomes before and after a tactical change; recommend changing one major variable at a time where possible.
10. **Reports and backups** — produce an English coaching report and allow export/import of a user-controlled backup.

## Core screens
- Dashboard: squad completeness, recent form, defensive/attacking signals, next actions, data-quality warnings.
- Squad: screenshot import, player profiles, search/filter, duplicate review, player comparison, depth chart.
- Tactics: formation comparison, best XI, role fit, substitutes, tactical plan.
- Matches: match log, footage/evidence attachments, trend analysis, opponent notes.
- Development: individual plans, training history, attribute changes.
- Manager: prioritised tasks, pending confirmations, decision history, settings and data export.

## Safety and technical boundaries
- The app is an assistant, not a game bot. No memory editing, game-file modification, network interception, or automated gameplay.
- OCR is fallible. Extracted values must be visibly marked as unconfirmed until the user reviews them.
- Match-video analysis requires a genuine supported vision pipeline; do not label a share-to-ChatGPT action as in-app analysis.
- If a feature requires internet, an external model, or paid service, disclose this and obtain consent before enabling it.
- Build and test each feature through GitHub Actions before distributing a replacement APK.

## Delivery order
1. Reliable local data model, import workflow, OCR and confirmation UI.
2. Squad quality checks, duplicate detection, lineup alternatives and tactical explanations.
3. Match diary, trend dashboard and development history.
4. Optional opt-in AI analysis service, only after costs, privacy, and reliability are defined.
5. Accessibility, backup/restore, crash handling, and release validation.
