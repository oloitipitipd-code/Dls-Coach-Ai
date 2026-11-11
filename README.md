# DLS Coach AI

**Standalone Android companion for Dream League Soccer 2026.** Version **1.7.0** (version code 8) on the `feature/real-account-integration-audit` development branch.

Core squad, tactical, match, development and manager functions run locally. The app does not require ChatGPT, an OpenAI account, a cloud AI service or a paid subscription for these features.

## What is included

- **Squad manager:** add, edit and remove players; record position, OVR and the ten visible card attributes; inspect squad depth and data completeness.
- **Screenshot import:** share one or more DLS screenshots to the app or choose an image; on-device ML Kit OCR proposes possible name/rating records for manual review. Nothing is added until confirmation.
- **Tactical centre:** choose a supported formation and playing style; receive a role-fit starting XI and bench suggestions based only on entered positions and attributes.
- **Match centre:** record wins/draws/losses, goals for/against, scorers, assists, notes and manually observed tactical events.
- **Performance centre:** review recent scoring trends, five-match comparisons (when ten matches exist), manually tagged events, player contributions and dated player-development snapshots.
- **Manager dashboard:** record user-confirmed resource snapshots; approve, dismiss or mark coaching recommendations as tested; review decision history.
- **Game link:** find and launch an installed DLS app through Android's normal launcher; keep local notes about camera, graphics, controls and sound settings.
- **Authorised screen reading:** Android MediaProjection permission, foreground notification and stop control; sampled frames are processed locally with bundled ML Kit OCR.
- **Backup and recovery:** export and restore JSON backups. Schema versions 1–3 are accepted; version 3 includes player development and manager data.
- **Optional integrations:** read the public GitHub Actions status and manually share a coaching report using Android's share sheet. Neither is required for core app use.

## Accuracy and safety boundaries

This is a companion app, not an official First Touch Games product.

- No verified public DLS gameplay API or SDK has been established for this project. The app does not directly authenticate to your DLS account or read private account inventory.
- The account checkbox is only your local confirmation; it is not DLS authentication. Keep Google/Apple sign-in and profile transfer inside the official game.
- The app does not automate taps, gameplay, purchases, upgrades, reward claims or settings changes. It does not inspect game memory/files, intercept traffic, use private endpoints or modify the game.
- OCR can misread text. Screenshot and live-capture candidates are drafts; verify names and ratings before importing. Positions and attributes are not inferred when they are not clearly available.
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
5. Confirm version 1.7.0 and test ordinary app functions before enabling screen capture.

## Project documents

- [Real-account integration and safety audit](docs/REAL_ACCOUNT_INTEGRATION_AUDIT.md)
- [Autonomous manager requirements and quality-squad standard](docs/AUTONOMOUS_MANAGER_SPEC.md)

## Development status

The feature branch contains the 1.7.0 manager and live-squad prototype. It is not the same as the repository's `main` branch. Do not treat a successful CI build as a phone-tested release. Remaining real-device checks include install/upgrade, data persistence, backup restore, screenshot import, DLS launcher detection, capture permission/stop behaviour, OCR quality and resource use on the target phone.
