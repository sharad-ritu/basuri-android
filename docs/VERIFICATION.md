# Original 0.1 manual verification

This records the first implementation. See [UI refresh verification](UI_REFRESH_VERIFICATION.md) for the 0.2 build and current interface.

Verified on November 5, 2026. No automated tests were written or run. Verification used the actual debug app, manual ADB interaction, screenshots, saved preferences, and Android audio-service inspection.

## Build and device

- JDK 17, Android SDK platform 36, Android Gradle Plugin 8.13.2, Kotlin 2.2.21, and Gradle wrapper 8.13.
- Command: `./gradlew :app:assembleDebug`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Final APK SHA-256: `30b8a78517e9f9708bf2504e0a3711c366b860e455805234b262372fefa8fa00`. The installed `base.apk` reported the same hash. Playback, capture, and background release were checked again on this final APK after the independent review.
- A separate `Basuri_Development` API 36 emulator was created because the existing emulator was in use by another session. The app was inspected at 720 × 1600 pixels and 320 dpi, equivalent to 360 × 800 dp. Its local AVD data is ignored by Git.

## Observed behavior

- Welcome and flute setup render with readable system bars, selectable Sa note and octave, and a daily lesson goal.
- Onboarding saves real settings and begins with no completed lessons or sessions.
- The first lesson shows the three-hole Sa fingering and scrollable breath and hold instructions.
- Denying microphone permission shows recovery guidance and an Android settings action. Lesson completion stays unavailable until a pitch match or explicit self-guided confirmation.
- Granting permission starts a real mono 24 kHz AudioRecord capture. Android reports an active VOICE_RECOGNITION recording for `np.basuri.app`. Silent input shows no measured pitch and earns no match.
- Reference playback creates an AudioTrack. Android reports its start, stop, and release after about 1.9 seconds. A rapid playback-to-listening switch stops and releases the track before capture starts.
- Backgrounding removes the active recording and audio-focus entry. Returning to the app leaves the microphone off.
- Changing settings to C5 and two lessons per day survives a force-stop and cold relaunch. The target becomes 523.3 Hz for Sa.
- All four lessons were completed by manually choosing the self-guided confirmation. Completion summaries correctly showed zero microphone matches. Preferences contained all four lesson IDs and four completed sessions for the date. This is inspection data created through the UI, not seeded progress.
- Re, Ga, and the Sa–Re–Ga–Re–Sa phrase render the appropriate note and finger-hole states. The last lesson offers free practice. Lesson transitions reset the scroll position to the new target.
- The UI was inspected at system font scale 1.3. The goal counter layout was corrected to keep the count together while its title wraps.
- No app crashes appeared in the emulator crash log during the walkthrough.

## Corrections found during inspection

- Read screen state during composition so system-bar contrast updates after the dark welcome screen.
- Set the root content color explicitly to the app's indigo ink.
- Clip the compact flute illustration to its drawing area.
- Reset lesson scroll position between steps.
- Load static AudioTrack samples before checking for its fully initialized state.
- Allow the daily-goal title to wrap at larger font scales without squeezing its counter.

## Limits

The emulator could not initialize the host PulseAudio driver. Audio-service traces confirm playback and capture lifecycle behavior, but audible output and live bansuri pitch accuracy have not been checked on physical hardware. No successful microphone pitch match is claimed. Noisy-room behavior, octave errors on real flutes, microphone privacy switches, audio-focus interruptions from phone calls, and device-specific recording failures still need manual hardware checks.

The pitch matcher measures a periodic frequency near a configured reference. It cannot assess embouchure, finger placement, musical tone, timing, or whether the input came from a flute. The app explains these limits and offers self-guided continuation.

Saved history records completed lessons and daily session counts. It counts self-guided and pitch-checked completions equally. The completion screen distinguishes those steps for the current session, but the saved lesson path does not retain per-step pitch results.

The decision trail was recorded at implementation checkpoints, with some decisions logged retrospectively. No full agent transcript was available for the independent evidence review. The music content is checked against the cited teacher and maker sources, not reviewed by a musician.

Claude Sonnet 5.5 reviewed the evidence and relevant audio and progress code. It supported the documented narrower manual claims and identified the transcript gap, older audio traces, and lack of saved per-step pitch history. The final APK was then reinstalled, its installed hash checked, and its audio lifecycle inspected again. Earlier audio traces also contain the failed playback attempts that led to the static-track fix; those attempts are not successful playback evidence.

Screenshots in `docs/screenshots/` show actual emulator screens. Their progress values come from the manual walkthrough.
