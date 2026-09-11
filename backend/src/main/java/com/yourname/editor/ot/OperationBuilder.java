package com.yourname.editor.ot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for {@link Operation}.
 * TODO: implement on your own.
 */
public class OperationBuilder {

    private final List<OperationComponent> components = new ArrayList<>();

    public OperationBuilder retain(int count) {
        // TODO: implement
        return this;
    }

    public OperationBuilder insert(String text) {
        // TODO: implement
        return this;
    }

    public OperationBuilder delete(int count) {
        // TODO: implement
        return this;
    }

    public OperationBuilder append(OperationComponent component) {
        // TODO: implement
        return this;
    }

    public OperationBuilder append(Operation operation) {
        // TODO: implement
        return this;
    }

    public Operation build() {
        // TODO: implement
        return new Operation(components);
    }
}
