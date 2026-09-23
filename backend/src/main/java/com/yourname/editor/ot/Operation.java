package com.yourname.editor.ot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Framework-free OT operation: ordered list of retain/insert/delete.
 * Blog mapping: a single blog-style {@code Insert(pos,text)} becomes
 * {@code [retain(pos), insert(text)]} and {@code Delete(pos,len)} becomes
 * {@code [retain(pos), delete(len)]}.
 */
public class Operation {

    private List<OperationComponent> components;

    public Operation() {
        this.components = new ArrayList<>();
    }

    public Operation(List<OperationComponent> components) {
        this.components = components == null ? new ArrayList<>() : new ArrayList<>(components);
    }

    public List<OperationComponent> getComponents() {
        return components;
    }

    public void setComponents(List<OperationComponent> components) {
        this.components = components == null ? new ArrayList<>() : new ArrayList<>(components);
    }

    /** Chars consumed from base document. */
    public int baseLength() {
        int n = 0;
        for (OperationComponent c : components) {
            n += c.baseLength();
        }
        return n;
    }

    /** Chars produced in target document. */
    public int targetLength() {
        int n = 0;
        for (OperationComponent c : components) {
            n += c.targetLength();
        }
        return n;
    }

    public boolean isNoop() {
        for (OperationComponent c : components) {
            if (c.isInsert()) {
                return false;
            }
            if (c.isDelete()) {
                return false;
            }
        }
        return true;
    }

    /** Compacted copy: merges adjacent same-type components, drops zero-length. */
    public Operation compacted() {
        List<OperationComponent> out = new ArrayList<>();
        for (OperationComponent c : components) {
            if (c == null || c.getType() == null) {
                continue;
            }
            if (c.isInsert()) {
                String text = c.getText() == null ? "" : c.getText();
                if (text.isEmpty()) {
                    continue;
                }
                if (!out.isEmpty() && out.get(out.size() - 1).isInsert()) {
                    OperationComponent last = out.get(out.size() - 1);
                    String merged = (last.getText() == null ? "" : last.getText()) + text;
                    last.setText(merged);
                    last.setCount(merged.length());
                } else {
                    out.add(new OperationComponent(OperationComponent.Type.INSERT, text.length(), text));
                }
            } else {
                int count = c.getCount();
                if (count <= 0) {
                    continue;
                }
                if (!out.isEmpty()) {
                    OperationComponent last = out.get(out.size() - 1);
                    if (last.getType() == c.getType()) {
                        last.setCount(last.getCount() + count);
                        continue;
                    }
                }
                out.add(new OperationComponent(c.getType(), count, null));
            }
        }
        return new Operation(out);
    }

    public List<OperationComponent> unmodifiableComponents() {
        return Collections.unmodifiableList(components);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Operation)) {
            return false;
        }
        Operation that = (Operation) o;
        return Objects.equals(this.compacted().components, that.compacted().components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(compacted().components);
    }

    @Override
    public String toString() {
        return "Operation" + components;
    }
}
