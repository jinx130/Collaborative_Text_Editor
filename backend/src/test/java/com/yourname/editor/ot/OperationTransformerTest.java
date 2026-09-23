package com.yourname.editor.ot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class OperationTransformerTest {

    private static boolean verifyTp1(String state, Operation a, Operation b) {
        OperationTransformer.TransformResult r = OperationTransformer.transform(a, b);
        String path1 = OperationApplier.apply(r.bPrime(), OperationApplier.apply(a, state));
        String path2 = OperationApplier.apply(r.aPrime(), OperationApplier.apply(b, state));
        return path1.equals(path2);
    }

    @Test
    void concurrentInsertsConvergeWithDeterministicOrder() {
        // Alice inserts "!" at 5, Bob inserts " World" at 5 in "Hello"
        Operation alice = new OperationBuilder().retain(5).insert("!").build();
        Operation bob = new OperationBuilder().retain(5).insert(" World").build();
        assertTrue(verifyTp1("Hello", alice, bob));

        OperationTransformer.TransformResult r = OperationTransformer.transform(alice, bob);
        // a wins: applying a then b' yields "Hello! World"
        assertEquals("Hello! World", OperationApplier.apply(r.bPrime(), OperationApplier.apply(alice, "Hello")));
    }

    @Test
    void concurrentDeletesOfSameRegionConverge() {
        Operation a = new OperationBuilder().delete(5).retain(6).build();
        Operation b = new OperationBuilder().delete(5).retain(6).build();
        assertTrue(verifyTp1("Hello World", a, b));
    }

    @Test
    void insertVsDeleteConverges() {
        Operation ins = new OperationBuilder().retain(5).insert("!").retain(6).build();
        Operation del = new OperationBuilder().retain(6).delete(5).build();
        assertTrue(verifyTp1("Hello World", ins, del));
    }

    @Test
    void overlappingDeletesConverge() {
        Operation a = new OperationBuilder().delete(4).retain(7).build();
        Operation b = new OperationBuilder().retain(2).delete(4).retain(5).build();
        assertTrue(verifyTp1("Hello World", a, b));
    }
}
