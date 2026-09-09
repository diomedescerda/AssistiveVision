# LiteRT (TensorFlow Lite) — the interpreter loads models and dispatches
# through JNI; keep the public API and native entry points intact.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep model metadata annotation types if present
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.support.**

# Sherpa-ONNX offline TTS — native code resolves Kotlin classes and native
# methods by name through JNI.
-keep class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn com.k2fsa.sherpa.onnx.**

# Streaming speech callback: sherpa's JNI looks up invoke([F)Ljava/lang/Integer;
# on the callback object; keep the adapter and its exact method signature.
-keep class com.unimagdalena.assistivevision.modules.TtsSampleCallback { *; }
