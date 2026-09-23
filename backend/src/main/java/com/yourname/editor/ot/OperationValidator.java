package com.yourname.editor.ot;

/**
 * Validates an operation against a base document length
 * (blog's validateOperation()).
 */
public final class OperationValidator {

    private OperationValidator() {
    }

    public static void validate(Operation operation, int baseLength) {
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        if (baseLength < 0) {
            throw new IllegalArgumentException("baseLength must be >= 0");
        }
        if (operation.baseLength() != baseLength) {
            throw new IllegalArgumentException(
                    "operation baseLength " + operation.baseLength()
                            + " does not match expected " + baseLength);
        }
        for (OperationComponent c : operation.getComponents()) {
            if (c.getType() == null) {
                throw new IllegalArgumentException("component type must not be null");
            }
            switch (c.getType()) {
                case RETAIN:
                case DELETE:
                    if (c.getCount() <= 0) {
                        throw new IllegalArgumentException(c.getType() + " count must be > 0");
                    }
                    break;
                case INSERT:
                    if (c.getText() == null || c.getText().isEmpty()) {
                        throw new IllegalArgumentException("insert text must be non-empty");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown component type");
            }
        }
    }
}
