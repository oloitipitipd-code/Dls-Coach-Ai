# Dls-Coach-Ai
Android companion app for Dream League Soccer 2026.

## Build
A debug APK is built by the GitHub Actions workflow in `.github/workflows/android.yml`.

## Current capabilities
- Local squad records and player attribute entry
- Formation and tactical planning
- Match result logging
- Role-fit starting XI estimates and player-development priorities
- Shareable coaching reports

## Important limitations
The current app does not yet perform native screenshot OCR or autonomous image analysis. Its screenshot action currently shares the selected image with another app; it does not extract player data itself. The app does not modify DLS, automate gameplay, or guarantee wins.

## Autonomous manager roadmap
The requirements, privacy rules, workflow, core screens, and staged delivery plan are documented in [AUTONOMOUS_MANAGER_SPEC.md](docs/AUTONOMOUS_MANAGER_SPEC.md).

The intended design is English-only, evidence-led, and local-first. Screenshot extraction must be reviewed by the user before changing the squad database. Any future cloud AI or paid service must be opt-in and disclose its costs and data handling.
