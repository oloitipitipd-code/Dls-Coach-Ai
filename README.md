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
Version 1.1.0 adds on-device OCR for squad screenshots. The app displays extracted text for review and can share that text. OCR does not yet convert the text into structured player records automatically, and it does not provide full AI image analysis. The app does not modify DLS, automate gameplay, or guarantee wins.

## Autonomous manager roadmap
The requirements, privacy rules, workflow, core screens, and staged delivery plan are documented in [AUTONOMOUS_MANAGER_SPEC.md](docs/AUTONOMOUS_MANAGER_SPEC.md).

The intended design is English-only, evidence-led, and local-first. Screenshot extraction must be reviewed by the user before changing the squad database. Any future cloud AI or paid service must be opt-in and disclose its costs and data handling.
