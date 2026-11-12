# DLS Coach AI — target-device compatibility record

**Purpose:** record the device/game diagnostic supplied by the app owner and define what must be tested before calling the Android build compatible.

## Supplied diagnostic record

| Field | Supplied value |
|---|---|
| DLS version | 13430 (160 p0) |
| DLS data version | 134300 |
| DLS build label | DLS13_13430 |
| Diagnostic timestamp | 2026-11-04 14:18 (device/game record; timezone not stated) |
| Android device | INFINIX X6532 |
| Android version | 14 |
| SoC | MediaTek MT6769 |
| GPU | Mali-G52 MC2 (3) M |

The diagnostic also contained a zero-filled Android device ID. It is not a usable device identifier and is intentionally not retained here. The DLS User ID and Google account email are intentionally excluded from this project document.

## What can be inferred

- Android 14 is within the app's declared Android API support range (the project currently declares minSdk 23 and targetSdk 35).
- MediaProjection and ML Kit OCR are implemented in the development branch, but this diagnostic alone does not prove that capture works correctly on this handset.
- The MediaTek/GPU details are useful for identifying the test device; they do not establish a reliable frame rate, OCR accuracy, thermal profile or available memory.
- DLS version 13430 is a baseline snapshot only. The app must not assume that future DLS versions use the same visible UI or OCR patterns.

## Required real-device acceptance tests

Record pass/fail, app version, DLS version, test date and any observed issue for each item:

1. Install the debug APK on the Infinix X6532 running Android 14.
2. Launch the app and verify that the dashboard and all navigation destinations open without crashing.
3. Add, edit and remove a test player; close and reopen the app; confirm persistence.
4. Export a JSON backup, clear only test data if safe, restore the backup and compare the records.
5. Import a DLS squad screenshot; verify that OCR text is presented for review and that no player is added without confirmation.
6. Confirm that the app detects and opens the installed DLS launcher activity, or reports clearly when it cannot.
7. Start screen reading and confirm Android's own permission prompt and visible foreground notification.
8. Stop screen reading from the app and from the notification; confirm the capture service stops and the status updates.
9. Open DLS screens with visible text and inspect OCR output; record false readings and missed text. Do not treat OCR candidates as verified player data.
10. Run a short DLS session while capture is active; observe heat, battery drain, lag, frame drops and responsiveness. Stop capture if the phone becomes hot or gameplay performance degrades.
11. Test app behaviour after screen rotation, app backgrounding, permission denial and Android stopping the capture session.
12. Confirm no raw screen frames, audio, passwords, Google tokens or private DLS account data are saved or uploaded.

## Release status

**Compatibility status: NOT YET VERIFIED ON THE TARGET DEVICE.**

A successful GitHub Actions build and JVM test run establish automated build/test results only. They do not prove successful installation, MediaProjection operation, OCR quality, battery/thermal safety or acceptable DLS responsiveness on this Infinix device. Do not describe the app as fully tested or production-ready until the acceptance tests above have been completed and their results recorded.
