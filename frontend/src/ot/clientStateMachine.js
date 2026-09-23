// Blog's OTClient state machine: Synchronized / AwaitingAck / AwaitingAckWithBuffer.
import { apply, isNoop, transform } from './operations.js';

export function createClientStateMachine(initialDoc = '', initialRevision = 0) {
  const s = {
    state: 'synced', // 'synced' | 'awaiting-ack' | 'awaiting-ack-buffer'
    doc: initialDoc ?? '',
    revision: initialRevision ?? 0,
    sent: null, // operation in flight
    pending: [], // buffered ops (kept as list, composed on ack)
  };

  function composeTwo(a, b) {
    // Compose is non-trivial in general; simplest correct approach for the
    // textarea flow is to keep them as a list and re-apply sequentially.
    // We store the list and let callers send sequentially. For the wire we
    // send the concatenation via pending queue replay, so compose() here
    // just returns the list for the socket layer to drain one-by-one.
    return [a, b];
  }

  return {
    get snapshot() {
      return { state: s.state, doc: s.doc, revision: s.revision, hasPending: s.pending.length > 0 };
    },
    getDoc() {
      return s.doc;
    },
    getRevision() {
      return s.revision;
    },
    getState() {
      return s.state;
    },
    setSnapshot(doc, revision) {
      s.doc = doc ?? '';
      s.revision = revision ?? 0;
      s.sent = null;
      s.pending = [];
      s.state = 'synced';
    },
    /** Local edit: apply immediately, return op to send (or null if buffered). */
    applyLocal(op) {
      if (!op || op.length === 0 || isNoop(op)) return null;
      s.doc = apply(op, s.doc);
      if (s.state === 'synced') {
        s.sent = op;
        s.state = 'awaiting-ack';
        return op;
      }
      s.pending.push(op);
      s.state = 'awaiting-ack-buffer';
      return null;
    },
    /** Server ACK: clear in-flight, promote one buffered op to send. */
    handleAck(nextRevision) {
      s.revision = nextRevision;
      s.sent = null;
      if (s.pending.length === 0) {
        s.state = 'synced';
        return null;
      }
      // Send next buffered op (keep the rest buffered).
      const nextOp = s.pending.shift();
      s.sent = nextOp;
      s.state = s.pending.length > 0 ? 'awaiting-ack-buffer' : 'awaiting-ack';
      return nextOp;
    },
    /**
     * Remote op from server (already serialized/transformed server-side):
     * transform it against in-flight + buffered ops, apply, rewrite buffers.
     * Returns { doc, cursorShift } info for the UI.
     */
    handleRemote(remoteOp, nextRevision, transformCursor) {
      s.revision = nextRevision;
      let op = remoteOp;
      if (s.sent) {
        const [remotePrime, sentPrime] = transform(op, s.sent);
        op = remotePrime;
        s.sent = sentPrime;
      }
      const newPending = [];
      for (const p of s.pending) {
        const [remotePrime, pendingPrime] = transform(op, p);
        op = remotePrime;
        newPending.push(pendingPrime);
      }
      s.pending = newPending;
      const before = s.doc;
      s.doc = apply(op, s.doc);
      void before;
      void composeTwo;
      return { doc: s.doc, transformedRemote: op };
    },
    forceResync(doc, revision) {
      this.setSnapshot(doc, revision);
    },
  };
}
