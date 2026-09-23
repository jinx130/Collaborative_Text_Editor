package com.yourname.editor.ot;

/**
 * Applies an operation to a document string (blog's apply()).
 */
public final class OperationApplier {

    private OperationApplier() {
    }

    public static String apply(Operation operation, String document) {
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        String doc = document == null ? "" : document;
        if (operation.baseLength() != doc.length()) {
            throw new IllegalArgumentException(
                    "operation baseLength " + operation.baseLength()
                            + " does not match document length " + doc.length());
        }
        StringBuilder out = new StringBuilder(operation.targetLength());
        int index = 0;
        for (OperationComponent c : operation.getComponents()) {
            if (c.isRetain()) {
                int count = c.getCount();
                if (index + count > doc.length()) {
                    throw new IllegalArgumentException("retain overruns document");
                }
                out.append(doc, index, index + count);
                index += count;
            } else if (c.isInsert()) {
                out.append(c.getText() == null ? "" : c.getText());
            } else if (c.isDelete()) {
                int count = c.getCount();
                if (index + count > doc.length()) {
                    throw new IllegalArgumentException("delete overruns document");
                }
                index += count;
            }
        }
        if (index != doc.length()) {
            throw new IllegalArgumentException("operation did not consume entire document");
        }
        return out.toString();
    }
}
