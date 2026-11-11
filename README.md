# Dls-Coach-Ai
Android companion app for Dream League Soccer 2026.

## Build
A debug APK is built by the GitHub Actions workflow in `.github/workflows/android.yml`.

## Current capabilities
- Local squad records and editable player profiles
- Formation and tactical planning
- Match result logging and basic trend analytics
- Role-fit starting XI estimates using confirmed positions and attributes only
- Backup export and restore
- Share squad screenshots to the app; on-device OCR creates reviewable player drafts before the user confirms any database changes
- User-authorised Android screen-capture foundation for live sessions, with a visible foreground notification and stop control

## Live capture status and limitations
The capture foundation requests Android MediaProjection permission, creates a foreground capture service, scales frames to a maximum long edge of 960 pixels, and closes sampled frames immediately. Capture is local-only: this stage does not save or upload frames, record audio, recognise gameplay events, generate live tactical advice, or control DLS. Capture can be stopped from the app or its persistent Android notification. Real-device behaviour, frame rate, heat, battery use and game responsiveness still require testing on the target phone.

## Important limitations
OCR is fallible. Extracted player names and ratings are drafts and require user review; positions and attributes are not inferred when they are not clearly visible. Match footage sharing is not equivalent to in-app video analysis. The app does not modify DLS, automate gameplay, spend in-game currency, or guarantee wins.

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
