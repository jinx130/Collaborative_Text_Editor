package com.yourname.editor.ot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Framework-free OT operation: ordered list of retain/insert/delete (§4).
 * TODO: implement on your own.
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
        // TODO: implement
        return 0;
    }

    /** Chars produced in target document. */
    public int targetLength() {
        // TODO: implement
        return 0;
    }

    public boolean isNoop() {
        // TODO: implement
        return false;
    }

    /** Compacted copy: merges adjacent same-type components, drops zero-length. */
    public Operation compacted() {
        // TODO: implement
        return new Operation(components);
    }

    public List<OperationComponent> unmodifiableComponents() {
        return Collections.unmodifiableList(components);
    }

    @Override
    public boolean equals(Object o) {
        // TODO: implement
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        // TODO: implement
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "Operation" + components;
    }
}
