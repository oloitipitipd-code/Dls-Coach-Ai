# DLS Coach AI — Autonomous Manager Specification

## Product goal
DLS Coach AI is a standalone, English-only Android companion intended to operate independently on the user's phone while DLS 26 is running. ChatGPT, OpenAI accounts, external AI assistants and cloud AI services are not runtime dependencies. Its own bundled on-device processing handles authorised screen capture, OCR, squad records, match analysis, tactical coaching and player development. It connects to DLS through user-authorised passive visual capture and user-supplied screenshots/results; no official public DLS gameplay API or partner integration has been established. It does not modify the game, control matches, spend coins/gems, buy players, or guarantee wins.

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

## Standalone DLS connection requirement
- The app must complete its core workflows without opening ChatGPT or sending content to an external assistant. No OpenAI key, ChatGPT subscription, external AI account or cloud inference is required for core operation.
- Current connection method: Android MediaProjection screen capture, explicitly approved by the user, with on-device ML Kit OCR. Prefer selecting the DLS app window when Android offers that option. Process frames locally and do not save or upload raw frames by default.
- The visual connection is passive. It can read only what is actually visible in captured DLS screens. OCR text is not equivalent to understanding player movement, ball possession, player identity or tactical events.
- Do not claim a direct internal game-data/API connection. No authorised public DLS 26 gameplay API or SDK has been verified. Any future official integration requires written permission or documented support from First Touch Games.
- Do not use Accessibility Service to tap controls, overlays that interfere with play, input injection, bots, game-memory inspection, reverse engineering, modified APKs, traffic interception or private endpoints. The app remains a coaching companion; the player controls DLS.
- Build local intelligence incrementally: screen OCR, conservative scoreboard/time extraction, confidence-labelled visual signals, then post-match summaries. Do not give tactical alerts until the relevant event detector has been validated against labelled DLS footage.
- If Android or DLS policy changes make a capture method inappropriate, disable it rather than bypassing platform/game protections.

## Safety and technical boundaries
- The app is an independent, local-first DLS companion, not a game bot. No memory editing, game-file modification, network interception, or automated gameplay. Core features must not depend on ChatGPT or another external assistant.
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

## Private, local-only coaching overlay — explicit user requirement
All companion coaching visuals and alerts (pass arrows, through-ball routes, shooting guides, interception warnings, defensive alerts and opponent-analysis labels) are intended to be visible only on the user's own Android device. They must not be sent to, rendered on, or modify the opponent's device or DLS match state.

- Draw guidance only in a separate Android companion overlay/window owned by DLS Coach AI, subject to Android overlay permission and platform rules. Never inject graphics into the DLS app, modify game files, intercept traffic, or alter transmitted match data.
- The overlay is a private display aid, not a shared in-game feature. The opponent receives only the normal DLS gameplay that the user legitimately controls.
- Keep screen analysis local on the device by default. If a future optional remote AI service is introduced, disclose exactly what data leaves the device, request explicit opt-in, and do not send opponent-visible or game-control data.
- If Android cannot safely display the overlay over DLS on the target device, provide an accessible companion notification/audio mode rather than bypassing platform protections.
- Clearly indicate capture/overlay state and provide immediate stop/hide controls. Do not claim absolute privacy against device-level screen recording, malware, or anyone physically viewing the user's screen.


## Precision shooting and shot-outcome learning — explicit user requirement

The user wants the companion to identify a precise visible target area on goal when shooting and improve shot accuracy over time. This is advisory guidance only; the user performs all aiming and shooting inputs.

### Target and trajectory guidance
- When a shot is a reasonable option, identify the visible goal mouth and goalkeeper/defender positions across multiple frames.
- Mark a specific recommended target area on the visible goal (for example, low far corner, high far corner, or near-post space) only when the goal boundaries, camera perspective and goalkeeper position are sufficiently clear.
- Draw a private overlay guide from the ball/shooter toward that target. Clearly distinguish the **recommended aim direction** from the **estimated ball path**; neither is an exact guarantee of the in-game trajectory.
- If useful and supported by validated DLS-specific evidence, show a short predicted path or landing/goal-entry region. Do not invent exact joystick coordinates, shot power, curve, spin, or a guaranteed path.
- Consider shooting angle, distance, visible goalkeeper position, defenders blocking the lane, body/ball movement if reliably observable, and whether a teammate has a clearly better chance.
- Recommend a shot type (such as normal or power shot) only after the relevant DLS controls and their effects have been verified from reliable evidence. Otherwise give direction-only guidance.
- If the goal is partly off-screen, the camera is moving, the keeper/ball cannot be tracked, or multiple lanes are similarly plausible, hide the precise marker and say **Shot target unclear — aim manually or create a clearer angle**.

### Accuracy improvement loop
1. Before the shot, record the recommended target zone, estimated lane openness, confidence and timestamp locally.
2. After the shot, analyse the supplied/captured frames to determine the observed shot direction and outcome only if these are visually clear. Allow the user to correct uncertain labels.
3. Classify outcomes conservatively (e.g. on target, saved, blocked, wide, scored, or unknown); do not infer a miss or save from an ambiguous replay or camera cut.
4. Compare recommendations with observed outcomes over multiple shots and matches. Track target-selection agreement, on-target rate and goal conversion separately; do not treat a goal alone as proof that the aim recommendation was accurate.
5. Tune recommendations only using labelled, sufficiently numerous examples. Keep training/test evidence separate where model evaluation is performed, expose confidence and sample counts, and avoid claiming improvement without measured held-out results.
6. Provide a brief post-match shooting report showing attempts analysed, unknown outcomes, target zones recommended, and measured results. Let the user correct records and reset personal calibration.

### Accuracy, privacy and fair-play safeguards
- Use conservative multi-frame tracking and temporal smoothing; no confident precise target from one uncertain screenshot/frame.
- Do not promise perfect accuracy or a guaranteed goal. Display calibrated confidence only after validation against labelled DLS 26 footage.
- Keep shot markers, aim guides and shot-history analysis inside the user's own companion overlay and local data store by default, subject to Android permissions and the previously defined privacy requirements.
- Never move the aim joystick, tap shoot, change power, select a shot type, or automate any gameplay input.
- Validate separately across camera views, shooting distances/angles, goalkeeper positions and shot types on the target Infinix Smart 9. Measure target-selection agreement, on-target classification accuracy, false target recommendations, analysis latency, battery/thermal impact and game responsiveness.
- Until these tests pass, label precision shooting guidance experimental and provide only broad directional advice when reliable.

## Team spacing, passing options and opponent-lane denial — explicit user requirement

The user wants DLS Coach AI to help their team create useful passing options in possession while reducing the opponent's ability to pass through or around the user's defensive shape. This is tactical advice based only on visible evidence; the app never moves players or changes game controls automatically.

### Creating useful options for the user's team
- Assess visible teammate spacing around the ball carrier: nearby support, forward options, width, depth, central overloads and isolated players.
- Identify when teammates appear too close together, hidden behind opponents, crowded into one channel, or leaving no safe backward/recycling option.
- Recommend a practical action such as switch play, use the wide option, pass to a supporting midfielder, release a forward run, recycle possession, or avoid forcing a pass.
- Identify visible space teammates could attack with a run, but describe it as an opportunity rather than claiming the app can trigger or control that run.
- Suggest formation/role/tactical adjustments between matches when the same spacing problem repeatedly appears. Explain the trade-off with defensive cover and counterattack risk.
- Draw a private overlay marker for a promising passing option or open space only when player tracking and team identification are sufficiently reliable. Hide it when uncertain.

### Closing opponent passing opportunities
- Track visible opponent ball carrier, nearby receivers, teammate defenders, passing lanes and the user's defensive shape across consecutive frames.
- Identify dangerous options such as a central forward pass between midfield and defence, a through-ball channel behind the back line, a switch to an unmarked wide player, or a short pass that bypasses the first defender.
- Recommend which zone or lane to protect and the most plausible nearby defender/midfielder to use as cover, based on visible position, goal-side relationship and ability to block or intercept the lane.
- Prioritise protecting the central route to goal, then the most immediate visible threat; do not tell the user to chase the ball if doing so would visibly open a more dangerous passing lane.
- Give concise instructions such as **CLOSE CENTRAL LANE**, **TRACK THE RUNNER**, **PROTECT THE LEFT CHANNEL**, or **PRESS THE BALL CARRIER — COVER THE PASS INSIDE** only when supported by the scene.
- Distinguish blocking a passing lane from attempting a tackle or interception. Recommend an interception only when the defender appears able to reach the ball safely; otherwise advise screening, tracking or recovering goal-side.
- Detect repeated opponent patterns only after multiple observed examples. Label one-off possibilities as suspected, not established weaknesses.

### Tactical shape and adaptation
- Evaluate team compactness, horizontal and vertical gaps, distance between midfield and defence, width, defensive line depth, fullback exposure and cover behind pressing players.
- Recommend whether to press, hold shape, drop/recover, narrow the block, protect a flank or switch the point of attack. State the main benefit and the risk (for example, narrowing may concede space wide).
- Adapt advice when the opponent changes their build-up route, makes a substitution, changes formation or repeatedly exploits a different channel, but only when the change is visually supported.
- Preserve the user's preferred aggressive-attacking approach while maintaining enough players in defensive cover to reduce counterattacks.
- Provide between-match formation and role recommendations when live evidence repeatedly shows structural gaps; do not claim a formation alone guarantees that lanes will be closed.

### Validation and safeguards
- Use multi-frame tracking of players, ball, team colours and pitch orientation; do not infer hidden player instructions, attributes or opponent intentions.
- If team identity, player positions or camera perspective are unclear, show **TEAM SHAPE UNCLEAR — HOLD POSITION / USE YOUR JUDGEMENT** rather than naming a lane or player.
- Measure lane-threat detection precision/recall, false alerts, missed dangerous passes, alert timing, and whether recommended defensive shape actually reduced completed opponent passes in labelled DLS 26 footage.
- Measure attacking support recommendations separately: useful passing-option agreement, successful progression, possession losses and counterattacks conceded.
- Evaluate on held-out match clips and the target Infinix Smart 9. Monitor latency, battery, heat and game responsiveness; reduce sampling or pause if performance is affected.
- All arrows, lane markers and tactical labels remain in the user's private companion overlay/local data by default. No game input automation, player movement, game modification, traffic interception or opponent-device display.

## Aerial duels, corners, crosses and heading intelligence — explicit user requirement

The user wants help winning more defensive aerial balls (especially when opponents take corners or cross) and creating better opportunities for their own players to score headers from crosses and set pieces. This is observational coaching only; the user performs all player switching, movement, marking, jumping, crossing and shooting inputs.

### Defending opponent corners and crosses
- Recognise when the opponent is preparing/taking a corner or delivering a cross, and qualify the scene before giving advice.
- Track the ball's visible flight, landing area, attacking runners and user's defenders across multiple frames where the video quality allows.
- Identify the most plausible defender to challenge for the ball using visible position relative to the ball's projected landing zone, goal-side position, proximity, approach path and nearby opponent pressure. Do not infer hidden height, strength, jumping or heading attributes from appearance.
- Recommend protecting the most dangerous near-post, central or far-post zone only when the ball trajectory and goal orientation are sufficiently clear.
- Alert the user to track an unmarked opponent, protect the central six-yard/goal area, challenge for a reachable aerial ball, or clear the ball when a safe clearance opportunity is visible.
- For a defensive header, distinguish a genuine reachable challenge from a situation where a defender is poorly positioned or likely to miss the ball. If uncertain, recommend protecting goal-side space rather than an unsupported jump/challenge.
- After the first defensive contact, assess whether the ball is cleared, remains dangerous, or falls to an opponent. Recommend a second-ball recovery position and avoid claiming the danger has ended while the ball remains live.
- Never claim a particular defender will win the header; phrase it as the best visible candidate and show confidence.

### Creating the user's own crossing and heading chances
- During possession, detect a plausible crossing opportunity from wide areas and assess the positions and movement of visible teammates and defenders in the penalty area.
- Identify potential receivers (e.g. near-post run, central run, far-post run, or edge-of-area support) using visible trajectories and space, without inventing player identities or hidden attributes.
- Recommend a cross only when a teammate appears to have a plausible route to the delivery area and the opposing defenders/goalkeeper do not clearly dominate that space. Otherwise suggest retaining possession, switching play, or making a safer pass.
- Show a private overlay guide for the likely delivery zone and receiver run only when ball, goal, teammate and opponent tracking are sufficiently reliable. Distinguish the intended cross destination from the actual ball trajectory; it is an estimate, not an exact control instruction.
- Assess whether the visible crossing lane is blocked, whether the receiver is marked, whether the far post is open, and whether a cut-back or ground pass appears safer than an aerial cross.
- Recommend a suitable visible target zone (near post, central area, far post, or cut-back zone) rather than promising that a particular player will score.
- For corners taken by the user's team, identify visible unmarked runs and crowded zones and suggest a delivery area only when the scene supports it. For defending corners, prioritise marking/covering dangerous runners and protecting the goal area.

### Aerial and crossing performance analysis
- Log only confirmed or user-corrected events: opponent corners, opponent crosses, defensive aerial challenges, clearances, second-ball recoveries, user's crosses, header attempts, shots on target, goals and unknown outcomes.
- Report separate measures: defensive first-contact rate, opponent second-ball recoveries/conceded chances, user's cross completion, header attempts, headers on target and goals from headers. Do not conflate these metrics.
- Compare recommendations with observed outcomes over multiple matches. Allow the user to correct uncertain event labels and show sample counts; do not claim improvement from a tiny or ambiguous sample.
- Use confirmed squad attributes/roles and user-provided player data to inform between-match aerial-role suggestions only when those attributes are available and verified. Do not assume that a taller-looking player has better heading ability or that a particular player has a hidden aerial stat.
- Suggest training/development priorities only when DLS 26 provides a verified relevant attribute or training mechanism; do not invent a heading upgrade mechanic or promise attribute gains.

### Validation, privacy and fair-play safeguards
- Use multi-frame ball-flight and player tracking; aerial motion blur, camera perspective, overlapping players and replays can make landing zones and first contact uncertain.
- If the ball flight, team identity, landing zone or player positions cannot be tracked reliably, show **AERIAL PLAY UNCLEAR — PROTECT THE GOAL AREA** or suppress the recommendation.
- Evaluate defensive corner/cross detection and attacking cross/header guidance separately on labelled DLS 26 footage, including precision, recall, first-contact classification, target-zone agreement, false alerts and timing.
- Validate on held-out clips and the target Infinix Smart 9. Monitor latency, battery, heat and game responsiveness; reduce sampling or pause if performance is affected.
- Keep all aerial target markers, crossing guides and defensive alerts in the user's private companion overlay/local data by default. Never automate gameplay input, modify DLS, intercept traffic or display guidance on the opponent's device.

## Goalkeeper cross-claim decision support — explicit user requirement

The user wants help deciding when to bring the goalkeeper out to catch or claim opponent crosses, especially corners and balls delivered into the penalty area. The companion must not automatically activate the goalkeeper or perform any game input; it can alert the user when a manual goalkeeper action appears appropriate.

### Cross-claim assessment
- Detect an opponent cross/corner and track the ball's flight, likely landing area, goalkeeper position, nearby attackers and defending players over consecutive frames.
- Assess whether the ball appears to enter the goalkeeper's reachable collection area, whether the keeper has a clear route, whether an opponent is likely to arrive first, and whether a defender is better placed to challenge.
- Recommend **BRING KEEPER OUT / CLAIM** only when the visual evidence strongly supports a safe, reachable claim and the keeper appears likely to arrive before the attacker.
- Recommend **KEEPER STAY / DEFENDER CHALLENGE** when the cross is contested, the ball is beyond the keeper's likely reach, an attacker has the better route, or leaving the goal would expose an obvious scoring opportunity.
- For uncertain crosses, show **CROSS DANGER — PROTECT THE GOAL; CLAIM DECISION UNCLEAR** rather than encouraging a risky rush.
- If the ball is caught/claimed, confirm this only when clearly visible. Otherwise track the second ball and alert the user to the continuing danger.
- Distinguish a catch/claim from a punch or deflection. Do not promise the keeper can catch every cross; the appropriate action depends on ball height/flight, reach, traffic and game mechanics.

### User interaction and alerting
- Provide a short, high-priority visual warning and optional synchronised voice cue, for example **CROSS IN — KEEPER CAN CLAIM: BRING HIM OUT NOW** or **CROSS IN — STAY GOAL-SIDE: DEFENDER IS BETTER PLACED**.
- The user must manually activate any goalkeeper movement/rush control available in DLS 26. The app must not tap, hold, swipe, move the keeper, catch, punch or otherwise automate gameplay.
- Do not assume the exact DLS 26 control mapping or activation timing. Verify the relevant control from reliable in-game evidence or user confirmation before displaying control-specific instructions.
- Include a conservative confidence threshold, alert cooldown and immediate suppression when tracking is lost or the cross is no longer dangerous.

### Validation and safeguards
- Label opponent crosses and corners in DLS 26 footage with the observed outcome: keeper catch/claim, punch, defender clearance, attacker contact, second-ball chance, goal, or unknown.
- Evaluate claim-versus-stay recommendations separately for catches, punches and defensive challenges. Measure decision precision, unsafe rush recommendations, missed claim opportunities, alert timing and false alerts.
- Validate on held-out footage and on the target Infinix Smart 9; monitor processing latency, heat, battery and game responsiveness.
- Keep keeper-decision alerts local and private in the companion overlay by default. Never modify the game, automate input, intercept traffic or claim guaranteed catches.

## Additional team-performance intelligence — proactive recommendations

The following complementary capabilities are added to help the user's team perform more consistently. All live advice is observational and the user remains responsible for every game input.

### 1. Stamina-aware substitutions and role protection
- Track visible stamina indicators only when they can be read reliably; otherwise allow the user to enter substitutions and stamina observations after the match.
- Identify players whose reduced stamina appears to coincide with slower recovery, late defensive tracking or weaker forward support.
- Recommend a substitution candidate by role fit, confirmed squad data, tactical need and remaining match context; explain what the change improves and what it may weaken.
- Do not assume a universal stamina threshold or guarantee that a substitution will improve the result. Do not make substitutions automatically.

### 2. Transition and counterattack protection
- When possession changes, assess whether the team is exposed to an immediate counterattack, which central or wide lane is most vulnerable, and whether enough players remain goal-side.
- Recommend a quick recovery shape, protecting the central route first when the visible threat is central, or covering the exposed flank when the threat is wide.
- In possession, assess whether an aggressive forward pass or cross leaves a dangerous space behind the ball. Prefer a safer support option when the visible counterattack risk is high.
- Track whether goals/chances conceded followed a turnover, an exposed flank, a central gap or a failed recovery, but classify causes as confirmed only when footage supports them.

### 3. Pressing and defensive-block intelligence
- Identify a plausible pressing trigger from visible play, such as a receiver facing their own goal, a loose/heavy touch, a backward pass or a receiver isolated near the sideline, only where the event is clearly observable.
- Recommend coordinated pressure on the ball carrier while protecting the nearest dangerous passing lane; avoid recommending an isolated press that opens a direct route to goal.
- Recommend holding shape, narrowing, dropping or pressing higher based on the visible opponent threat and the user's tactical preference.
- Treat pressing triggers as suggestions, not certainty about opponent intent or game mechanics.

### 4. Set-piece planning for both teams
- For the user's attacking corners and free kicks, identify visible target zones, potential runners and short-pass alternatives when the scene is clear.
- For defending corners and free kicks, identify dangerous opponents, likely delivery zones and areas needing cover; integrate goalkeeper claim-versus-stay guidance.
- Record set-piece outcomes separately from open-play events and recommend changes only after repeated evidence.
- Do not assume a particular set-piece routine or control sequence unless verified in DLS 26.

### 5. Match-state tactical adaptation
- Consider scoreline, match time, current formation, visible opponent pressure and available substitutes when suggesting whether to protect a lead, seek an equaliser or chase a goal.
- When leading, prioritise compactness and safe possession without abandoning all attacking outlets. When chasing, suggest measured attacking changes while warning about the defensive space they expose.
- Do not infer the match clock or score when it is not clearly visible; ask the user to confirm uncertain match context.

### 6. Post-match diagnosis and controlled experiments
- Produce a concise report separating goals conceded/scored, shots, chances, turnovers, pass patterns, crosses, corners, aerial duels, goalkeeper decisions and substitutions where observable or user-confirmed.
- Identify the two or three strongest evidence-supported performance problems rather than listing every possible weakness.
- Suggest one major tactical adjustment at a time where practical, record the change, and compare a sufficient set of subsequent matches against the prior baseline.
- Track mode, opponent strength only if a defensible measure is available, formation, lineup and match context so comparisons are not misleading.
- Show sample sizes, unknown events and uncertainty. Do not claim a tactic caused improvement from a single win or loss.

### 7. Reliability and user control
- Provide a simple coaching intensity setting (quiet, balanced, urgent) and allow separate control of voice, visual alerts and overlay guidance.
- Include a short pre-match checklist: formation/role balance, defensive cover, set-piece plan and available substitutes.
- Include a post-match correction screen so the user can correct OCR, event labels, match result and inaccurate recommendations.
- Keep all recommendations advisory, explain the evidence briefly, and provide a way to dismiss or report an incorrect alert.
- Validate each feature separately on labelled DLS 26 footage and the target Infinix Smart 9. Monitor false alerts, latency, heat, battery and game responsiveness; disable or downgrade unreliable features.

## Player development, recruitment and transfer intelligence — explicit user requirement

The user wants DLS Coach AI to help develop existing players (including players rated 80+ toward 90+), identify better players outside the squad from visible opponent evidence, and manage recruitment priorities. The assistant must distinguish recommendations and tracking from actions it cannot legitimately perform in DLS 26.

### Existing-player development
- Maintain a confirmed player profile with current rating, position, visible attributes, role suitability, development history and user-confirmed availability.
- Identify players rated 80–89 who may be worth investing in, but assess role-relevant attributes and squad value rather than rating alone.
- Recommend a development priority for each player using only verified DLS 26 development/coaching mechanics and confirmed player data.
- Track before/after ratings and attributes, development resources spent when the user records them, and observed performance changes.
- Estimate a possible development pathway only when supported by verified game rules and recorded player progression. Never promise an 80+ player will reach 90+, invent a training outcome, or claim an attribute can be upgraded if the game does not support it.
- Identify when further investment appears inefficient compared with recruiting a better player, explaining the evidence and uncertainty.
- The user must approve and carry out every in-game development action unless a legitimate, documented DLS feature provides an explicit supported integration. Do not automate taps or gameplay.

### Recruitment and transfer shortlist
- Identify squad gaps from the confirmed lineup, tactical system, substitutes, injuries/availability if applicable, and recurring match weaknesses.
- Build a prioritised recruitment shortlist by position and role (e.g. centre-back with recovery pace and defensive ability, defensive midfielder with tackling and stamina, forward with finishing and movement), using verified visible attributes.
- Compare candidates by role fit, rating, relevant attributes, team balance, expected development potential only where evidence exists, and the user's available in-game coins/gems when supplied.
- Track transfer-market candidates and player availability only from screenshots or other user-provided, permitted evidence. Do not claim continuous access to the live DLS transfer market unless a legitimate supported source/integration is actually available.
- For an opponent's player who is not in the user's squad, record the identity, position, visible rating/attributes and observed performance only when these are clearly visible or user-confirmed. Distinguish an opponent's best observed performer from a generally best player.
- Build a watchlist of opponent players and external recruitment targets with evidence, date/match context and confidence. Do not infer hidden ratings, attributes, contract status, price or availability.
- Recommend a purchase only when it addresses a defined squad need and is affordable based on confirmed resource information. Explain which current player would lose a place and how the signing changes the squad.

### Purchase and development approval gates
- The app may autonomously analyse, rank, track, remind and prepare a recommended transfer/development plan.
- It must not spend coins or gems, buy/sign/release players, confirm purchases, consume coaches/resources, or execute development actions without the user's explicit approval for that specific action.
- Never store, request or use the user's game password, session token or other credentials. Do not automate game controls, modify game files, intercept game traffic or bypass game protections.
- If DLS 26 offers no official supported API/integration for transfers or training, present the recommendation and a clear manual checklist; do not claim that the app can buy or upgrade players inside the game.
- Provide a purchase confirmation summary before any future supported transaction: player, position, rating/attributes, cost, available balance after purchase, squad impact and reason. Require explicit confirmation and provide a cancel option.
- Maintain a transaction/development history only from verified user-confirmed actions; never claim an in-game action succeeded merely because the user viewed a recommendation.

### Recruitment and development learning
- Evaluate signings after multiple recorded matches using role performance, team balance, match context and the problems the signing was intended to solve.
- Compare development investments using recorded before/after values and resources spent. Separate rating increase from actual match contribution.
- Track whether recruited players improve the intended outcome (e.g. fewer goals conceded, better midfield recovery, more chances created) while acknowledging opponent and match variability.
- Display sample counts, missing data and uncertainty. Do not label an opponent player as a priority target based on one isolated event.

### Validation and user experience
- Add a Player Development screen with confirmed profiles, development priorities, before/after history and resource notes.
- Add a Recruitment screen with squad gaps, ranked targets, opponent-player watchlist, comparison and approval checklist.
- Keep all player data local by default and provide export/backup. Clearly label OCR-extracted but unconfirmed information.
- Test calculations and UI using manually verified squad screenshots and user-confirmed transfer/development records before claiming the feature works.

## Gradual long-term player maximisation — clarified requirement

The user explicitly wants the system to develop suitable existing players gradually over many coaching sessions, continuing until each reaches the maximum permanent development allowed by that player's card/game rules, rather than replacing every player below 90 overall.

### Verified game-mechanic distinction
- First Touch Games' official support guidance says coaches are used to develop players and randomly select a player to permanently upgrade.
- Form Boosts are temporary attribute increases for a limited number of matches; they must never be recorded as permanent development.
- The app must inspect the current in-game Coaches screen and the player's actual card/attribute details where possible. Do not hard-code an assumed universal maximum rating, +10 ceiling, coach yield, rarity effect, or training allocation without current official evidence or user-provided in-game confirmation.
- Standard coach selection may be random, so the app must not promise that using a coach will upgrade a particular chosen player. It should track the actual recipient and result after the user applies a coach.
- If the current DLS version offers a targeted/special coach for an eligible card, the app may recommend it based on the on-screen rules, but must not assume all players/cards are eligible.

### Long-term development workflow
1. Establish a baseline for every player from user-confirmed card rating and visible attributes.
2. Rank development candidates by role value, squad importance, starting rating, confirmed attributes, development progress and the team's tactical needs. A player rated 80+ may be worth retaining and developing; rating alone must not determine the decision.
3. Recommend the appropriate in-game coach category using the current coach descriptions and verified attribute effects. Check the in-game cost and available coach resources before advising the user to spend.
4. Before the user applies a coach, show the likely eligible recipients/risks and clearly state if standard coach selection is random. Require the user to make the in-game decision.
5. After training, ask for or import a screenshot of the actual recipient and updated attributes/rating. Record the actual result, coach used and resource cost; never assume the intended player received the upgrade.
6. Repeat over time, maintaining a per-player development ledger and showing progress from baseline, recent changes, remaining visible development opportunities and any uncertainty about the maximum.
7. Stop recommending permanent training when the game indicates the player has reached their maximum development potential or relevant attributes are capped. Mark the player as **fully developed** only when supported by in-game evidence.
8. Compare continued development cost/opportunity with recruitment alternatives, but do not discard a useful player merely because the player has not yet reached 90 overall.
9. Keep Form Boosts in a separate temporary-performance ledger with expiry/matches remaining. Never mix temporary boosts into permanent rating progression or claim they move a player toward their permanent maximum.
10. Provide squad-wide progress reports: players in development, permanent rating/attribute changes, coaches/resources used, fully developed players, and players better prioritised for recruitment.

### Limits and user approval
- DLS Coach AI may plan, track, calculate, compare, remind and recommend gradual training.
- It must not automatically spend coins/gems, consume coaches, release players or operate the game. The user must perform and approve every in-game training or recruitment action.
- Do not promise a specific final rating or timeline. Development outcomes depend on the player's actual game limits, coach selection and game version.
- Re-check current official DLS guidance and the user's live in-game screens when mechanics change.

## 97+ permanent overall development target

The user's squad-building target is **97+ permanent overall rating** for players selected as long-term development investments.

- Treat 97+ as the preferred target, not a guaranteed outcome or a universal maximum.
- Estimate feasibility from the player's confirmed original card rating and the current, verified DLS 26 development rules. A community DLS 26 guide reports a +10 permanent overall development ceiling (with an 86-rated Harry Kane reaching 96), but this is not confirmed by the official First Touch Games support article. Mark this ceiling as community-reported until verified in-game or by an official source.
- If the reported +10 ceiling applies to the user's card, a base rating of at least 87 would be needed to reach 97 permanently. This is a conditional calculation, not a universal rule; card type, special-card rules and future game updates may differ.
- Before recommending a player for a 97+ development pathway, inspect the actual card type, original rating, current permanent rating, visible attributes and in-game coach/development screen. Distinguish permanent overall rating from temporary Form Boost display.
- Classify players as: **97+ pathway plausible**, **97+ pathway unverified**, or **97+ target not supported by current evidence**. Explain the reason and never imply certainty where the maximum is unknown.
- Prioritise recruitment of players with a credible route to 97+ if they are not already in the squad. Do not waste development resources chasing an unsupported target; compare role fit and attribute needs as well as overall rating.
- Track each player's permanent progress and stop when the in-game maximum is reached. Report the actual final rating, even if below 97, rather than falsifying or obscuring the result.
- Official FTG guidance confirms that standard coaches randomly select a player for permanent upgrade and that Form Boosts are temporary. Source: https://support.ftgames.com/hc/en-us/articles/360003914778-How-do-I-develop-my-players
- Community-reported DLS 26 +10 ceiling and example: https://dmairdls.com/blog/how-to-max-players-in-dls-26-coaches-form-boosts . Treat as secondary evidence, not official confirmation.

## Recruitment threshold: 80+ overall

The user's minimum recruitment threshold is **80 overall rating**.

- Shortlist players rated 80 or higher; do not shortlist a player below 80 unless the user explicitly changes this rule.
- Evaluate each 80+ candidate by position, role fit, visible attributes, squad need, resources and development potential—not overall rating alone.
- The intended pathway is recruit 80+ players, retain suitable players and progressively apply permanent development coaches over multiple sessions.
- Set 97+ as the preferred final overall target where the specific card's verified development ceiling makes it plausible.
- Do not imply every 80+ recruit can reach 97+. If a +10 permanent overall development ceiling is confirmed for that card, an 80–86 base-rated player would have a corresponding 90–96 ceiling, while an 87+ base-rated player could have a 97+ pathway. Treat this arithmetic as conditional on the actual in-game rule and card type.
- For every candidate, display starting rating, estimated/verified maximum, gap to 97, confidence in the estimate, key role attributes and a clear recommendation: recruit for 97+ pathway, recruit for role value and develop to verified maximum, or do not recruit.
- Track recruitment and development separately: date acquired, initial rating and attributes, each coach use, actual recipient, permanent rating/attribute changes, resource cost, current rating and verified stopping point.
- Never spend coins, gems or coaches, or make a game purchase, without explicit user approval. The user performs all in-game actions manually.

## Squad-benefit recruitment gate

An 80+ rating is a minimum eligibility filter, **not** a reason by itself to recruit. Recommend a player only when evidence indicates that the player would materially benefit this specific squad and the user's aggressive-attacking / strong-defence style.

Before recommending recruitment, compare the candidate with current squad players in the same role and assess:
- Whether the position or role is an actual squad weakness or depth gap.
- Whether the candidate improves role-relevant visible attributes (for example pace, stamina, strength, tackling, positioning, passing, crossing, dribbling or finishing as appropriate to the role).
- Whether the candidate improves the starting XI, rotation options, tactical flexibility or reliable cover.
- Whether the candidate duplicates an already strong role while leaving a more important weakness unresolved.
- Recruitment cost and opportunity cost, using only values visible in-game or supplied by the user.
- Whether the candidate has a credible development pathway toward 97+ or, if not, still provides enough immediate role value to justify recruitment.

Classify each candidate as **strong squad upgrade**, **useful depth**, **situational option**, or **do not recruit**. Explain the specific squad benefit and the evidence. Do not recommend a signing solely because of overall rating, popularity, name or speculative potential. If the candidate does not improve the squad enough to justify the cost, recommend saving resources.


## Final quality-squad standard — recruitment, development and team balance

This section consolidates the user's standing squad-building rules and takes precedence over any conflicting generic recruitment wording elsewhere in this document.

### Non-negotiable recruitment gate
1. **Minimum overall rating: 80.** Do not recommend a player below 80 unless the user explicitly changes this threshold.
2. **Squad benefit is mandatory.** An 80+ rating alone never justifies recruitment. The candidate must solve a documented squad weakness, improve the starting XI, provide valuable role-specific depth, or add a clearly useful tactical option.
3. Compare the candidate directly with the user's current players in the same position/role. Explain who the candidate would replace or cover, what improves, and what trade-off remains.
4. Reject unnecessary duplicates when a more important position remains weak. If no candidate offers enough value for the cost, recommend saving resources.
5. Use only visible, verified card data and user-confirmed costs. Unknown attributes, prices, availability and potential must remain unknown.

### Team-quality assessment
Assess the squad as a complete team, not as a collection of high-rated cards:
- **Goalkeeper:** verified goalkeeper reactions and handling, with the best available keeper selected for the role.
- **Centre-backs:** role-appropriate tackling, strength, speed, stamina and defensive cover.
- **Fullbacks/wingbacks:** speed, stamina, tackling and useful passing/crossing, balanced against the space they leave behind.
- **Defensive midfield:** tackling, strength, stamina, passing and ability to protect the back line.
- **Central/attacking midfield:** passing, control, stamina, progression and support for both attack and defence.
- **Wingers/forwards:** speed, acceleration, control, passing/crossing and finishing according to the intended role.
- **Strikers:** finishing, control, speed and acceleration, with role fit and chance creation considered.
- Evaluate left/right balance, natural-position coverage, substitutes, fatigue options and tactical flexibility.
- Preserve the user's preferred identity: aggressive attacking with strong defensive protection. Do not maximise attack by leaving the defence exposed.

### 97+ development pathway
- **97+ is the preferred long-term overall target**, but it is not a guaranteed or universal maximum.
- Recruit 80+ players only when they provide sufficient immediate squad value. Separately identify which recruits have a credible 97+ development pathway under the actual card-specific DLS 26 rules.
- Do not label an 80–86 player as a 97+ prospect based solely on the unverified community-reported +10 rule. If that +10 limit is confirmed for the specific card, explain that the resulting ceiling would be 90–96; a starting rating of 87 or above would be needed for 97+ under that assumption.
- If a player cannot credibly reach 97+, the player may still be recruited for an important immediate role or useful depth when the value justifies the cost. Do not waste resources solely to chase a number.
- Track original rating, current permanent rating, visible attributes, card type, each actual coach outcome and resource cost. Keep temporary Form Boosts separate.
- Standard coaches may select a random eligible player. Never promise that a particular player will receive an upgrade. User confirms and performs all in-game actions.
- Stop at the maximum evidenced by the current game/card. Report the actual final rating, even when it is below 97.

### Recruitment decision report
For every shortlisted player, show:
- Eligibility: 80+ confirmed or not eligible.
- Position and intended role.
- Current squad comparison and exact weakness addressed.
- Visible attribute advantages and disadvantages.
- Effect on starting XI, bench depth and tactical balance.
- Cost/opportunity cost, only when confirmed.
- Development pathway: 97+ plausible, unverified, or not supported by current evidence.
- Final decision: **SIGN — STRONG UPGRADE**, **SIGN — USEFUL DEPTH**, **CONSIDER — SITUATIONAL**, or **DO NOT SIGN — SAVE RESOURCES**.

### Quality assurance before calling a squad “strong”
Do not call a squad optimised or complete until:
- At least 11 confirmed players and their natural positions are recorded.
- The proposed XI contains no duplicate player and uses players in defensible roles.
- The most important positional weaknesses and depth gaps are disclosed.
- The starting XI and substitutes are compared against the chosen aggressive-attacking/strong-defence style.
- Unconfirmed OCR fields and missing attributes are clearly flagged.
- Recommendations explain evidence and trade-offs; no fabricated ratings, stats, prices or guarantees.
- The app's actual implementation is distinguished from planned features. A specification, mock-up or recommendation algorithm is not proof that live vision, automatic recruitment, player upgrades or other features are implemented.

### Current product truth
The current Android app supports manual player entry, local squad/match records, formation selection, heuristic role-fit XI estimates, basic development priorities, on-device OCR text extraction for review, and sharing match evidence/report text. OCR does not yet create player records automatically. Match footage is shared for external review; the app does not perform in-app video analysis. Live screen capture, live computer vision, private tactical overlay, voice coaching, automated structured squad extraction, persistent coach-outcome development ledger and recruitment-market integration are not implemented in the inspected app version. Do not describe any of those as working until code is added and a successful build plus device testing confirms it.


## Optional external integrations — version 1.4.0

External services are optional helpers, never dependencies for core coaching:
- **GitHub (read-only):** a user-triggered request to the public GitHub Actions API checks the latest workflow run for this public repository and displays its title, status/conclusion and short commit SHA. The user can open the repository or workflow page. No GitHub authentication token is requested or stored. This does not install updates or grant repository write access.
- **ChatGPT (manual share):** the user can generate a coaching report and invoke Android's standard text-share chooser. The user chooses ChatGPT, if installed, and completes the share. The app must not silently transmit reports, screenshots, player records or other personal data.
- **Offline-first:** squad records, match history, tactics and local coaching features must continue to work if internet access is unavailable or either optional service is absent. The GitHub status check requires internet; ChatGPT report review requires an installed receiving app and an explicit user share action.
- **Credentials:** never embed GitHub tokens, OpenAI API keys or account passwords in the APK. Do not request credentials for the current read-only GitHub check or manual ChatGPT share.
- **Future integrations:** any authenticated GitHub workflow, cloud AI analysis or automatic synchronisation requires a separate, secure design, clear data disclosure, cost disclosure where relevant, and explicit user opt-in. Do not imply these future functions exist until implemented and tested.


## DLS game launcher and settings profile — version 1.5.0

The Game Link screen uses Android's launcher-intent visibility to look for an installed launcher activity whose visible app label contains "Dream League Soccer". If found, it launches that activity using a normal Android intent. This is app discovery and launch, not a privileged game integration. The screen reuses the existing user-authorised MediaProjection controls and lets the user record their own camera, graphics/performance, controls, sound and other settings notes in local preferences.

The settings profile does not write into DLS or apply values automatically. DLS internal settings, account state, live game state and player data remain outside the app's direct control unless First Touch Games supplies a documented supported interface. OCR remains limited to visible text and must not be represented as reliable gameplay understanding. Keep all game-control, input-injection, private-file and reverse-engineering restrictions in force.
