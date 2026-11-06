# Basuri UI refresh

The first version gives illustration and a tuner more room than the learner's next action. This refresh uses the [comparative research](UI_UX_RESEARCH.md) to make the lesson, instrument, and feedback share the screen.

## Visual direction

Keep bamboo gold and indigo as Basuri's identity. Use a rounded bold display face, ordinary Android sans for instructions, and a restrained scale: 28 sp screen titles, 20 sp section titles, and 16 sp instructions. Replace the fixed normal-weight rounded typeface with a bold face so headings have the intended hierarchy.

| Token | Value | Role |
| --- | --- | --- |
| Ink | #222D50 | Note names and text |
| Indigo | #4C5FB5 | Current lesson and main actions |
| Bamboo | #E7BB72 | Instrument drawing and small accents |
| Paper | #F7F8FC | Page background |
| Mist | #EBEEF8 | Quiet tracks and secondary choices |
| Green | #236957 | Measured matches and completed practice |

## Layout and behavior

```
Learn                   Lesson                  Practice
basuri          Sa G4   close   progress         Practice room
[daily goal, compact]   One physical action     [Sa] [Re] [Ga]
Your first notes        [relevant diagram]      [note + fingering]
1 Current lesson        Short instruction       [compact meter]
  [Start lesson]        [More guidance]         [feedback + hold]
2 Hold Sa               [Continue]              [Hear] [Listen]
3 Re and Ga                                     [Help / pitch details]
4 Small melody
```

The home path must begin on the first screen. Only the current lesson gets a filled panel. Instructions become three small guide pages, each with one action and a diagram. Longer explanations stay available in a sheet.

The practice panel uses a numbered bamboo strip and a shallow horizontal meter. The listening action stays in the lesson footer. It never depends on a disabled Continue button to explain the task. A real match stops capture and enables the next note. Silence shows no measurement.

Self-guided practice becomes an explicit session mode chosen once per lesson. The learner still confirms each practiced note. It earns no microphone result and is labeled in the footer and completion summary. Existing saved progress and settings remain compatible.

Setup separates choosing the flute from choosing a daily goal. Settings keep Save visible and move explanations into contextual help. Completion prioritizes the actual lesson result and the next action.

## Critique before implementation

Shrinking the old hero alone would leave the paragraph lesson and detached tuner unchanged. The revised plan changes those interactions as well. A decorative winding path would spend space without adding meaning to four lessons, so keep a clear connected sequence. Borrow task placement from the reference apps, not their mascots, colors, rewards, or branding.

Keep 48 dp secondary targets and 56 dp main actions. Let text wrap and content scroll at larger font settings. Use text and shape alongside color, and announce stable status changes rather than every measured frame. No automated tests will be created or run.
