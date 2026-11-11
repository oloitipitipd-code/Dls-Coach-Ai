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

## Defensive emergency response — user-selected requirement
**Priority scenario:** opponents get behind the user's defensive line; defenders do not switch quickly enough and the goalkeeper reacts late. The preferred alert is a warning paired with one recommended action. This is coaching assistance only: the user remains in control of all game inputs.

### User experience
- Show a high-priority, compact on-screen alert and speak the same short instruction through text-to-speech when voice coaching is enabled.
- Example alert: **DANGER — RUN BEHIND THE LINE. Switch to the nearest covering defender and track the runner.**
- Example goalkeeper alert, only when the visual evidence supports it: **KEEPER DECISION — Through-ball entering space behind defence. Consider bringing the goalkeeper out if you can reach the ball first; otherwise stay goal-side and cover the passing lane.**
- If the ball is loose and the opponent is closest, say **PRESS / INTERCEPT** only when the likely interception path is visibly safe; otherwise recommend recovering goal-side.
- Alerts must never say that the app has switched a player, moved the goalkeeper, tackled, collected or kicked the ball. Those actions are not performed by the app.
- Provide a quiet mode, voice mute, alert cooldown and a user-adjustable sensitivity setting. Never cover critical game controls with the coaching panel.

### Detection pipeline design
1. **Frame sampling:** obtain user-authorised MediaProjection frames at a conservative, adaptive rate. Begin with low-rate sampling; increase only if device performance and detection latency remain acceptable. Do not promise frame-perfect reaction.
2. **Scene qualification:** detect whether the match is active and whether the view is sufficiently clear. Ignore menus, replays, pauses, loading screens and low-confidence frames.
3. **Visual tracking:** estimate ball location, direction of travel, visible player locations, team colours where distinguishable, goal area and defensive line. Track objects across consecutive frames; attach timestamps and confidence to every estimate. Do not infer player identity or hidden attributes unless explicitly readable and confirmed.
4. **Danger assessment:** calculate evidence-based indicators such as ball moving into space behind the defensive line, an attacker advancing goal-side of the last covering defenders, a defender trailing the runner, and distance/time-to-goal increasing in danger. Use temporal evidence across multiple frames rather than a single image.
5. **Defender-switch recommendation:** identify the most plausible covering defender from visible position, proximity to the runner/ball, goal-side position and ability to intercept the route. If candidates are ambiguous, issue a generic instruction to select the nearest goal-side defender rather than naming a player.
6. **Goalkeeper decision support:** estimate whether the ball is moving into the space behind the defensive line and whether the goalkeeper appears positioned to reach it before the attacker. Because perspective and game mechanics can make this uncertain, use conditional language and never issue a goalkeeper-rush instruction from weak evidence. Distinguish a collectable through-ball from a situation where staying goal-side is safer.
7. **Alert arbitration:** prioritise imminent one-on-one/clear run-in danger, then immediate through-ball/keeper decision, then recover-and-cover situations. Deliver only the highest-priority actionable instruction; suppress duplicates with a cooldown and update the alert if the situation changes materially.
8. **Feedback and learning:** after the match, let the user mark an alert as helpful, late, incorrect or distracting. Store these labels locally and use them to tune thresholds; do not claim model learning from outcomes that were not recorded.

### Initial event rules (heuristics to validate, not assumed game facts)
- **BREAKAWAY_DANGER:** attacker appears beyond the defensive line with possession or a reachable pass, advancing toward goal, while no clear covering defender is goal-side.
- **THROUGH_BALL_BEHIND_LINE:** ball travels into open space behind defenders and an attacker is positioned to chase it.
- **KEEPER_MAY_SWEEP:** through-ball enters keeper-accessible space and visual tracking suggests the keeper could arrive before the attacker. This event has a higher confidence threshold than a general danger alert.
- **RECOVER_GOAL_SIDE:** possession is lost and the opponent has a forward route; advise recovery and protection of the central lane rather than an unsupported tackle instruction.
- **INTERCEPTION_WINDOW:** a visible pass trajectory crosses near a defender who appears able to reach it first; advise an interception attempt only when confidence is adequate.

### Confidence, latency and safety rules
- Use three confidence bands: high (specific instruction permitted), medium (cautious general warning), low (no alert; continue observing). Thresholds must be calibrated against labelled DLS 26 recordings, not invented in advance.
- A candidate starting target is to keep the live warning useful and timely, but actual end-to-end latency must be measured on the user's phone. Report measured latency and false-alert rate during testing; do not advertise a guaranteed response time.
- If ball/player tracking is lost, the camera changes, visual clutter is high, or team colours cannot be distinguished, show **Tracking uncertain — no reliable defensive alert** rather than guessing.
- Limit repeated voice alerts; do not speak over every frame. Use short phrases and a cooldown, with immediate re-alert only for a materially new danger.
- Automatically reduce sampling or pause analysis if the phone heats, battery drain is excessive, or DLS frame rate is affected.

### Build and validation sequence
**Stage A — capture foundation:** explicit start/stop, Android capture permission, visible capture indicator, frame sampling, lifecycle cleanup and performance telemetry. No tactical claims yet.

**Stage B — offline labelled replay tests:** collect user-provided DLS 26 clips with permission; manually label through-balls, breakaways, defender recovery, goalkeeper sweeps and ambiguous scenes. Split clips into development and held-out test sets. Measure event precision/recall, false alerts per match, missed danger events and alert delay.

**Stage C — live defensive alerts:** implement conservative multi-frame heuristics and show on-screen alerts first. Require the user to confirm whether alerts were correct. Do not enable keeper-sweep recommendations until held-out testing demonstrates acceptable reliability.

**Stage D — synchronised voice:** add Android text-to-speech for the exact displayed alert, with mute, audio focus handling and a short cooldown. Verify that speech does not obscure game audio or become distracting.

**Stage E — personal calibration:** use user feedback and recorded match outcomes to adjust sensitivity. Keep a visible reset option and explain why each alert was generated.

**Release gate:** do not call this feature real-time-ready until it has been tested on the target Infinix Smart 9 during actual DLS 26 matches, with documented latency, false-alert rate, missed-event rate, thermal behaviour, battery impact and game responsiveness. If results are poor, ship it as experimental or offer post-match analysis instead.

## Attacking pass, through-ball and shooting guidance — requested capability
The user wants visual directional guidance to reduce misplaced passes and improve shot selection. The assistant remains observational: it may draw guidance in its own companion overlay, but it must never control the game's joystick/buttons or inject input.

### Pass and receiving-player guidance
- Track the ball, visible teammates, opponents and pitch/goal orientation over consecutive captured frames.
- Estimate likely teammate identities by team colour and trajectory only where reliably distinguishable; otherwise refer to a teammate by visible location (e.g. left-side option, central runner) and do not invent a name.
- Rank candidate receivers using passing-lane openness, opponent interception risk, distance, receiver movement, available space after reception, forward progression and defensive balance if possession is lost.
- Draw an overlay arrow/line from the ball carrier toward the recommended receiving option. Use a distinct dashed line for a through-ball lane into open space, and a warning marker where an opponent may intercept.
- If no safe pass is evident, say **No clear passing lane — retain possession or recycle the ball** rather than forcing a target.
- Recalculate when the ball carrier, teammate run, opponent position or camera view changes. Remove stale arrows immediately when the situation changes.

### Shooting guidance
- Detect whether the ball carrier appears in a plausible shooting position and estimate visible shooting lanes toward the goal.
- Show one or two candidate shot directions only when goal orientation, ball location and goalkeeper/defender positions are sufficiently clear.
- Prefer a lane with less visible obstruction and a plausible angle; consider whether a teammate has a better-positioned option before recommending a shot.
- Distinguish **shoot**, **pass**, and **not enough visual evidence**. Do not claim to predict the exact outcome, power, curve, aim-stick coordinate or scoring probability without validated DLS-specific data.

### Overlay design and interaction
- The companion overlay may show a thin solid arrow for a recommended direct pass, a dashed arrow for a through-ball, and a separate shot-direction guide toward a visible goal opening. Use labels and patterns as well as colour for accessibility.
- Keep the overlay compact, semi-transparent and positioned away from DLS controls and important play areas. The user can move, resize, hide or disable it.
- The overlay is advisory only. It must not obscure controls, tap the screen, move the aim joystick, choose a receiver automatically, or perform a pass/shot.
- Pair important guidance with a short optional voice prompt (e.g. **Through-ball: central runner**) and rate-limit speech to avoid distraction.

### Technical constraints and validation
- Pitch perspective, camera movement, zoom, player overlap, team-kit similarity and motion blur can make object tracking ambiguous. If confidence is insufficient, hide the arrow and display **Pass target unclear — choose manually**.
- Use multi-frame tracking and temporal smoothing; never draw a confident route from a single uncertain frame. Mark predicted space/trajectory as an estimate, not a guaranteed ball path.
- Measure target-selection agreement against human-labelled DLS 26 clips, false recommendations, stale-arrow duration, overlay latency and device performance on the target phone.
- Test passes, through-balls and shots separately across different camera views and match situations. Do not enable a feature as reliable until held-out testing demonstrates acceptable performance.
- Maintain defensive awareness: do not recommend an aggressive forward pass if the visible risk of immediate counterattack is high and a safer option exists.

