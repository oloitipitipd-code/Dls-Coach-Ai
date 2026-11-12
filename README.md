# DLS Coach AI

**Standalone Android companion for Dream League Soccer 2026.** Version **1.8.0** (version code 9) on the `feat/autonomous-manager-permissions` development branch.

Core squad, tactical, match, development and manager functions run locally. The app does not require ChatGPT, an OpenAI account, a cloud AI service or a paid subscription for these features.

## What is included

- **Squad manager:** add, edit and remove players; record position, OVR and the ten visible card attributes; inspect squad depth and data completeness.
- **Screenshot import:** share one or more DLS screenshots to the app or choose an image; on-device ML Kit OCR proposes possible name/rating records for manual review. Nothing is added until confirmation.
- **Tactical centre:** choose a supported formation and playing style; receive a role-fit starting XI and bench suggestions based only on entered positions and attributes.
- **Match centre:** record wins/draws/losses, goals for/against, scorers, assists, notes and manually observed tactical events.
- **Performance centre:** review recent scoring trends, five-match comparisons (when ten matches exist), manually tagged events, player contributions and dated player-development snapshots.
- **Autonomous manager policy:** full autonomy for local, reversible squad assessments, lineup/tactical refreshes, development priorities and match summaries; purchases, spending, account operations and irreversible actions remain approval-gated and manual in DLS.
- **Manager dashboard:** record user-confirmed resource snapshots; approve, dismiss or mark coaching recommendations as tested; review decision history.
- **Game link:** find and launch an installed DLS app through Android's normal launcher; keep local notes about camera, graphics, controls and sound settings.
- **Authorised screen reading:** Android MediaProjection permission, foreground notification and stop control; sampled frames are processed locally with bundled ML Kit OCR. When you open DLS squad/player screens, the app can identify possible visible player-name/rating text; it does not yet extract a complete roster or all card attributes reliably.
- **Backup and recovery:** export and restore JSON backups. Schema versions 1–3 are accepted; version 3 includes player development and manager data.
- **Online AI chat:** an optional cloud-connected coach can discuss saved squad and match records, compare formations, choose an XI and explain decisions. The documented setup uses a provider free tier where available, but quotas and terms can change; no paid fallback is configured. No provider key is embedded in the APK.

## Accuracy and safety boundaries

This is a companion app, not an official First Touch Games product.

- No verified public DLS gameplay API or SDK has been established for this project. The app does not directly authenticate to your DLS account or read private account inventory. Internet access connects the app to its AI service, not to private DLS account data.
- The account checkbox is only your local confirmation; it is not DLS authentication. Keep Google/Apple sign-in and profile transfer inside the official game.
- The app does not automate taps, gameplay, purchases, upgrades, reward claims or settings changes. It does not inspect game memory/files, intercept traffic, use private endpoints or modify the game.
- The online AI uses records already saved in the app. It cannot independently discover the full DLS roster, live coins/gems, transfers or match state. No screenshot upload is required for AI chat. The existing user-authorised live screen reader can inspect visible text while you navigate DLS screens, but it currently produces only tentative name/rating candidates and cannot guarantee complete player-card extraction. A complete automatic roster sync requires an authorised integration from First Touch Games.
- Live OCR is text recognition, not validated player-card vision, ball tracking, score/event detection or tactical interpretation. It does not yet analyse match video.
- Resource balances and development snapshots are manually entered. Marking a recommendation as tested records your statement; it does not verify an in-game outcome.
- Coaching scores are estimates, not official DLS formulas. Recommendations cannot guarantee wins or permanent player-rating gains.

## Privacy and performance

Screen reading starts only after the Android system permission prompt. Frames are sampled and processed on-device; raw frames are not saved or uploaded, and audio is not captured. A persistent notification exposes the active session and a stop action. OCR text and user-entered records are stored locally in app preferences. Exported backups are files you control.

The target device for practical testing is an entry-level Android phone. Capture frame rate, OCR accuracy, heat, battery use and game responsiveness must still be checked on the actual phone. Stop capture if the device becomes hot or DLS performance degrades.

## Build and validation

GitHub Actions runs:
1. JVM unit tests
2. Android `lintDebug`
3. Debug APK assembly
4. Debug APK artifact upload

The debug APK is the installable test build. A green CI run verifies those automated checks only; it does not prove real-device behaviour, DLS compatibility or account integration.

## Install

1. Download the `DLS-Coach-AI-debug` artifact from the successful workflow run.
2. Extract the ZIP on Android.
3. Open `app-debug.apk` and allow installation from Files if Android asks.
4. If Android reports a signature conflict with an older CI build, export your app backup first. Different GitHub runners can generate different debug signing keys. Uninstall the old build only after you have secured your backup, then install and restore it.
5. Confirm version 1.8.0 and test ordinary app functions before enabling screen capture.

## Project documents

- [Real-account integration and safety audit](docs/REAL_ACCOUNT_INTEGRATION_AUDIT.md)
- [Target-device compatibility record and release checklist](docs/DEVICE_COMPATIBILITY.md)
- [Autonomous manager requirements and quality-squad standard](docs/AUTONOMOUS_MANAGER_SPEC.md)

## Development status

The `feat/autonomous-manager-permissions` branch builds on the 1.7.0 release candidate and adds the selected autonomous-manager permission policy as version 1.8.0. It is ahead of the repository's `main` branch; use the pull request to review and validate it before merging. Do not treat a successful CI build as a phone-tested release. Remaining real-device checks include install/upgrade, data persistence, backup restore, screenshot import, DLS launcher detection, capture permission/stop behaviour, OCR quality and resource use on the target phone.


## Online AI setup

The optional cloud AI gateway is in `cloud/`. Follow [cloud/README.md](cloud/README.md) to deploy it and configure a server-side Gemini API key on the provider's free tier where available. Do not attach billing if you want to keep the setup free. The provider receives chat text and saved squad/match context when you send a message.
