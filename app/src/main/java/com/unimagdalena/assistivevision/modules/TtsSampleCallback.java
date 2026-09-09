package com.unimagdalena.assistivevision.modules;

import kotlin.jvm.functions.Function1;

/**
 * Named adapter handed to sherpa-onnx's JNI layer for streaming speech.
 *
 * The native generateWithCallbackImpl resolves the callback reflectively by
 * looking up {@code invoke([F)Ljava/lang/Integer;} on the callback object's
 * class. D8 lambda desugaring drops that specialized signature from Kotlin
 * lambdas, which aborts the process with NoSuchMethodError the first time
 * speech is generated. A named class compiled by javac keeps the exact
 * signature the native code expects.
 */
public final class TtsSampleCallback implements Function1<float[], Integer> {

    private final Function1<float[], Integer> delegate;

    public TtsSampleCallback(Function1<float[], Integer> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Integer invoke(float[] samples) {
        return delegate.invoke(samples);
    }
}
