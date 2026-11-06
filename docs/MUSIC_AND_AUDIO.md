# Music and audio notes

## Fingering and relative Sa

Basuri teaches the six-hole Nepali basuri (बाँसुरी), which uses the same fingering as the six-hole Hindustani bansuri. Holes are counted from the blowing hole. Sa closes holes 1–3, shuddh Re closes 1–2, and shuddh Ga closes only hole 1. The other finger holes stay open. The blowing hole is separate from the six finger holes.

The fingering was checked against the teacher-authored [Bansuri Academy swaras chart](https://bansuri.com/learn-bansuri/swaras-symbols/) and the maker's [Bansuri Australia beginner guidance](https://bansuriaustralia.com.au/beginners-tips/). [Dr. Kerry Kriger's Bhoopali lesson](https://bansuribliss.com/bhoopali/) also identifies the three-hole Sa and gives examples with different tonic pitches. Lesson copy is newly authored for this app.

Sa is relative to the flute and chosen tonic. Printed flute labels can refer to different conventions, including an all-closed pipe note. Basuri therefore asks for the three-hole Sa's sounding note and octave explicitly. It does not infer them from a product label. The introductory content does not cover Carnatic venu fingerings, seven-hole variants, half-holing, or advanced ragas. A teacher can help with hold, breath, tone quality, and instrument-specific tuning.

References use A4 = 440 Hz with Re two semitones and Ga four semitones above Sa, shifted by an optional fine tuning of up to ±50 cents set in My flute. The tuning applies to reference tones, pitch matching, the tanpura, and notation playback; pitch names stay those of the nearest equal-tempered note. This equal-tempered guide is an approximation for beginner pitch matching, not a claim that Hindustani intonation is fixed to piano temperament. Sa can be set from C3 through B5. The audio detector covers roughly 100–1600 Hz, including the authored Sa, Re, and Ga targets in that range.

## Audio processing

`AudioEngine.kt` generates a short harmonic reference tone with an attack and release envelope. This is a synthesized pitch guide, not a recording of a bansuri. It uses `AudioTrack` for playback and `AudioRecord` with a 24 kHz mono PCM input for listening. One coroutine owns each audio resource. Switching modes cancels and joins the previous audio job before opening the next resource. Capture waits an additional 350 ms for speaker decay.

Pitch detection uses the YIN cumulative mean normalized difference function, a fixed comparison window, and parabolic lag refinement. Silence below an RMS threshold, clipping, and frames without sufficient periodicity produce no pitch measurement. A match requires approximately 1.5 consecutive seconds within ±35 cents of the exact target octave. Up to about 200 ms of readings outside the band (breath noise, an unclear frame, or an octave misread) pauses the hold instead of resetting it; anything longer resets it. Long tones allow 400 ms. A completed match remains earned until the exercise changes. In 0.2, earning a match also stops microphone capture. Starting a new free-practice listening attempt clears the previous free-practice match.

A cents offset is `1200 * log2(measuredHz / targetHz)`. Basuri displays the measured frequency and offset directly. It never substitutes a target for missing microphone data. A microphone cannot prove that a sound came from a flute. Other pitched sources can trigger it. Background audio, hardware signal processing, instrument harmonics, or microphone privacy controls may prevent a reading or cause octave errors. The app offers guidance and self-guided continuation rather than claiming to diagnose embouchure or finger placement.

## Sa detection

Setup and My flute can listen for the three-hole Sa. Detection waits for 16 consecutive frames (about 1.4 seconds) whose pitches stay within 40 cents of their running median, allowing up to two unclear frames, and gives up after 15 seconds. The median is rounded to the nearest equal-tempered note for the Sa setting, and the remainder becomes the fine tuning. Results outside C3–B5 are rejected rather than clamped. Nothing is saved until the learner confirms.

## Resource handling

Audio resources are released in `finally` blocks on completion, cancellation, and errors. The view model cancels audio when screens change, the note changes, or the app stops. Audio focus loss stops the current session. Permission denial and capture failures appear as actionable messages. Progress survives process death, and in-progress lesson navigation uses `SavedStateHandle`. Audio never resumes automatically after backgrounding or process recreation.
