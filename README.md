# Basuri

A native, offline Android app for learning the six-hole Nepali basuri (बाँसुरी) day to day. Built with Kotlin and Jetpack Compose. It includes:

- a 20-lesson course in six units, from a first sound to overblown high Sa and short tunes
- a daily riyaz checklist, streaks, XP, and achievements
- long-tone warm-ups and note practice with microphone pitch feedback
- a tanpura drone and a taal-aware metronome
- a sargam notation guide with play-along examples
- local practice reminders

All progress is saved on the device.

## Build the debug APK

Use JDK 17 and an Android SDK with platform 36 and compatible build tools. The Gradle 8.13 wrapper is included. Gradle downloads dependencies on the first build.

1. Set `JAVA_HOME` to your JDK 17 installation if needed.
2. Set the SDK path in the untracked `local.properties` file:

   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```
3. Build the app:

   ```sh
   ./gradlew :app:assembleDebug
   ```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`. This command does not run tests. No automated tests are included, as requested.

## Install and open

With a device or emulator connected:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n np.basuri.app/.MainActivity
```

Android 8.0 or later is required. Allow microphone access when you choose **Start listening**. If you deny access, you can read the instructions, hear reference tones, and mark an exercise as self-guided. Android app settings can restore microphone permission.

If multiple devices are connected, add `-s <serial>` after `adb`. Use `adb devices` to find the serial.

## Use the app day to day

Set the actual note and scientific octave of Sa with the three finger holes nearest the blowing hole closed. Choose **Detect my Sa** and play that note to let the microphone set the note, octave, and a fine tuning in cents, or choose them yourself. The default G4 is a starting configuration, not a measurement of your flute.

- **Learn** opens with **Today's riyaz**: a long-tone warm-up, today's lesson, and two minutes with the metronome. Each item opens the right screen. Below it are your streak, level, and the learn path in six units. Each lesson unlocks the next. Completed lessons can be repeated, and after the last one, today's item becomes a rotating review.
- **Practice** has two modes. **Find a note** matches one note. **Long tones** times how long you hold a note within 50 cents. A hold of 5 seconds counts as the day's warm-up, marked with a vibration and a TalkBack announcement. **Warm up** from Today's riyaz always starts on Sa. Only notes you have reached on the learn path are offered.
- **Tools** has a tanpura drone at your Sa and a metronome with simple meters and taal presets. They can play together. Only metronome time counts toward the daily plan. The session timer starts at 2 minutes, the length of the daily rhythm task, and each tab keeps its settings while you switch away.
- **Notation** explains Bhatkhande sargam and plays its read-along examples.
- **My flute** holds your Sa and its fine tuning, daily goal, reminders (daily at a preset or custom time, a 21:30 streak saver, and a Saturday evening summary of the Sunday-to-Saturday week), and a reset option.

A microphone pitch match requires 1.5 seconds within 35 cents of the selected reference. A flicker of under about 200 ms pauses the timer instead of resetting it. Choose **Practice without microphone** once to use self-guided mode for that lesson, then confirm each practiced note. Either kind of completed lesson counts toward the daily goal and the streak. The app stores progress and settings in private SharedPreferences. It does not save audio or upload data, and it requests no internet permission. Uninstalling clears progress.

For pitch checking, use a quiet room and play one note at a time, with the tanpura off. The reference stops before capture starts. Leaving the foreground stops all audio. While audio plays or the microphone listens, the screen stays on so it doesn't time out mid-practice.

Notifications are local, inexact alarms, so they need no exact-alarm permission and may arrive a few minutes late. The daily reminder offers **Start lesson**, **Warm up**, and **In 1 hour**. The app asks for notification permission on Android 13 and later.

Shuddh Ma is not taught yet. On a six-hole bansuri it needs a partly covered hole 1, and the fingering diagrams cannot show half-holes. Opening all six holes sounds tivra Ma.

See [music and audio notes](docs/MUSIC_AND_AUDIO.md) for tuning assumptions and limitations, [design](docs/DESIGN.md) for visual choices, and [UI refresh verification](docs/UI_REFRESH_VERIFICATION.md) for the build and emulator findings.

The 0.2 UI refresh follows [research into comparable learning apps](docs/UI_UX_RESEARCH.md). The previous 0.1 walkthrough remains in [the original verification notes](docs/VERIFICATION.md).
