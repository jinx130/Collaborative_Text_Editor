package com.yourname.editor.ot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class OperationApplierTest {

    @Test
    void appliesInsertAtStart() {
        Operation op = new OperationBuilder().insert("Hello").retain(6).build();
        assertEquals("Hello World", OperationApplier.apply(op, " World"));
    }

    @Test
    void appliesDelete() {
        Operation op = new OperationBuilder().delete(5).retain(6).build();
        assertEquals(" World", OperationApplier.apply(op, "Hello World"));
    }

    @Test
    void appliesReplace() {
        // Replace "Hello" with "Hi" in "Hello World"
        Operation op = new OperationBuilder().delete(5).insert("Hi").retain(6).build();
        assertEquals("Hi World", OperationApplier.apply(op, "Hello World"));
    }

    @Test
    void rejectsBaseLengthMismatch() {
        Operation op = new OperationBuilder().retain(3).build();
        assertThrows(IllegalArgumentException.class, () -> OperationApplier.apply(op, "Hello"));
    }
}
