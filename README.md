# Dls-Coach-Ai
Standalone Android companion for Dream League Soccer 2026. Core features run inside this app and do not require ChatGPT, an OpenAI account, or cloud AI.

## Build
A debug APK is built by the GitHub Actions workflow in `.github/workflows/android.yml`. Current app version: 1.5.0 (version code 6).

## Current capabilities
- Local squad records and editable player profiles
- Formation and tactical planning
- Match result logging and basic trend analytics
- Role-fit starting XI estimates using confirmed positions and attributes only
- Backup export and restore
- Share squad screenshots to the app; on-device OCR creates reviewable player drafts before the user confirms any database changes
- User-authorised Android screen capture with local on-device ML Kit text recognition, visible foreground notification and stop control

## Live capture status and limitations
The live companion requests Android MediaProjection permission, creates a foreground capture service, scales frames to a maximum long edge of 960 pixels, and samples them locally. Bundled ML Kit OCR reads visible text on-device at a limited sampling rate; raw frames are not saved or uploaded and audio is not captured. OCR is not yet gameplay-event recognition and does not generate validated tactical alerts or control DLS. Capture can be stopped from the app or its persistent Android notification. Real-device behaviour, OCR accuracy, frame rate, heat, battery use and game responsiveness still require testing on the target phone.

## DLS game link and settings profile (version 1.5.0)
- The Game Link tab searches Android-visible launcher activities for an installed app labelled Dream League Soccer and opens the matching game through its normal launcher activity.
- It provides user-authorised MediaProjection screen capture controls and a local OCR status explanation.
- It stores the user's own camera, graphics/performance, controls, sound and other settings notes locally.
- The settings profile is a record and guide only. It does not silently change DLS settings, control gameplay or inspect private game data. The current OCR does not yet reliably recognise gameplay events or settings.

## Optional integrations (version 1.4.0)
- **GitHub:** the Connect screen can query the latest public GitHub Actions workflow status and open the repository/build page. This is read-only, user-triggered and does not request a GitHub login or token.
- **ChatGPT:** the user can manually share a generated coaching report through Android's share menu and select ChatGPT if installed. The app does not require ChatGPT and does not send reports automatically.
- Both integrations are optional. Core squad, tactics, match logging and local analysis remain on-device. The GitHub status check requires an internet connection; manual report sharing requires the user to choose a receiving app.

## Important limitations
OCR is fallible. Extracted player names and ratings are drafts and require user review; positions and attributes are not inferred when they are not clearly visible. Full match-video event analysis is not yet implemented. The app does not modify DLS, automate gameplay, spend in-game currency, or guarantee wins.

## Autonomous manager roadmap
The requirements, privacy rules, workflow, core screens, and staged delivery plan are documented in [AUTONOMOUS_MANAGER_SPEC.md](docs/AUTONOMOUS_MANAGER_SPEC.md).

The intended design is English-only, evidence-led, and local-first. Any future cloud AI or paid service must be opt-in and disclose costs and data handling.

## Squad-building rules
- Recruitment threshold: 80+ overall, **only when the player materially benefits the current squad**.
- Compare candidates against existing players by position, role-relevant attributes, squad weakness, depth and cost. Avoid unnecessary signings and save resources when there is no worthwhile upgrade.
- Preferred long-term development target: 97+ permanent overall **where the specific player's actual DLS 26 development limit makes it achievable**. Never promise a rating or confuse temporary Form Boosts with permanent development.
- Full recruitment, development, tactical and quality-assurance requirements are in [the final quality-squad standard](docs/AUTONOMOUS_MANAGER_SPEC.md#final-quality-squad-standard--recruitment-development-and-team-balance).
## Engineering checks
The CI workflow runs JVM unit tests for the capture-frame brightness helper before assembling the debug APK. These tests validate pixel-stride handling, row padding, invalid dimensions and short buffers; they do not replace real-device capture, battery, heat or game-performance testing.

## Player development and team trends
The Performance Centre supports dated, manually verified player-card snapshots (overall rating plus one selected attribute), comparison of first and latest records, and inclusion of development history in backups and coaching reports. Team analysis compares the latest five logged matches with the previous five when at least ten results are available. These are descriptive trends from user-entered records, not causal proof or live game telemetry.

## Tactical event pattern tracking
When recording a match, users can optionally tag observed events such as opponent counter-attacks, high-press situations, midfield losses, missed chances, goalkeeper errors, defensive errors and set pieces. The Performance Centre counts only these explicit manual tags and highlights the most frequent categories. This is a review aid, not automatic event recognition; inspect the relevant match footage and notes before changing tactics.

The tactical pattern view focuses on the latest five logged matches and, once ten matches are available, compares them with the preceding five. It reports changes in manually tagged event counts only; it does not infer that a tag caused a win or loss. Fewer than ten matches are never presented as a complete five-versus-five comparison.

## Player match contributions
Match logging optionally accepts goal scorers and assists using exact squad names, comma-separated. Repeat a name when a player scores or assists more than once in a match. The Performance Centre aggregates only these manually entered contributions; players without recorded contributions are not assumed to have played or performed poorly. Older match records remain compatible and simply have no scorer/assist data.

After ten matches, the Performance Centre also compares each player's recorded goals and assists in the latest five matches against the previous five. This is a limited contribution trend, not a complete performance rating: appearances, minutes, defensive actions and chance creation are not inferred.

## Standalone DLS connection
DLS Coach AI is designed to run independently on the phone. It does not require ChatGPT, an OpenAI account, an external AI assistant or cloud inference for its core features. The current connection to DLS is passive Android screen capture with local ML Kit OCR, using the Android permission prompt. It reads visible text only at this stage; it does not yet identify gameplay events or control the game. No official public DLS gameplay API/SDK has been verified. Any deeper integration would require documented authorisation from First Touch Games. The app will not use bots, input injection, game-memory access, reverse engineering, private endpoints or traffic interception.
