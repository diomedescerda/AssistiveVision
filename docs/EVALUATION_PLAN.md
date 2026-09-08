# AssistiveVision — Evaluation Plan (Proposal Phase 3)

This document turns the proposal's Phase 3 activities (device-positioning
evaluation, scenario selection, controlled experiments on occlusion/illumination/
response time, final report) into a **reproducible protocol** with defined metrics,
sample sizes, acceptance thresholds, and reporting formats.

Everything marked **[user]** requires physical devices, participants, or
institutional steps that cannot be automated in this repository. Everything else
(scripts, schemas, logging hooks) is implementable here or already implemented.

---

## 1. Objectives and hypotheses

- **H1 (detection quality):** The 10-class on-device model announces the correct
  object with spatial zone in ≥ 80% of controlled indoor captures (macro-F1 ≥ 0.75
  at IoU ≥ 0.5, default threshold 0.30).
- **H2 (response time):** End-to-end capture→speech completes in ≤ 2.5 s at p95 on
  the reference device.
- **H3 (positioning):** The lowest-focal-length (0.5× ultra-wide) binding yields
  higher detection recall at 0.5–1.5 m than the 1× main lens.
- **H4 (illumination/occlusion):** Detection F1 degrades monotonically with reduced
  illumination and increasing occlusion; thresholds quantify the degradation.
- **H5 (TTS clarity):** Bundled-voice announcements are transcribed correctly
  (word error rate ≤ 10%) and rated ≥ 4/5 comprehension by participants.

## 2. Test environment

- **Reference device [user]:** the Snapdragon 685 phone used for earlier
  measurements, plus ≥ 1 additional device (different OEM/Android version) for
  compatibility. Record: model, Android version, build, battery state, thermal state.
- **App build:** release variant (`assembleRelease`) signed, installed via `adb`.
- **Environment [user]:** one indoor room per scenario set; measure illumination
  with a lux-meter app (record lux per session); fixed camera distance marks
  (0.5 / 1 / 2 / 3 m) with floor tape.

## 3. Scenario matrix [user]

| Axis | Levels |
|------|--------|
| Positioning | handheld chest-height, handheld waist-height, tripod-fixed |
| Distance | 0.5 m, 1 m, 2 m, 3 m |
| Object classes | all 10 classes (at least 3 instances each where available) |
| Illumination | bright (≥ 500 lux), indoor (~200 lux), dim (~50 lux) |
| Occlusion | none, ~25%, ~50% (manual occluders or natural clutter) |
| Negative scenes | empty room, scenes with **non-target** COCO objects only (false-positive check) |
| Lens | 0.5× ultra-wide vs 1× main (H3; force via a debug build flag or by testing a device without ultra-wide) |

Each cell: ≥ 5 captures → recommended **≥ 240 captures** per device for the full
matrix (10 classes × 4 distances × 3 illumination × 3 occlusion × ~0.7 weighting
plus negatives). If time-boxed, prioritize: classes × distances at indoor/none, then
illumination sweep on {chair, person, table}, then occlusion sweep, then negatives.

## 4. Ground truth and annotation [user]

- For every capture, save the **frozen frame** (`adb exec-out screencap` or the
  in-app frozen view screenshot) and annotate **all** target objects with bounding
  boxes and class labels in a CSV/`pascal_voc`-style file.
- **Matching rule:** a detection matches ground truth iff same class and
  **IoU ≥ 0.5**. Each GT box matches at most one detection (highest score wins).
- Store per-capture: `image_id, scenario (lens/distance/lux/occlusion), gt_class,
  gt_bbox, pred_class, pred_bbox, pred_score, pred_zone, gt_zone, capture_ts,
  preprocess_ms, inference_ms, localize_ms, tts_synth_ms, end_to_end_ms`.

## 5. Offline / no-network verification [user + automated]

1. Enable airplane mode; verify capture + announcement works (this is the H1 run).
2. `adb shell dumpsys netpolicy | grep -i <pkg>` and packet capture (optional) to
   confirm zero traffic — structurally guaranteed by the missing INTERNET permission.
3. Automated: `TtsOfflineInstrumentedTest` (assets present; engine init + speak
   completes; espeak copy on disk) runs green on device/emulator.

## 6. Metrics (definitions)

- **Precision_c = TP_c / (TP_c + FP_c)**, **Recall_c = TP_c / (TP_c + FN_c)**,
  **F1_c = 2PR/(P+R)** per class; **macro-F1** = mean over the 10 classes.
- **Zone accuracy** = share of matched detections whose announced zone equals the GT
  zone (GT zone from GT box center, same thresholds).
- **mAP@0.5** (optional, secondary): COCO-style AP over score thresholds.
- **Latency percentiles:** report **p50/p95** (never only the mean) for
  `preprocess_ms`, `inference_ms`, `end_to_end_ms` (capture tap → speech done).
- **TTS real-time factor (RTF)** = synthesis wall time / synthesized audio duration;
  target ≤ 1.0 on reference hardware.
- **Threshold sweep:** confidence 0.10→0.90 step 0.05 → P/R curves to justify the
  0.30 default.

## 7. Battery, thermal, memory [user]

Scripted session: 10 min idle preview + 60 captures at 1 capture / 5 s.
Record before/after: battery %, `dumpsys batterystats` delta for the package,
`dumpsys thermalservice` temperature, peak RSS (`dumpsys meminfo`). Acceptance:
≤ 10% battery per session; no thermal throttling warning during the session.

## 8. TTS clarity, comprehension, and user testing [user]

- **Transcription test:** play ≥ 40 announcements (20 per language) to 5+ listeners;
  score WER from typed transcriptions (H5). Include both voices at rate 1.0 and 1.5.
- **Subjective rating:** 5-point comprehension/naturalness rating per voice.
- **Accessibility walkthrough:** full task list with TalkBack enabled
  (grant permission → capture → change language → adjust rate → verify live-region
  status and button labels). Record task success/failures.
- **Participants [user]:** ideally include visually impaired participants;
  **ethics/consent:** obtain institutional approval and signed informed consent
  before any human data collection; anonymize all records; no data in this repo.

## 9. Baselines / comparison [user]

- Baseline A: platform TTS reading the same sentences (documents why bundled voices
  were required: availability/offline variance).
- Baseline B: no-zone announcement vs zone announcement (zone-accuracy usefulness).
  Both feed the final report's discussion.

## 10. Acceptance thresholds

| Metric | Threshold |
|--------|-----------|
| macro-F1 @ IoU 0.5, thr 0.30 | ≥ 0.75 |
| Recall for `person`, `chair`, `table` | ≥ 0.80 each |
| Zone accuracy | ≥ 0.85 |
| end-to-end p95 | ≤ 2.5 s |
| TTS RTF | ≤ 1.0 |
| WER (transcription test) | ≤ 10% |
| Battery per scripted session | ≤ 10% |
| Negative-scene false announcements | ≤ 1 per 20 negative captures |

## 11. Statistical reporting

Per metric: n, mean, standard deviation, p50/p95 (latency), 95% CI (bootstrap or
t-based), and per-scenario breakdowns. Compare lens conditions (H3) with a paired
test (Wilcoxon signed-rank on per-scenario F1) and report effect size. Report all
failures/exceptions with counts. Raw per-capture CSVs + this plan + analysis script
constitute the reproducible artifact set.

## 12. Limitations and threats to validity (for the final report)

- Single reference device family for latency; cross-device variance expected.
- Ground truth by manual annotation → annotator error; mitigate with a second pass.
- Controlled indoor scenes ≠ real-world homes; occlusion levels are approximations.
- English voice is `low` quality tier (16 kHz) vs Spanish `high` (22 kHz) — expected
  to dominate H5 differences between languages; report per-voice.
- Doors/desks are not detectable classes (see Requirements §4) — a documented scope
  limitation, not a measurement failure.

## 13. Artifacts and ownership

| Artifact | Owner | Location |
|----------|-------|----------|
| Capture/annotation CSV schema | repo | this §4 |
| Latency logging hooks | repo (implemented) | `MainViewModel` / `AudioModule` stage timings |
| Offline instrumented test | repo | `TtsOfflineInstrumentedTest` |
| Raw captures, annotations, analysis notebook, final report, ethics docs | **user** | keep outside repo (privacy) |
