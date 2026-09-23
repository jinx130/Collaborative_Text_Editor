package com.yourname.editor.ot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * OT transform for two concurrent operations sharing the same base document.
 *
 * <p>Implements the blog's transformation functions (insert/insert,
 * insert/delete, delete/delete, delete/insert) generalized to Wave-style
 * retain/insert/delete components. Returns a symmetric pair
 * {@code (a', b')} satisfying TP1:
 * {@code apply(apply(S,a),b') == apply(apply(S,b),a')}.</p>
 *
 * <p>Tie-break: when both sides insert at the same position, the first
 * argument {@code a} wins (its text comes first). The server always calls
 * {@code transform(historyOp, incomingOp)} so history wins, giving a total
 * order and making TP2 irrelevant (central serialization).</p>
 */
public final class OperationTransformer {

    private OperationTransformer() {
    }

    public record TransformResult(Operation aPrime, Operation bPrime) {
    }

    public static TransformResult transform(Operation a, Operation b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("operations must not be null");
        }
        if (a.baseLength() != b.baseLength()) {
            throw new IllegalArgumentException(
                    "concurrent operations must share baseLength: " + a.baseLength() + " vs " + b.baseLength());
        }

        OperationBuilder aPrime = new OperationBuilder();
        OperationBuilder bPrime = new OperationBuilder();

        Deque<OpCursor> qa = cursorOf(a.getComponents());
        Deque<OpCursor> qb = cursorOf(b.getComponents());

        OpCursor ca = qa.pollFirst();
        OpCursor cb = qb.pollFirst();

        while (ca != null || cb != null) {
            // Insert has no base chars: emit + retain on the other side.
            // a wins ties because we drain a-inserts first.
            if (ca != null && ca.type == OperationComponent.Type.INSERT) {
                aPrime.insert(ca.text);
                bPrime.retain(ca.text.length());
                ca = qa.pollFirst();
                continue;
            }
            if (cb != null && cb.type == OperationComponent.Type.INSERT) {
                bPrime.insert(cb.text);
                aPrime.retain(cb.text.length());
                cb = qb.pollFirst();
                continue;
            }
            if (ca == null || cb == null) {
                // One side exhausted; the other can only be trailing retain
                // (inserts already drained above). Emit it on both sides.
                if (ca != null) {
                    if (ca.type != OperationComponent.Type.RETAIN) {
                        throw new IllegalStateException("operation length mismatch during transform");
                    }
                    aPrime.retain(ca.remaining);
                    bPrime.retain(ca.remaining);
                    ca = qa.pollFirst();
                    continue;
                }
                if (cb != null) {
                    if (cb.type != OperationComponent.Type.RETAIN) {
                        throw new IllegalStateException("operation length mismatch during transform");
                    }
                    aPrime.retain(cb.remaining);
                    bPrime.retain(cb.remaining);
                    cb = qb.pollFirst();
                    continue;
                }
                break;
            }

            // Both consume base document: retain/delete.
            int min = Math.min(ca.remaining, cb.remaining);
            boolean aRetain = ca.type == OperationComponent.Type.RETAIN;
            boolean bRetain = cb.type == OperationComponent.Type.RETAIN;

            if (aRetain && bRetain) {
                aPrime.retain(min);
                bPrime.retain(min);
            } else if (!aRetain && !bRetain) {
                // delete vs delete: same chars deleted by both -> drop from both primes.
            } else if (aRetain) {
                // a retain, b delete -> b's delete survives, a emits nothing.
                bPrime.delete(min);
            } else {
                // a delete, b retain -> a's delete survives, b emits nothing.
                aPrime.delete(min);
            }

            ca.remaining -= min;
            cb.remaining -= min;
            if (ca.remaining == 0) {
                ca = qa.pollFirst();
            }
            if (cb.remaining == 0) {
                cb = qb.pollFirst();
            }
        }

        return new TransformResult(aPrime.build(), bPrime.build());
    }

    /** Single-sided transform: transform {@code incoming} against applied {@code prior}. */
    public static Operation transformAgainst(Operation prior, Operation incoming) {
        return transform(prior, incoming).bPrime();
    }

    private static Deque<OpCursor> cursorOf(List<OperationComponent> components) {
        Deque<OpCursor> q = new ArrayDeque<>();
        if (components != null) {
            for (OperationComponent c : components) {
                if (c == null || c.getType() == null) {
                    continue;
                }
                if (c.isInsert()) {
                    String text = c.getText() == null ? "" : c.getText();
                    if (!text.isEmpty()) {
                        q.addLast(new OpCursor(OperationComponent.Type.INSERT, text.length(), text));
                    }
                } else if (c.getCount() > 0) {
                    q.addLast(new OpCursor(c.getType(), c.getCount(), null));
                }
            }
        }
        return q;
    }

    private static final class OpCursor {
        final OperationComponent.Type type;
        int remaining;
        final String text;

        OpCursor(OperationComponent.Type type, int remaining, String text) {
            this.type = type;
            this.remaining = remaining;
            this.text = text;
        }
    }
}
