# Basuri design

Basuri uses the material and fingering of a bamboo flute as its visual identity. Indigo gives the current lesson and main action emphasis. Bamboo gold belongs to the instrument and small accents. Green marks completed practice and actual pitch matches.

The [UI refresh plan](UI_REFRESH_PLAN.md) records the layout choices and critique. The [research note](UI_UX_RESEARCH.md) cites first-party references from Yousician, Simply Piano, Duolingo Music, TonalEnergy, and Android accessibility and permission guidance. These are references for task placement and feedback, not copied branding.

## Tokens

| Role | Value |
| --- | --- |
| Ink, note names and text | `#222D50` |
| Indigo, current lesson and main actions | `#4C5FB5` |
| Bamboo, instrument and accent | `#E7BB72` |
| Paper, page background | `#F7F8FC` |
| Mist, quiet tracks and secondary choices | `#EBEEF8` |
| Green, completed practice and measured matches | `#236957` |

Rounded bold system sans carries headings and note names. Android sans carries instructions and controls. Screen titles are 28 sp, section titles are 20 sp, and ordinary instructions are 16 sp. Guide cues use 19 sp to be readable while holding a flute. Main actions have at least 56 dp height; secondary controls have at least 48 dp touch targets. Text can wrap, and content remains scrollable at larger system font settings.

## Screen structure

Home places a compact daily goal above the four-stop beginner path. Only the current lesson gets a filled panel and a Start lesson action. Completed lessons offer repeat access. The path reports completion, not musical mastery.

Setup separates choosing Sa from choosing a daily goal. It explains the three-hole convention and scientific octave, and labels the reference as chosen rather than detected. Save stays visible in settings, while supporting explanations remain available in a sheet.

Each lesson begins with three short guide pages. A page contains one physical task, a relevant diagram, a short cue, and optional longer guidance. Back returns to the previous instruction. Starting practice places the target note, fingering strip, and compact horizontal pitch meter together. The lesson footer keeps Hear and Start listening accessible.

Self-guided mode is chosen once per lesson. It hides the pitch meter, labels the microphone as off, and asks the learner to confirm each practiced note. It never earns a microphone result. The completion screen separates pitch-matched and self-guided steps for that session. Saved history counts both modes equally and does not retain individual pitch results.

## Feedback and accessibility

The tuner draws a measured marker only when a reliable input frequency exists. Silence is not shown as a correct note. A real match stops capture and enables the next note. Permission and capture errors provide recovery before the meter, while reference playback and self-guided practice remain available.

Open and closed holes use shape, fill, numbering, and a TalkBack summary. Selections have explicit selected state. The listening control exposes microphone state. Important results and errors can be announced, while rapidly changing pitch-frame values are not live announcements. Color changes respond to actual state changes; there is no fabricated waveform or accuracy score.

## Streaks, XP, and achievements (0.3)

Rewards come only from real, saved actions. A streak counts consecutive days with at least one completed lesson. It is shown in the home header, in a streak card with a seven-day strip, and on the completion screen when a lesson keeps today's streak. Ember orange (`#D9692B`) belongs to the streak flame alone. A streak freeze is earned every seven streak days, up to two. It covers one missed day automatically and is drawn in frost blue (`#3F7FB8`), so it is never mistaken for practice.

XP rewards finishing a lesson (10), a first completion (20), each pitch-matched note (5), each self-guided note (2), the first lesson of the day (10), and reaching the daily goal (15). A pitch match earns more than a self-guided note because it was measured. Levels need 50 XP, then 10 XP more for each level after that. Achievements are calculated from saved progress, so they always agree with it. Leaving a lesson early warns when doing so would leave today's streak open. Tapping the streak card opens a month calendar with stats for current and best streak, practice days, lessons, metronome minutes, saved freezes, and total XP. Metronome sessions of ten seconds or more add to a **Steady pulse** achievement, but they never keep a lesson streak. Progress saved by 0.2 receives the base and first-completion XP it would have earned, and its longest streak is recalculated from session history.

## Rhythm and Notation (0.3)

**Tools** (formerly Rhythm) has a tanpura drone and a metronome. The metronome runs from 30 to 240 BPM, with tap tempo, simple meters, and the taal presets in `Taals.kt`. Sam, tali, khali, ordinary beats, and subdivisions each have their own synthesized click. Clicks are placed by sample position in a streaming `AudioTrack`, and the beat display reads the playback head. Playback stops when the tab closes, when the app leaves the foreground, or when another app takes audio focus.

**Notation** teaches Bhatkhande-style sargam. It draws the komal, tivra, and octave marks itself, lays phrases out on shared taal grids, shows each swar's pitch on the learner's flute, and gives a six-hole fingering chart that only includes fingerings it can state with confidence. Each read-along example has a **Play along** control at 50, 70, or 90 BPM. It plays synthesized reference tones at the learner's Sa over soft taal clicks, and highlights the sounding beat from the audio playback head. Only one example plays at a time. Playback stops at the end of the phrase, when the tab closes, when the app leaves the foreground, and when audio focus is lost.

## Daily reminder (0.3)

**My flute** has an optional daily reminder, with morning, midday, evening, and night presets. It saves as soon as it changes, because turning it on may open Android's notification permission prompt. It uses an inexact `AlarmManager` window, so it needs no exact-alarm permission and may arrive a few minutes late. It skips days that already have a completed lesson. It is rescheduled after a reboot, an app update, or a clock or time zone change. Nothing goes over the network.

## Daily use (0.4)

Learn opens with what to do today. **Today's riyaz** is a three-item checklist: a long-tone warm-up (5 seconds within 50 cents in Practice), the daily lesson goal, and two minutes of metronome. Each item opens its screen, and a full day counts toward the **Full riyaz** achievement. The streak card follows, with a calendar icon for the calendar and stats. The learn path then groups lessons into collapsible units with progress rings, with the current unit open. The home screen shows three achievements, earned ones first and then the closest to being earned, and "See all" opens the full grid in a sheet.

Every tab uses the same header: a 28 sp title, a one-line muted subtitle, and 20 dp gutters. Cards are white with a 20 dp radius and a hairline border. Every Material color role maps to a Basuri token, so default chips, sliders, and switches never fall back to Material lavender. Indigo marks the selected control and the main action. Ember belongs to the streak flame, and frost blue to streak freezes.

Notes carry their octave as in Bhatkhande notation: a dot below for mandra and above for taar, drawn by `SwarLabel`. TalkBack hears "low Pa" or "high Sa". The fingering diagram adds an **Overblow** or **Low octave** tag. Shuddh Ma is left out until half-hole diagrams exist.

Notifications use three channels (daily reminder, streak saver, weekly summary), so people can mute each one in Android settings. The daily reminder never fires on a day that already has a lesson. The completion screen offers a reminder only once, after a lesson. Changing a setting re-arms every alarm, and so do app start, reboot, update, and clock changes.
