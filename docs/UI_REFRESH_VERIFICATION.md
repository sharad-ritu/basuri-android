# Basuri 0.2 UI refresh verification

November 5, 2026. This refresh follows the [comparative research](UI_UX_RESEARCH.md) and [design plan](UI_REFRESH_PLAN.md). No automated tests were written or run. Verification uses manual interaction with the actual Android app.

## Build

`./gradlew :app:assembleDebug` succeeds with Java 17 and Android SDK 36. The delivered app is version 0.2.0, version code 2. The APK is `app/build/outputs/apk/debug/app-debug.apk`.

Final APK SHA-256: `25f96bfeab059f7603e9613456de6c5200c41918bc7e7b94de89f084fbab6ef9`. The emulator's and Samsung phone's installed APKs reported the same hash.

## Observed interface behavior

- A fresh emulator setup presents flute configuration and the daily goal as separate pages. The Save or Continue action stays visible. The final setup action opens the first lesson's guide.
- Each lesson begins with three guide pages, with back navigation, a relevant diagram, one short action cue, and optional longer guidance.
- At 360 × 800 dp with the default font scale, the first practice screen shows the target note, all six numbered finger holes, the compact horizontal meter, and both audio actions without scrolling.
- Home shows the four lesson stops and highlights the next available lesson. Saved lesson IDs and settings use the existing storage format.
- Denying microphone permission shows recovery before the practice panel. The settings action opens Android app details. Permissions and Microphone are the route to restore access. Listening does not restart automatically on returning.
- Self-guided mode is chosen once for the lesson. It removes the pitch meter, labels the microphone as off, and still asks the learner to confirm each note. The two-note Sa lesson advances without repeating the mode dialog. Completion correctly shows zero pitch-matched steps and two self-guided steps.
- The emulator's saved preferences contain the first two completed lesson IDs and two completed sessions. These values came from the manual walkthrough, not seeded progress.
- Home, free practice, and the fixed-footer self-guided lesson were inspected at system font scale 1.3. A cramped fingering legend was corrected by allowing the orientation label to wrap with space before the legend. The corrected screen keeps both labels distinct and its controls readable. The font scale was restored afterward.
- The app's microphone button exposes on/off state. Per-frame pitch values are not live announcements. Actual matches and errors retain polite announcements. A full TalkBack session was not performed.
- On the final APK, Android audio-service traces show a reference AudioTrack starting, stopping, and releasing after about 1.9 seconds. AudioRecord opens at 24 kHz mono. Silent input displays no pitch result. Backgrounding removes the active recording and audio-focus entry, and returning leaves the microphone off.
- Changing Sa from G4 to C5 in settings opened from practice returns to the practice room with C5 as the reference. The existing completed lessons remain saved.
- The final 0.2 APK was installed on the connected Samsung SM-M025F using `adb install -r`. The app reports version code 2 and version 0.2.0. Its saved preferences before and after installation are identical: onboarding completed, G4, daily goal 1, and no completed sessions or lessons. The user's phone data was never cleared. The app was launched after installation, but phone-screen visual inspection was unavailable while its screen was off.

Screenshots in `docs/screenshots/v2/` are real emulator captures from the walkthrough. Inspection progress appears in those images. The user's phone progress is separate and is not cleared by the update.

## Environment and limits

The isolated API 36 emulator stopped during verification and was restarted. A System UI unresponsive dialog appeared during its first cold boot and was dismissed with Wait. These are emulator environment observations, not a claim that Basuri crashed.

The emulator cannot initialize the host PulseAudio driver. Audible output and real bansuri pitch accuracy remain unverified on physical hardware. No successful live pitch match is claimed. The automatic stop after a successful pitch match is implemented, but that positive-match path still needs a manual instrument check. Microphone privacy switches, noisy rooms, and device-specific audio behavior also remain hardware checks.

The user explicitly requested no tests. No test sources, synthetic pitch results, automated UI test suites, or fake progress were added. The research uses first-party product material, not measured usability studies. Fingering follows the previously cited six-hole Hindustani convention and has not had an independent musician review.

## Independent review

Claude Sonnet 5.5 found no blocking findings and supported the narrower manual claims. The review led to a guide counter based on the actual list size, one match announcement instead of two, and a reference replay action after a match. These small changes were rebuilt and installed, and the final installed hashes were checked again. The post-match replay UI, like the positive-match flow itself, still needs a real instrument check. No further review round was started.

Settings continue to use an explicit Save action. Leaving with Back or another tab discards unsaved choices. The action remains pinned on screen; this behavior was retained rather than adding a separate draft and discard flow in this refresh.

The decision trail contains checkpoint entries, including retrospective verification notes. A full agent transcript was unavailable for audit. The source references are not an independent musician or usability review.
