# Real-account integration audit — DLS Coach AI

**Audit date:** 11 November 2026  
**Repository baseline:** `main`, app version 1.5.0 (versionCode 6)  
**Purpose:** establish what DLS Coach AI can technically access and what it may safely/permissibly automate before implementing deeper integration.

## Executive finding

**A verified, supported direct DLS 26 account API/SDK or partner integration has not been identified.** The repository itself correctly records this limitation. The app must not claim that it can read private account data, change DLS settings, upgrade players, claim gifts, recruit players, spend currency, or control gameplay.

First Touch Games' published EULA (last updated 25 July 2025) prohibits, except where expressly permitted, cheats, exploits, automation software, bots, hacks, mods, and unauthorised third-party software designed to modify or interfere with the Games. It also prohibits attempts to gain unauthorised access to the Online Service or connected systems other than through the interface provided by First Touch Games. The EULA warns that violations may lead to action including a game ban.

Accordingly, **do not build game-control automation, input injection, private endpoint/API access, traffic interception, game-memory/file inspection or modification, reverse engineering, modified APK support, or automated in-game purchases/rewards/upgrades.** This is a product and account-safety boundary, not merely a technical limitation.

## Current implementation verified in repository source

Inspected files:
- `README.md`
- `docs/AUTONOMOUS_MANAGER_SPEC.md`
- `app/src/main/java/com/dlscoachai/app/MainActivity.kt`
- `app/src/main/java/com/dlscoachai/app/LiveCaptureService.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `.github/workflows/android.yml`

The current app is a native Kotlin/Jetpack Compose Android app (version 1.5.0). It already has:
- Local squad/player records and manual player editing.
- Formation and tactical planning.
- Match result logging and basic trend analysis.
- Role-fit starting XI estimates based on entered attributes.
- Player progress snapshots.
- JSON backup/restore and text coaching report export/share.
- User-authorised Android MediaProjection capture with a foreground notification and stop control.
- On-device ML Kit text recognition on sampled visible screen frames.
- A DLS launcher lookup/open flow and a locally stored settings-notes profile.
- An optional read-only GitHub Actions status lookup and manual report sharing.

The inspected `LiveCaptureService` samples a screen frame about once per second and performs local OCR at a slower interval. It does not persist or upload raw frames, capture audio, interpret match events, identify players/ball trajectories, or send game inputs. The current OCR text is not yet a validated structured player-data feed. Screenshot OCR produces reviewable drafts; user confirmation is required before changing squad records.

The existing settings profile is notes only: it does not write settings into DLS. Opening DLS via its normal Android launcher activity is not privileged account integration.

## Management dashboard implementation update — 11 November 2026

A local Manager tab has been added on the development branch. It records user-entered resource snapshots (coins, gems, available coaches and unclaimed gifts), timestamps the latest snapshot, presents coaching recommendations for approval or dismissal, and stores a local decision history. This is a management workflow over user-confirmed information, not a direct account connector. No resource value is read from DLS automatically, and approval does not trigger any in-game action. The resource ledger and decision history are currently held in SharedPreferences and are not yet part of the JSON backup schema; this limitation must be addressed before treating them as durable backup data.

## Capability and permission matrix

| Capability | Current status | Safe product treatment |
|---|---|---|
| Launch installed DLS using its normal launcher activity | Implemented | Keep as a convenience link |
| User-authorised screen capture | Implemented | Keep permission prompt, persistent foreground notification, clear stop control and local processing |
| OCR of visible screen text | Implemented, limited | Treat output as untrusted draft text; require user review |
| Read squad/player details from visible screens | Partially feasible in principle; not yet validated as structured extraction | Develop conservative screen parsing and confidence-labelled confirmation; never claim access to hidden account data |
| Read match events from live frames | Not implemented | Research passive computer vision only; validate against labelled clips before coaching claims |
| Read private account state, balances, inbox gifts or server-side inventory | No supported interface verified | Do not attempt private endpoints, storage scraping or traffic interception |
| Change DLS camera, graphics, control or sound settings | Not supported by current code; no documented interface verified | Give guided instructions; user applies settings in DLS |
| Upgrade players, consume coaches, recruit/release players, claim gifts or spend coins/gems | No supported interface verified | Calculate and recommend; user performs actions in DLS |
| Tap buttons, move players, automate matches or play on the user's behalf | Prohibited boundary for this project | Do not implement |
| Access Google/Apple credentials or game login secrets | Not required and not supported | Never request or store passwords/session tokens |

## Platform-policy consideration

Google Play's AccessibilityService policy says general automation apps must keep actions narrow and clearly understood. It prohibits use of AccessibilityService to autonomously initiate, plan and execute actions or decisions, except for qualifying accessibility tools whose core purpose is assisting people with disabilities. A general DLS assistant must not claim accessibility-tool status to bypass this rule.

This audit therefore does not recommend AccessibilityService, UI tapping, overlay-based game control, or autonomous action execution. The existing MediaProjection flow is a user-started, visible, passive capture path; retain it only with clear disclosure, explicit Android permission, stop controls and no attempt to bypass capture indicators or restrictions.

## What can be built safely next

1. **Improve visible-screen understanding:** parse only clearly visible DLS text from the existing local capture/OCR stream; attach screen type, timestamp and confidence; distinguish menu, squad, player profile, settings and match screens.
2. **Create a confirmation inbox:** show extracted player names, positions, ratings and attributes as drafts. Never overwrite the local squad until the user confirms. Provide correction and duplicate detection.
3. **Make settings guidance useful:** detect/record visible settings when legible, compare them with the user's preferred profile, and provide step-by-step instructions. Do not attempt to write settings into DLS.
4. **Build a resource and development ledger:** let the user confirm visible balances, coaches, gifts and actual upgrade outcomes. Track recommendations versus completed actions separately.
5. **Add post-match visual analysis first:** analyse user-selected clips or stills with clear consent and validate event detection before describing it as automatic live match intelligence.
6. **Measure device impact:** test capture/OCR latency, battery drain, temperature, memory, frame drops and DLS responsiveness on the target Infinix Smart 9 before increasing sampling.
7. **Keep core features local-first:** no cloud uploads, paid APIs, or external account credentials unless separately disclosed and explicitly enabled.

## Approval and account-protection rules

- The user selected maximum legitimate access, with recommendations as the default action mode.
- DLS Coach AI may automatically organise local records, analyse confirmed data, calculate options, create development plans, and prepare recommendations.
- Any action inside DLS that spends currency, consumes a coach, changes a player, recruits/releases a player, claims a reward, or changes settings remains a user-performed action unless First Touch Games later documents and authorises a supported integration.
- Never ask for the DLS password, Google password, Apple password, session cookies, or authentication tokens.
- Never claim an action succeeded unless the app has reliable, permitted evidence that it succeeded.
- If First Touch Games supplies written permission or a documented public integration in future, re-audit its scope and terms before implementation.

## Sources

1. First Touch Games, [Terms of Use / End User Licence Agreement](https://firsttouchgames.com/terms-of-use), last updated 25 July 2025. Relevant sections: 1 (limited licence), 2.1.2 (cheats, exploits, automation software, bots, hacks, mods and unauthorised interference), 2.1.3 (unauthorised access), and 4.4 (enforcement, potentially including bans).
2. Google Play Console Help, [Use of the AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491). Relevant guidance: general automation must be narrow and understandable; AccessibilityService may not autonomously initiate, plan and execute actions or decisions except for qualifying accessibility tools.
3. First Touch Games Support, [Why have I been blocked from playing DLS?](https://support.ftgames.com/hc/en-us/articles/360008904718-Why-have-I-been-blocked-from-playing-DLS). FTG explains its fair-play stance and warns about modified APKs, external hacks, exploits and interference with normal progression/gameplay.
4. First Touch Games Support, [How do I backup/restore/transfer my save data?](https://support.ftgames.com/hc/en-us/articles/214387685-How-do-I-backup-restore-transfer-my-save-data). FTG describes supported profile linking for save protection; this is not a third-party API for account control.


### Prototype implementation update — 11 November 2026

The development branch now contains an initial `LiveSquadIntelligence` text-analysis prototype. It classifies a small set of visible screen labels and parses lines ending in a plausible overall rating only when the text indicates a squad or player-profile context. It persists candidate names/ratings separately and exposes a review inbox in Game Link. The user must explicitly confirm before candidates are appended to the local squad; imported positions are set to unknown and attributes are left unverified. Four JVM tests cover squad parsing, unknown-screen rejection, duplicate/attribute filtering and settings classification.

This does **not** yet provide visual card detection, reliable position/attribute extraction, resource/gift tracking, development-change detection or live tactical analysis. The parser is a conservative proof of concept; real-device OCR accuracy and performance are not yet verified. This work is on `feature/real-account-integration-audit`; `main` is preserved.

## Release gate

Do not describe DLS Coach AI as having direct account access or autonomous account management unless First Touch Games provides documented authorisation and a supported interface, and the integration passes security, privacy and real-device tests. Until then, describe it accurately as a **local-first DLS companion with user-authorised passive screen capture, on-device OCR, squad/tactical analysis and user-confirmed management records**.
