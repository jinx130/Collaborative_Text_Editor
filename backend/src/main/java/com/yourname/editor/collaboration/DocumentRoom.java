package com.yourname.editor.collaboration;

import com.yourname.editor.ot.Operation;
import com.yourname.editor.ot.OperationApplier;
import com.yourname.editor.ot.OperationTransformer;
import com.yourname.editor.ot.OperationValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * In-memory OT room: sessions, content, revision, history.
 * Central serialization point (blog's OTServer): every incoming op is
 * transformed against all history since the client's base revision, then
 * applied, appended, and broadcast. This total order is what lets us skip
 * TP2 handling on clients.
 */
public class DocumentRoom {

    public record SubmitResult(Operation transformed, long revision, boolean noop) {
    }

    private final String documentId;
    private String content;
    private long revision;
    private final List<Operation> history = new ArrayList<>();
    private final Set<String> sessionIds = new CopyOnWriteArraySet<>();
    private final Map<String, String> sessionToClient = new ConcurrentHashMap<>();

    public DocumentRoom(String documentId, String content, long revision) {
        this.documentId = documentId;
        this.content = content == null ? "" : content;
        this.revision = revision;
    }

    public String getDocumentId() {
        return documentId;
    }

    public synchronized String getContent() {
        return content;
    }

    public synchronized long getRevision() {
        return revision;
    }

    public synchronized List<Operation> historySince(long baseRevision) {
        if (baseRevision < 0 || baseRevision > revision) {
            throw new IllegalArgumentException("stale baseRevision " + baseRevision + " (server " + revision + ")");
        }
        return new ArrayList<>(history.subList((int) baseRevision, (int) revision));
    }

    /**
     * Blog OTServer.handleOperation(): transform -> apply -> append -> bump.
     */
    public synchronized SubmitResult submit(Operation incoming, long baseRevision) {
        if (incoming == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        if (baseRevision < 0 || baseRevision > revision) {
            throw new IllegalArgumentException("stale baseRevision " + baseRevision + " (server " + revision + ")");
        }
        Operation transformed = new Operation(new ArrayList<>(incoming.getComponents())).compacted();
        for (int i = (int) baseRevision; i < revision; i++) {
            Operation prior = history.get(i);
            transformed = OperationTransformer.transformAgainst(prior, transformed);
        }
        OperationValidator.validate(transformed, content.length());
        if (transformed.isNoop()) {
            return new SubmitResult(transformed, revision, true);
        }
        content = OperationApplier.apply(transformed, content);
        history.add(transformed.compacted());
        revision++;
        return new SubmitResult(transformed.compacted(), revision, false);
    }

    public void addSession(String sessionId, String clientId) {
        sessionIds.add(sessionId);
        if (clientId != null) {
            sessionToClient.put(sessionId, clientId);
        }
    }

    public void removeSession(String sessionId) {
        sessionIds.remove(sessionId);
        sessionToClient.remove(sessionId);
    }

    public Set<String> getSessionIds() {
        return Set.copyOf(sessionIds);
    }

    public boolean isEmpty() {
        return sessionIds.isEmpty();
    }
}
