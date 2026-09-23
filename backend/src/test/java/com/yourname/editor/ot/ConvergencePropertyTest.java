package com.yourname.editor.ot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Proves TP1 (blog's verifyTP1) over a fuzz of inserts/deletes, plus the
 * article's Alice/Bob scenario.
 */
public class ConvergencePropertyTest {

    static String applyPath(String state, Operation first, Operation secondPrime) {
        return OperationApplier.apply(secondPrime, OperationApplier.apply(first, state));
    }

    static boolean verifyTp1(String state, Operation a, Operation b) {
        OperationTransformer.TransformResult r = OperationTransformer.transform(a, b);
        return applyPath(state, a, r.bPrime()).equals(applyPath(state, b, r.aPrime()));
    }

    static Operation insertOp(int docLen, int pos, String text) {
        OperationBuilder b = new OperationBuilder();
        if (pos > 0) {
            b.retain(pos);
        }
        b.insert(text);
        if (pos < docLen) {
            b.retain(docLen - pos);
        }
        return b.build();
    }

    static Operation deleteOp(int docLen, int pos, int len) {
        OperationBuilder b = new OperationBuilder();
        if (pos > 0) {
            b.retain(pos);
        }
        b.delete(len);
        if (pos + len < docLen) {
            b.retain(docLen - pos - len);
        }
        return b.build();
    }

    @Test
    void aliceAndBobScenario() {
        assertTrue(verifyTp1("Hello",
                insertOp(5, 5, "!"),
                insertOp(5, 5, " World")));
    }

    @Test
    void fuzzInsertInsert() {
        List<String> states = List.of("", "a", "ab", "hello world");
        for (String state : states) {
            for (int p1 = 0; p1 <= state.length(); p1++) {
                for (int p2 = 0; p2 <= state.length(); p2++) {
                    assertTrue(verifyTp1(state, insertOp(state.length(), p1, "X"), insertOp(state.length(), p2, "Y")),
                            "insert-insert failed on '" + state + "' " + p1 + "," + p2);
                }
            }
        }
    }

    @Test
    void fuzzDeleteDelete() {
        String state = "hello world";
        List<int[]> ranges = new ArrayList<>();
        ranges.add(new int[] { 0, 5 });
        ranges.add(new int[] { 0, 5 });
        ranges.add(new int[] { 0, 1 });
        ranges.add(new int[] { 6, 5 });
        ranges.add(new int[] { 2, 4 });
        for (int[] r1 : ranges) {
            for (int[] r2 : ranges) {
                assertTrue(
                        verifyTp1(state, deleteOp(state.length(), r1[0], r1[1]), deleteOp(state.length(), r2[0], r2[1])),
                        "delete-delete failed " + r1[0] + "," + r1[1] + " vs " + r2[0] + "," + r2[1]);
            }
        }
    }

    @Test
    void fuzzInsertDelete() {
        String state = "hello world";
        for (int pos = 0; pos <= state.length(); pos++) {
            assertTrue(verifyTp1(state, insertOp(state.length(), pos, "X"), deleteOp(state.length(), 0, 5)));
            assertTrue(verifyTp1(state, deleteOp(state.length(), 0, 5), insertOp(state.length(), pos, "X")));
        }
    }

    @Test
    void serverSerializationIsOrderIndependent() {
        // Two clients at revision 0 submit concurrently; server applies op1 then
        // transformed op2. Result must equal TP1 path1.
        Operation a = insertOp(5, 5, "!");
        Operation b = insertOp(5, 5, " World");
        OperationTransformer.TransformResult r = OperationTransformer.transform(a, b);
        String serverOrder = OperationApplier.apply(r.bPrime(), OperationApplier.apply(a, "Hello"));
        assertEquals("Hello! World", serverOrder);
    }
}
