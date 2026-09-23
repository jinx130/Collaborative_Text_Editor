package com.yourname.editor.ot;

import java.util.Objects;

/**
 * Single retain/insert/delete component (Wave-style operation model).
 * Mirrors the blog's operation types: insert/delete/retain, but as
 * components of a composite {@link Operation} instead of single
 * position-based ops.
 */
public class OperationComponent {

    public enum Type {
        RETAIN,
        INSERT,
        DELETE
    }

    private Type type;
    // Integer (not int): Jackson 3 fails on missing JSON properties for
    // primitives, and browser INSERT components legitimately omit "count".
    private Integer count;
    private String text;

    public OperationComponent() {
    }

    public OperationComponent(Type type, Integer count, String text) {
        this.type = type;
        if (type == Type.INSERT) {
            this.text = text == null ? "" : text;
            this.count = this.text.length();
        } else {
            this.text = null;
            this.count = count == null ? 0 : count;
        }
    }

    public static OperationComponent retain(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("retain count must be > 0");
        }
        return new OperationComponent(Type.RETAIN, count, null);
    }

    public static OperationComponent insert(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("insert text must be non-empty");
        }
        return new OperationComponent(Type.INSERT, text.length(), text);
    }

    public static OperationComponent delete(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("delete count must be > 0");
        }
        return new OperationComponent(Type.DELETE, count, null);
    }

    public boolean isRetain() {
        return type == Type.RETAIN;
    }

    public boolean isInsert() {
        return type == Type.INSERT;
    }

    public boolean isDelete() {
        return type == Type.DELETE;
    }

    /** Chars consumed from base document (retain + delete). */
    public int baseLength() {
        if (type == null) {
            return 0;
        }
        switch (type) {
            case RETAIN:
            case DELETE:
                return getCount();
            case INSERT:
            default:
                return 0;
        }
    }

    /** Chars produced in target document (retain + insert). */
    public int targetLength() {
        if (type == null) {
            return 0;
        }
        switch (type) {
            case RETAIN:
                return getCount();
            case INSERT:
                return text == null ? 0 : text.length();
            case DELETE:
            default:
                return 0;
        }
    }

    public int insertLength() {
        if (!isInsert() || text == null) {
            return 0;
        }
        return text.length();
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getCount() {
        if (isInsert()) {
            return text == null ? 0 : text.length();
        }
        return count == null ? 0 : count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperationComponent)) {
            return false;
        }
        OperationComponent that = (OperationComponent) o;
        return getCount() == that.getCount()
                && type == that.type
                && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, getCount(), text);
    }

    @Override
    public String toString() {
        if (type == null) {
            return "unknown";
        }
        switch (type) {
            case RETAIN:
                return "retain(" + count + ")";
            case INSERT:
                return "insert(\"" + text + "\")";
            case DELETE:
                return "delete(" + count + ")";
            default:
                return "unknown";
        }
    }
}
