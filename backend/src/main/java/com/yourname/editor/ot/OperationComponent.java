package com.yourname.editor.ot;

/**
 * Single retain/insert/delete component (§4).
 * TODO: implement on your own.
 */
public class OperationComponent {

    public enum Type {
        RETAIN,
        INSERT,
        DELETE
    }

    private Type type;
    private int count;
    private String text;

    public OperationComponent() {
    }

    public OperationComponent(Type type, int count, String text) {
        // TODO: implement
        this.type = type;
        this.count = count;
        this.text = text;
    }

    public static OperationComponent retain(int count) {
        // TODO: implement
        return new OperationComponent(Type.RETAIN, count, null);
    }

    public static OperationComponent insert(String text) {
        // TODO: implement
        return new OperationComponent(Type.INSERT, text == null ? 0 : text.length(), text);
    }

    public static OperationComponent delete(int count) {
        // TODO: implement
        return new OperationComponent(Type.DELETE, count, null);
    }

    public boolean isRetain() {
        // TODO: implement
        return type == Type.RETAIN;
    }

    public boolean isInsert() {
        // TODO: implement
        return type == Type.INSERT;
    }

    public boolean isDelete() {
        // TODO: implement
        return type == Type.DELETE;
    }

    public int baseLength() {
        // TODO: implement
        return 0;
    }

    public int targetLength() {
        // TODO: implement
        return 0;
    }

    public int insertLength() {
        // TODO: implement
        return 0;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
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
        // TODO: implement
        return super.toString();
    }
}
