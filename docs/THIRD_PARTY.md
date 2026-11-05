# Bundled components and attribution

This component inventory accompanies the current application on **`main`**.
A distributed APK records its exact source commit in `build-manifest.json`.

The app source uses the [Apache-2.0 license](../LICENSE). Bundled models,
voices and phonemization data retain their own terms. A debug APK is a research
build; the label “debug” does not replace those terms.

| Component | Exact version or identity | Upstream and terms |
|---|---|---|
| EfficientDet-Lite2 | `efficientdet-lite2-detection-metadata.tflite`, SHA-256 `6fd32c84ab1eb0f7e7f3a7a20a20d7df1530daa8378728f7c79571096286bd52` | [TensorFlow Lite object detection models](https://www.tensorflow.org/lite/examples/object_detection/overview), [TensorFlow models source, Apache-2.0](https://github.com/tensorflow/models). The repository preserves the exact evaluated binary; it does not contain a training/export recipe proving how that binary was produced. |
| LiteRT | Maven `com.google.ai.edge.litert:litert:1.4.2` | [LiteRT source and license](https://github.com/google-ai-edge/LiteRT) |
| sherpa-onnx | JitPack `com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.6` | [Source at v1.13.6](https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.6), Apache-2.0; includes ONNX Runtime native components with their upstream notices |
| Spanish Piper voice | `vits-piper-es_MX-claude-high-int8` | [Bundled model card](../app/src/main/assets/tts/vits-piper-es_MX-claude-high-int8/MODEL_CARD), [dataset/model source](https://huggingface.co/spaces/HirCoir/Piper-TTS-Spanish), Apache-2.0 as stated in the card |
| English Piper voice | `vits-piper-en_US-amy-low-int8` | [Bundled model card](../app/src/main/assets/tts/vits-piper-en_US-amy-low-int8/MODEL_CARD), [mimic3-voices](https://github.com/MycroftAI/mimic3-voices). The card delegates license details to upstream; its repository includes CC-BY-SA-4.0 and voice-specific terms. Do not relabel this asset Apache-2.0. |
| espeak-ng data | Exact 355-file bundled `tts/espeak-ng-data/` tree | [espeak-ng source and GPL-3.0 license](https://github.com/espeak-ng/espeak-ng). The original data-generation commit is not recorded; checksums identify the supplied files. |

Voice distributions are available from [sherpa-onnx TTS model releases](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models).
The repository already contains the voice binaries and cards, so no model
download is required to build. [assets.sha256.json](assets.sha256.json) hashes
all 362 bundled files; `python scripts/verify_assets.py` checks them.

The Gradle/Maven dependencies, Android SDK and build tools have their own
licenses, available with those distributions. Preserve this attribution file,
the app license and the bundled model cards when sharing an APK. Public
redistribution also needs the applicable upstream notices and source obligations;
the app's Apache license alone is not a license for every bundled asset.
