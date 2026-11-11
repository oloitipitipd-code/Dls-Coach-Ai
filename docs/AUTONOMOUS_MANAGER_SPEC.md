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

## Live match coaching — requested product capability
The target experience is a user-controlled live companion for DLS 26 with both spoken coaching and a compact on-screen coaching panel.

### Live session flow
1. The user starts a coaching session and explicitly grants Android screen-capture permission. Prefer Android MediaProjection app-only capture where the device/Android version supports it; otherwise explain if whole-screen capture is required.
2. Capture and analyse sampled frames rather than assuming every frame can be processed. Monitor CPU, memory, temperature, battery and game responsiveness; reduce sampling or pause analysis if it causes noticeable lag.
3. Present concise, confidence-labelled observations and tactical suggestions. Use text first; optional text-to-speech reads the same short advice through the user's selected audio output.
4. Provide clear pause, stop, mute, and capture-status controls. Stop capture when the session ends. Do not record or transmit audio; screen capture is opt-in and visibly indicated.
5. Save a post-match summary only after the user confirms the match result and any uncertain events.

### Opponent weakness intelligence
Analyse only what is actually visible in captured gameplay or supplied match evidence. Maintain a per-match opponent profile with confidence and evidence/time markers. Candidate observations include:
- Repeatedly exposed left or right defensive channel.
- Space between midfield and defence, or poor midfield recovery.
- Defensive line repeatedly vulnerable to through-balls or pace.
- Slow recovery after losing possession, weak pressing, or exploitable build-up patterns.
- Goalkeeper positioning or rebound patterns only when clearly visible in multiple relevant events.
- Repeated tendencies such as overcommitting fullbacks, predictable passing routes, or leaving a player unmarked.

For each observation, distinguish **observed**, **suspected**, and **not enough evidence**. Recommend a practical response (e.g. switch the point of attack, use width, target space behind the line, protect the exposed channel, or change pressing intensity) and explain the evidence briefly. Never invent opponent player ratings, hidden attributes, formation, or intentions. Reassess when the opponent changes tactics. Avoid overreacting to a single ambiguous frame.

### Coaching output priorities
- Immediate safety/defensive alert when a clear dangerous pattern is detected.
- One concise tactical action at a time; avoid distracting streams of commentary.
- Spoken and on-screen advice must stay synchronised and be independently muteable.
- Advice must respect the user's preferred aggressive-attacking style while maintaining defensive cover.

### Technical, platform and fair-play boundaries
- This feature is a passive, user-authorised visual assistant. It must not tap controls, move players, automate gameplay, alter game files, intercept game traffic, or provide unfair hidden-information advantages.
- Do not bypass Android capture indicators, permissions, or other platform protections. Respect DLS terms and applicable platform policies; if a proposed implementation conflicts with them, do not implement it.
- Real-time computer vision may be uncertain and resource-intensive on entry-level phones. Validate latency, heat, battery use, frame drops and false detections on the target device before enabling live advice by default.
- If reliable on-device analysis is not feasible, offer a lower-frequency live mode or post-match analysis rather than claiming real-time accuracy.

