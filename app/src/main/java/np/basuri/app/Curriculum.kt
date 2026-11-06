package np.basuri.app

/** The Learn path. Lesson ids are saved in progress, so never rename an existing id. */
val courses = listOf(
    Course(1, "Your first notes", "Sa, Re, and Ga"),
    Course(2, "Breath and hands", "Steady notes and small movements"),
    Course(3, "Below Sa", "Low Ni, Dha, and Pa"),
    Course(4, "A brighter register", "Overblow to Pa, Dha, Ni, and high Sa"),
    Course(5, "Climbs and patterns", "Exercises with the notes you know"),
    Course(6, "Your first tune", "Two original folk-style phrases"),
)

// All-open is tivra Ma, not shuddh Ma. Shuddh Ma needs partial coverage of hole 1,
// which Swar.closed cannot represent. Keep Ma out of this path until both the model
// and diagrams support it. Sources: https://bansuri.com/learn-bansuri/swaras-symbols/
// and https://rangayyan.ca/WebsiteMusic/Bansuri1.pdf (fingering charts, pages 3–4).
// The later climbs deliberately omit Ma and are not described as a complete scale.
val lessons = listOf(
    Lesson("sound", "Your first sound", "Find a comfortable breath", 3, listOf(
        Guide("Get comfortable", "Support the flute with both hands. Keep your shoulders relaxed.",
            "Sit tall without stiffening. Your nearer hand covers holes 1–3, and the other hand supports the far end. Mirror the hold if you play to the left. Keep your grip gentle.", GuidePicture.Flute),
        Guide("Blow across the edge", "Send a small, gentle stream of air across the far edge of the blowing hole.",
            "Rest the blowing hole below your lower lip, as if blowing across a bottle. Explore with the finger holes open. Roll the flute a little toward or away from you until a tone appears. Rest between breaths. The app cannot judge your embouchure.", GuidePicture.Breath),
        Guide("Now find Sa", "Close holes 1, 2, and 3 with your finger pads. Leave the other holes open.",
            "Count from the blowing hole, not the open end. Cover gently without squeezing. A first sound may be airy. You can practice self-guided if you are still finding a tone.", GuidePicture.Fingers(Swar.Sa))
    ), listOf(Swar.Sa), "A first sound may be airy. Rest between breaths and try again gently.", unit = 1),
    Lesson("sa", "Make Sa your home", "Listen, then hold one note", 3, listOf(
        Guide("Cover the nearest three", "Close holes 1, 2, and 3. Keep your hands relaxed and leave holes 4–6 open.",
            "Use your finger pads to cover each hole. Support the flute without squeezing. Numbering begins at the blowing hole.", GuidePicture.Fingers(Swar.Sa)),
        Guide("Listen before you play", "Hear Sa once. Let the reference stop, then hum the note softly.",
            "Your reference follows the Sa note and octave in My flute. It is a synthesized pitch guide. Basuri stops playback before it opens the microphone. Hum only if it feels comfortable, then try the note on your flute.", GuidePicture.Flute),
        Guide("Let the note settle", "Play Sa for a comfortable breath. Aim to stay near the reference for 1.5 seconds.",
            "The center of the meter is your target. A match requires 1.5 seconds within 35 cents. If your comfortable three-hole Sa sounds an octave away, check your Sa setting. Do not force your breath to satisfy the meter.", GuidePicture.Fingers(Swar.Sa))
    ), listOf(Swar.Sa, Swar.Sa), "Keep your sound comfortable. A steady note matters more than a long breath.", unit = 1),
    Lesson("rega", "Meet Re and Ga", "Lift one finger at a time", 4, listOf(
        Guide("Begin with Sa", "Cover the three holes nearest the blowing hole. This is your starting position.",
            "Keep holes 4–6 open throughout this lesson. Sa is the reference for the notes that follow, and its sounding pitch depends on your flute.", GuidePicture.Fingers(Swar.Sa)),
        Guide("Lift into Re", "Lift the finger on hole 3. Keep holes 1 and 2 covered.",
            "This is shuddh Re, a whole step above Sa. Listen to the reference before trying it. Move only the finger on hole 3 and keep the other holes covered gently.", GuidePicture.Fingers(Swar.Re)),
        Guide("Then lift into Ga", "Lift the finger on hole 2. Only hole 1 stays covered.",
            "This is shuddh Ga, a whole step above Re. Keep your support comfortable as you uncover the holes. Take a new breath between notes whenever you need one.", GuidePicture.Fingers(Swar.Ga))
    ), listOf(Swar.Re, Swar.Ga), "Re closes two holes. Ga closes one. Move slowly and listen to each note.", unit = 1),
    Lesson("changes", "A small melody", "Sa, Re, Ga, Re, Sa", 4, listOf(
        Guide("Follow five notes", "Play Sa, Re, Ga, Re, Sa. Lift one finger at a time, then cover the holes again.",
            "Keep holes 4–6 open throughout. The phrase uses only Sa, shuddh Re, and shuddh Ga. It is a short beginner exercise, not a complete raga.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.Re, Swar.Ga, Swar.Re, Swar.Sa), "3 closed   2   1   2   3 closed")),
        Guide("Give each note space", "Hold each note for a comfortable breath. You can breathe between notes.",
            "This first exercise checks each pitch separately. It does not measure rhythm or the smoothness of your changes. Smooth transitions can come later.", GuidePicture.Breath),
        Guide("Return to Sa", "Cover holes 1, 2, and 3 again. Listen for the return to your starting note.",
            "Move only the fingers you need. Keep your shoulders relaxed. Returning to a comfortable Sa matters more than playing the phrase quickly.", GuidePicture.Fingers(Swar.Sa))
    ), listOf(Swar.Sa, Swar.Re, Swar.Ga, Swar.Re, Swar.Sa), "Take your time. A relaxed return to Sa matters more than speed.", unit = 1),

    Lesson("steady", "One steady breath", "Start softly and finish comfortably", 4, listOf(
        Guide("Settle your hold", "Bring the flute to your lip without raising your shoulders.",
            "Begin a practice day with a familiar Sa. Spend a few minutes on one lesson, then repeat an earlier one. Stay with a lesson for several days if you need to. The suggested minutes are practice time, not a deadline.", GuidePicture.Flute),
        Guide("Keep Sa even", "Play Sa with an even stream of air for about three seconds.",
            "Take an ordinary, comfortable breath first. End the note before you run out of air. Listen for an even sound as well as a steady pitch. The microphone checks pitch only, so it cannot tell you whether the tone is clear.", GuidePicture.Breath),
        Guide("Rest before the repeat", "Lower the flute and take a few normal breaths before playing again.",
            "Practice two short Sa notes with a rest between them. You do not need to hold the longest possible note. Stop and rest if you feel lightheaded. A comfortable start is enough for today.", GuidePicture.Flute)
    ), listOf(Swar.Sa, Swar.Sa), "Begin each day with a familiar note. Repeat this lesson on another day before moving on if Sa still feels uncertain.", unit = 2),
    Lesson("lightfingers", "Light fingers", "Keep each finger near its hole", 4, listOf(
        Guide("Move without blowing", "Silently change from Sa to Re and back. Lift the finger on hole 3 only a little.",
            "Keep your thumbs in a comfortable supporting position. The finger should clear the hole fully without stretching away from the flute. Pause if your grip tightens.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.Re, Swar.Sa), "3 closed   2   3 closed")),
        Guide("Add the second lift", "Silently change from Re to Ga and back. Keep hole 1 sealed.",
            "Only the finger on hole 2 moves here. Keep holes 3–6 open. These small movements prepare you for longer phrases without asking you to play faster.", GuidePicture.Fingers(Swar.Ga)),
        Guide("Play the return", "Play Sa, Re, Ga, Re, Sa with your fingers close to the holes.",
            "Let each note settle before changing. Breathe between notes whenever you need to. After the pitch steps, try the phrase once more at your own pace and listen for gaps in the sound.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.Re, Swar.Ga, Swar.Re, Swar.Sa), "3 closed   2   1   2   3 closed"))
    ), listOf(Swar.Sa, Swar.Re, Swar.Ga, Swar.Re, Swar.Sa), "Small finger movements are enough. Repeat yesterday's lesson before adding a new one.", unit = 2),

    Lesson("lowni", "Step below Sa", "Close one more hole for low Ni", 4, listOf(
        Guide("Cover hole 4", "From Sa, close hole 4. Keep holes 1–3 covered and holes 5–6 open.",
            "Use the nearest finger of your farther hand for hole 4. Four holes closed gives low Ni with a gentle stream of air. This is shuddh Ni in the mandra saptak, the register below your home Sa.", GuidePicture.Fingers(Swar.MandraNi)),
        Guide("Soften the air", "Play low Ni with a slower, warmer-feeling stream of air.",
            "Keep the air continuous. If the note jumps up, ease the air speed a little. If the tone disappears, check the seal on hole 4 before trying more breath. The app cannot identify an air leak or judge your hand position.", GuidePicture.Breath),
        Guide("Come home to Sa", "Play Sa, low Ni, Sa, low Ni, Sa. Lift only the finger on hole 4 to return.",
            "Low Ni is one semitone below Sa, so this is a small pitch change. Listen to both references. Keep your Sa setting unchanged as you explore lower notes.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.MandraNi, Swar.Sa, Swar.MandraNi, Swar.Sa), "3 closed   4   3   4   3 closed"))
    ), listOf(Swar.Sa, Swar.MandraNi, Swar.Sa, Swar.MandraNi, Swar.Sa), "Low Ni closes four holes. Use gentle air and return to Sa whenever you need to reset.", unit = 3),
    Lesson("lowdha", "A little lower", "Add hole 5 for low Dha", 4, listOf(
        Guide("Cover hole 5", "From low Ni, close hole 5. Leave only hole 6 open.",
            "Keep holes 1–4 sealed as the next finger comes down. Five holes closed gives shuddh low Dha. Let the finger pad cover the hole without pressing harder than you need to.", GuidePicture.Fingers(Swar.MandraDha)),
        Guide("Keep the low sound", "Play low Dha with the gentle air you used for low Ni.",
            "The note may need a small change in air speed or angle. If the sound becomes thin or jumps up, pause and check the hole seals. Do not push harder to make a low note speak.", GuidePicture.Breath),
        Guide("Return in steps", "Play Sa, low Ni, low Dha, low Ni, Sa.",
            "Keep holes 1–3 covered throughout. Close hole 4, then hole 5. Lift them in the opposite order to return. Take a separate breath for each pitch step if that is more comfortable.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.MandraNi, Swar.MandraDha, Swar.MandraNi, Swar.Sa), "3 closed   4   5   4   3 closed"))
    ), listOf(Swar.Sa, Swar.MandraNi, Swar.MandraDha, Swar.MandraNi, Swar.Sa), "Check one hole at a time if low Dha is difficult. Repeat low Ni first, then add hole 5.", unit = 3),
    Lesson("lowpa", "All six covered", "Find low Pa and return to Sa", 5, listOf(
        Guide("Cover the last hole", "From low Dha, close hole 6 so that all six finger holes are covered.",
            "This is low Pa, the lowest note in this course. Keep your wrists comfortable. If you cannot reach hole 6 without strain, ask a teacher about your hold or flute size. Squeezing harder does not fix a gap.", GuidePicture.Fingers(Swar.MandraPa)),
        Guide("Let low Pa speak", "Play low Pa with a slow, gentle stream of air.",
            "A complete seal matters on every hole. Try a small adjustment of the flute angle if needed. Keep the first low Pa short. This same fingering will later give a higher Pa with a faster stream of air.", GuidePicture.Breath),
        Guide("Walk back to Sa", "Play Sa, low Ni, low Dha, low Pa, low Dha, low Ni, Sa.",
            "Close holes 4, 5, and 6 in order on the way down. Lift holes 6, 5, and 4 to return. Repeat this gentle descent on several practice days before trying overblowing.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.MandraNi, Swar.MandraDha, Swar.MandraPa, Swar.MandraDha, Swar.MandraNi, Swar.Sa), "3 closed   4   5   6   5   4   3 closed"))
    ), listOf(Swar.Sa, Swar.MandraNi, Swar.MandraDha, Swar.MandraPa, Swar.MandraDha, Swar.MandraNi, Swar.Sa), "Low Pa needs all six holes sealed. Short, comfortable notes are enough. Rest before repeating the descent.", unit = 3),

    Lesson("pa", "The next Pa", "Same six holes, a higher sound", 5, listOf(
        Guide("Begin with low Pa", "Close all six holes and play a comfortable low Pa.",
            "Keep this fingering for the whole lesson. The new Pa is in the madhya saptak, the same named register as your home Sa. It is one octave above low Pa and seven semitones above home Sa.", GuidePicture.Fingers(Swar.MandraPa)),
        Guide("Focus the air", "Make the opening between your lips a little narrower to send the air faster.",
            "Aim for a higher Pa while keeping all six holes closed. This is overblowing. You need a focused stream, not a large blast. Make small changes and relax between tries. Overblown notes can take many practice days to settle.", GuidePicture.Breath),
        Guide("Compare the two Pas", "Play low Pa, Pa, low Pa, Pa with a fresh breath for each.",
            "The fingers stay still while the air changes. All-closed is the basic fingering used here. Some teachers use a vented first hole for Pa, but alternate fingerings are outside this course. The microphone checks the target octave as well as pitch.", GuidePicture.Phrase(listOf(Swar.MandraPa, Swar.Pa, Swar.MandraPa, Swar.Pa), "6 closed throughout; change the air speed"))
    ), listOf(Swar.MandraPa, Swar.Pa, Swar.MandraPa, Swar.Pa), "Faster, focused air gives Pa above Sa. If it feels forced, rest and return to low Pa before trying again.", unit = 4),
    Lesson("dha", "Lift into Dha", "Open hole 6 with focused air", 4, listOf(
        Guide("Set five holes", "Close holes 1–5. Leave hole 6 open.",
            "This is the same fingering as low Dha. A faster, focused stream gives shuddh Dha above your home Sa. Keep the fingers on holes 1–5 settled as you lift the last finger.", GuidePicture.Fingers(Swar.Dha)),
        Guide("Keep the higher sound", "Play Dha with the focused air you used for Pa.",
            "Start with the reference, then try a short note. Small adjustments of air speed and angle help the pitch settle. If you hear low Dha, pause and refocus the air rather than squeezing your hands.", GuidePicture.Breath),
        Guide("Move between two notes", "Play Pa, Dha, Pa, Dha. Move only the finger on hole 6.",
            "Keep the overblown sound for both notes. Give each pitch a separate breath if needed. The meter cannot tell whether a change is smooth, so listen for interruptions yourself after the pitch steps.", GuidePicture.Phrase(listOf(Swar.Pa, Swar.Dha, Swar.Pa, Swar.Dha), "6 closed   5   6   5 closed; overblow"))
    ), listOf(Swar.Pa, Swar.Dha, Swar.Pa, Swar.Dha), "Keep the first five holes sealed. Make a small air adjustment if Dha drifts sharp or flat.", unit = 4),
    Lesson("ni", "One step from the top", "Four holes for the higher Ni", 4, listOf(
        Guide("Set four holes", "Close holes 1–4. Leave holes 5 and 6 open.",
            "This repeats the low Ni fingering with overblowing. Shuddh Ni in this register sits just below high Sa. Keep your farther hand relaxed as two of its fingers lift.", GuidePicture.Fingers(Swar.Ni)),
        Guide("Let Ni settle", "Play Ni with a narrow, focused stream of air.",
            "Listen to the reference before each attempt. The pitch can rise if you force the air. Use small changes and keep your jaw comfortable. A teacher can help with tuning and tone if the note remains difficult.", GuidePicture.Breath),
        Guide("Come back through Dha", "Play Dha, Ni, Dha, Pa with focused air.",
            "Move between five and four closed holes, then cover all six for Pa. These are all overblown notes. Stop for a breath whenever needed rather than rushing the return.", GuidePicture.Phrase(listOf(Swar.Dha, Swar.Ni, Swar.Dha, Swar.Pa), "5 closed   4   5   6 closed; overblow"))
    ), listOf(Swar.Dha, Swar.Ni, Swar.Dha, Swar.Pa), "Ni closes four holes, just like low Ni. The focused air selects the higher sound.", unit = 4),
    Lesson("highsa", "Reach high Sa", "Three holes, one octave higher", 5, listOf(
        Guide("Return to three holes", "Close holes 1, 2, and 3. Leave holes 4–6 open.",
            "This is your familiar Sa fingering. With overblowing it gives taar Sa, called high Sa in Basuri. A dot above a note in sargam notation marks this upper register.", GuidePicture.Fingers(Swar.TaarSa)),
        Guide("Keep the air focused", "Play high Sa with a faster, narrower stream than your home Sa.",
            "Make small changes until the higher sound appears. Keep your shoulders and throat relaxed. High Sa needs practice. If you cannot reach it comfortably today, repeat the earlier overblowing lessons on another day.", GuidePicture.Breath),
        Guide("Finish the short climb", "Play Ni, high Sa, Ni, high Sa.",
            "Lift the finger on hole 4 for high Sa. Cover it again for Ni. Keep the overblown sound for both notes. The pitch steps allow a new breath each time.", GuidePicture.Phrase(listOf(Swar.Ni, Swar.TaarSa, Swar.Ni, Swar.TaarSa), "4 closed   3   4   3 closed; overblow"))
    ), listOf(Swar.Ni, Swar.TaarSa, Swar.Ni, Swar.TaarSa), "High Sa uses three closed holes and focused air. Keep your saved Sa setting at your original home Sa.", unit = 4),
    Lesson("octave", "Two kinds of Sa", "Change the air, keep the fingering", 5, listOf(
        Guide("Find home again", "Play your home Sa with gentle air and three holes closed.",
            "After the higher notes, return to the comfortable Sa you learned first. Keep the same flute setting. High Sa is exactly one octave above this reference in the app.", GuidePicture.Fingers(Swar.Sa)),
        Guide("Make the octave change", "Keep your fingers still and focus the air to reach high Sa.",
            "Begin with a new breath. Once high Sa speaks, stop and relax before returning to home Sa. You do not have to connect the two sounds in one breath yet. Listen to each reference if the register is uncertain.", GuidePicture.Breath),
        Guide("Alternate the two", "Play Sa, high Sa, Sa, high Sa, Sa with a rest between notes.",
            "Only your air changes. A pitch match does not grade the attack, the tone, or the octave transition. After the steps, listen to one gentle return to Sa without watching the meter.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.TaarSa, Swar.Sa, Swar.TaarSa, Swar.Sa), "3 closed throughout; gentle air, then focused air"))
    ), listOf(Swar.Sa, Swar.TaarSa, Swar.Sa, Swar.TaarSa, Swar.Sa), "Keep all three fingers still. If high Sa is tiring, finish with a comfortable home Sa and try again another day.", unit = 4),

    Lesson("rise", "A longer climb", "Sa to high Sa, with Ma left out", 5, listOf(
        Guide("Begin the climb", "Play Sa, Re, Ga slowly with gentle air.",
            "Aroh means an ascending sequence. This exercise leaves out Ma. All holes open gives tivra Ma on a standard bansuri. Shuddh Ma needs partial coverage of hole 1, which this course does not teach. This is not a complete scale.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.Re, Swar.Ga), "3 closed   2   1 closed")),
        Guide("Cross from Ga to Pa", "From Ga, cover all six holes and use focused air for Pa.",
            "This move changes several fingers and the air together. Try it silently first if needed, then take a fresh breath for Pa. There is a larger pitch gap here because Ma is omitted. Avoid rushing across it.", GuidePicture.Fingers(Swar.Pa)),
        Guide("Continue to high Sa", "Play Pa, Dha, Ni, high Sa with focused air.",
            "Lift one finger at a time, starting at hole 6. The practice steps join these notes to Sa, Re, and Ga for a seven-note climb that skips Ma. Let each note settle before moving on.", GuidePicture.Phrase(listOf(Swar.Pa, Swar.Dha, Swar.Ni, Swar.TaarSa), "6 closed   5   4   3 closed; overblow"))
    ), listOf(Swar.Sa, Swar.Re, Swar.Ga, Swar.Pa, Swar.Dha, Swar.Ni, Swar.TaarSa), "This climb skips Ma. Breathe before Pa so you can reset the fingers and focus the air.", unit = 5),
    Lesson("fall", "Come down gently", "High Sa to Sa, with Ma left out", 5, listOf(
        Guide("Descend through Pa", "Play high Sa, Ni, Dha, Pa with focused air.",
            "Avroh means a descending sequence. Cover holes 4, 5, and 6 in order. These four notes all use the overblown sound, even as their pitches descend.", GuidePicture.Phrase(listOf(Swar.TaarSa, Swar.Ni, Swar.Dha, Swar.Pa), "3 closed   4   5   6 closed; overblow")),
        Guide("Soften into Ga", "From Pa, leave only hole 1 closed and return to gentle air for Ga.",
            "Take a fresh breath before Ga if that helps. This change crosses the register break in reverse. The exercise skips Ma, so do not treat the jump from Pa to Ga as an adjacent scale step.", GuidePicture.Fingers(Swar.Ga)),
        Guide("Finish at home", "Play Ga, Re, Sa with gentle air.",
            "Cover hole 2, then hole 3. Finish without trying to empty your lungs. Together the practice steps make a descending exercise with Ma omitted, not a full avroh of the shuddh scale.", GuidePicture.Phrase(listOf(Swar.Ga, Swar.Re, Swar.Sa), "1 closed   2   3 closed"))
    ), listOf(Swar.TaarSa, Swar.Ni, Swar.Dha, Swar.Pa, Swar.Ga, Swar.Re, Swar.Sa), "Use focused air down to Pa, then gentler air for Ga, Re, Sa. Rest before repeating the whole descent.", unit = 5),
    Lesson("pairs", "Two of each", "A first repeating-note alankar", 5, listOf(
        Guide("Separate the repeats", "Play Sa twice, with a short breath between the two notes.",
            "Alankars are practice patterns. This one repeats each note before moving on. Keep your fingers still during each pair. A fresh breath separates the notes without introducing tonguing technique.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.Sa), "3 closed for both notes")),
        Guide("Give the notes a pulse", "Play Re twice, counting four slow, even counts for each note.",
            "Count at a comfortable pace, about one count each second. Breathe between notes. The app checks each pitch separately. It does not score the counts or the rhythm. Practice the pulse by listening to yourself.", GuidePicture.Phrase(listOf(Swar.Re, Swar.Re), "2 closed; four slow counts per note")),
        Guide("Repeat Ga, then Pa", "Play Ga, Ga, Pa, Pa with a new breath for each note.",
            "Ga uses one closed hole and gentle air. Pa uses all six closed with focused air. The full practice pattern is Sa Sa, Re Re, Ga Ga, Pa Pa. It skips Ma. Rest before each new pair if you need to.", GuidePicture.Phrase(listOf(Swar.Ga, Swar.Ga, Swar.Pa, Swar.Pa), "1 closed   1   6   6 closed; overblow Pa"))
    ), listOf(Swar.Sa, Swar.Sa, Swar.Re, Swar.Re, Swar.Ga, Swar.Ga, Swar.Pa, Swar.Pa), "Separate repeated notes with a breath. Keep the same comfortable length for each note when you repeat the pattern yourself.", unit = 5),
    Lesson("threes", "Three-note steps", "Sa Re Ga, Re Ga Pa, Ga Pa Dha", 6, listOf(
        Guide("Play the first group", "Play Sa, Re, Ga as one slow three-note group.",
            "A palta repeats a pattern from a different starting note. Begin this one with three notes you know well. Each pitch step can use its own breath. Later, try a group in one breath only if that feels comfortable.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.Re, Swar.Ga), "3 closed   2   1 closed")),
        Guide("Start one note later", "Play Re, Ga, Pa with a pause before Pa if needed.",
            "The pattern moves through the chosen notes and skips Ma. Prepare all six fingers before playing Pa with focused air. This is an exercise in remembering a pattern, not a claim about a particular raga.", GuidePicture.Phrase(listOf(Swar.Re, Swar.Ga, Swar.Pa), "2 closed   1   6 closed; overblow Pa")),
        Guide("Move the group again", "Play Ga, Pa, Dha at the same unhurried pace.",
            "The practice steps contain all three groups in order. After completing them, repeat the groups yourself with a rest between groups. The microphone does not check the grouping or the timing.", GuidePicture.Phrase(listOf(Swar.Ga, Swar.Pa, Swar.Dha), "1 closed   6   5 closed; overblow Pa and Dha"))
    ), listOf(Swar.Sa, Swar.Re, Swar.Ga, Swar.Re, Swar.Ga, Swar.Pa, Swar.Ga, Swar.Pa, Swar.Dha), "Think in three-note groups. Repeat a difficult group on another day before trying to connect all nine notes.", unit = 5),

    Lesson("call", "A little call", "The opening of your first tune", 5, listOf(
        Guide("Begin the call", "Play Sa, Re, Ga, Ga with a breath between the two Gas.",
            "This is the first half of a short folk-style tune written for Basuri. It is an original practice melody, with no raga label. Use gentle air for these opening notes.", GuidePicture.Phrase(listOf(Swar.Sa, Swar.Re, Swar.Ga, Swar.Ga), "3 closed   2   1   1 closed")),
        Guide("Answer the rise", "Play Pa, Ga, Re, Sa, taking a new breath before Ga.",
            "Prepare the focused air for Pa, then return to gentle air for Ga. Keep the phrase comfortable. You can breathe after any note while you learn it.", GuidePicture.Phrase(listOf(Swar.Pa, Swar.Ga, Swar.Re, Swar.Sa), "6 closed   1   2   3 closed; overblow Pa")),
        Guide("Give it a shape", "Repeat the last Sa for four slow counts, then let the sound end gently.",
            "For your own repeat after the pitch steps, give the earlier notes two counts each and the final Sa four. Pause for a breath after the two Gas. These lengths are a musical suggestion. The app checks pitch, not phrasing or rhythm.", GuidePicture.Fingers(Swar.Sa))
    ), listOf(Swar.Sa, Swar.Re, Swar.Ga, Swar.Ga, Swar.Pa, Swar.Ga, Swar.Re, Swar.Sa), "Repeat the call across several days. Use two slow counts per note and four on the final Sa when practicing on your own.", unit = 6),
    Lesson("answer", "Bring the tune home", "A second phrase to follow the call", 6, listOf(
        Guide("Begin the answer", "Play Ga, Pa, Dha, Pa at a comfortable pace.",
            "This is the second half of Basuri's original tune. Start with gentle air on Ga, then focused air on Pa and Dha. Take a breath before the first Pa whenever you need to reset.", GuidePicture.Phrase(listOf(Swar.Ga, Swar.Pa, Swar.Dha, Swar.Pa), "1 closed   6   5   6 closed; overblow Pa and Dha")),
        Guide("Come home", "Play Ga, Re, Sa, Sa with gentle air.",
            "Take a new breath before this half. Close one more hole for Re, then one more for Sa. Separate the two Sas with a comfortable breath. Let the final Sa last a little longer during your own repeat.", GuidePicture.Phrase(listOf(Swar.Ga, Swar.Re, Swar.Sa, Swar.Sa), "1 closed   2   3   3 closed")),
        Guide("Join the two phrases", "After the pitch steps, play the call, rest for a breath, then play the answer.",
            "The call is Sa Re Ga Ga, Pa Ga Re Sa. The answer is Ga Pa Dha Pa, Ga Re Sa Sa. Try two slow counts for each note and four for each phrase's last Sa. Repeat an earlier lesson on days when a change feels difficult. Completing the path records practice, not mastery.", GuidePicture.Flute)
    ), listOf(Swar.Ga, Swar.Pa, Swar.Dha, Swar.Pa, Swar.Ga, Swar.Re, Swar.Sa, Swar.Sa), "Keep revisiting the tune and earlier exercises over the coming weeks. A comfortable phrase is a useful daily goal.", unit = 6),
)
