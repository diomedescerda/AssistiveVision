# LiteRT (TensorFlow Lite) — the interpreter loads models and dispatches
# through JNI; keep the public API and native entry points intact.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep model metadata annotation types if present
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.support.**
