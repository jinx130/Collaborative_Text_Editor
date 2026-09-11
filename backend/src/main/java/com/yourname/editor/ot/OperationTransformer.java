package com.yourname.editor.ot;

/**
 * OT transform for two concurrent operations sharing the same base document.
 * TODO: implement on your own.
 */
public final class OperationTransformer {

    private OperationTransformer() {
    }

    public record TransformResult(Operation aPrime, Operation bPrime) {
    }

    public static TransformResult transform(Operation a, Operation b) {
        // TODO: implement
        throw new UnsupportedOperationException("TODO: implement");
    }
}
