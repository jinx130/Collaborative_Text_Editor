package com.yourname.editor.ot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for {@link Operation}.
 */
public class OperationBuilder {

    private final List<OperationComponent> components = new ArrayList<>();

    public OperationBuilder retain(int count) {
        if (count <= 0) {
            return this;
        }
        append(new OperationComponent(OperationComponent.Type.RETAIN, count, null));
        return this;
    }

    public OperationBuilder insert(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        append(new OperationComponent(OperationComponent.Type.INSERT, text.length(), text));
        return this;
    }

    public OperationBuilder delete(int count) {
        if (count <= 0) {
            return this;
        }
        append(new OperationComponent(OperationComponent.Type.DELETE, count, null));
        return this;
    }

    public OperationBuilder append(OperationComponent component) {
        if (component == null || component.getType() == null) {
            return this;
        }
        if (component.isInsert()) {
            String text = component.getText() == null ? "" : component.getText();
            if (text.isEmpty()) {
                return this;
            }
            if (!components.isEmpty() && components.get(components.size() - 1).isInsert()) {
                OperationComponent last = components.get(components.size() - 1);
                String merged = (last.getText() == null ? "" : last.getText()) + text;
                last.setText(merged);
                last.setCount(merged.length());
            } else {
                components.add(new OperationComponent(OperationComponent.Type.INSERT, text.length(), text));
            }
            return this;
        }
        int count = component.getCount();
        if (count <= 0) {
            return this;
        }
        if (!components.isEmpty()) {
            OperationComponent last = components.get(components.size() - 1);
            if (last.getType() == component.getType()) {
                last.setCount(last.getCount() + count);
                return this;
            }
        }
        components.add(new OperationComponent(component.getType(), count, null));
        return this;
    }

    public OperationBuilder append(Operation operation) {
        if (operation == null || operation.getComponents() == null) {
            return this;
        }
        for (OperationComponent c : operation.getComponents()) {
            append(c);
        }
        return this;
    }

    public Operation build() {
        return new Operation(new ArrayList<>(components)).compacted();
    }
}
