package com.yourname.editor.collaboration;

import com.yourname.editor.collaboration.protocol.ClientMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Tracks per-document presence (blog's PresenceManager): clientId -> name,
 * color (deterministic hash like the article), cursor/selection.
 */
@Component
public class PresenceTracker {

    public record PresenceEntry(String clientId, String name, String color, int cursor,
            Integer selectionStart, Integer selectionEnd) {
    }

    private final Map<String, Map<String, PresenceEntry>> byDocument = new ConcurrentHashMap<>();

    public List<ClientMessage.PresenceUser> update(String documentId, String clientId, String name, int cursor,
            Integer selectionStart, Integer selectionEnd) {
        byDocument.computeIfAbsent(documentId, k -> new ConcurrentHashMap<>())
                .put(clientId, new PresenceEntry(clientId, name == null ? "Anonymous" : name,
                        ClientMessage.colorFor(clientId), cursor, selectionStart, selectionEnd));
        return list(documentId);
    }

    public List<ClientMessage.PresenceUser> remove(String documentId, String clientId) {
        Map<String, PresenceEntry> m = byDocument.get(documentId);
        if (m != null) {
            m.remove(clientId);
        }
        return list(documentId);
    }

    public List<ClientMessage.PresenceUser> list(String documentId) {
        Map<String, PresenceEntry> m = byDocument.getOrDefault(documentId, Map.of());
        List<ClientMessage.PresenceUser> out = new ArrayList<>();
        for (PresenceEntry e : m.values()) {
            out.add(new ClientMessage.PresenceUser(e.clientId(), e.name(), e.color(), e.cursor()));
        }
        return out;
    }

    public void removeClientEverywhere(String clientId) {
        // best-effort; rooms broadcast fresh lists on leave anyway
        for (String docId : List.copyOf(byDocument.keySet())) {
            remove(docId, clientId);
        }
    }
}
