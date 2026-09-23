package com.yourname.editor.collaboration;

import com.yourname.editor.document.Document;
import com.yourname.editor.document.DocumentService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Registry of live {@link DocumentRoom}s, lazily seeded from the database.
 * Rooms are always opened through an ownership check, so private documents
 * can't leak through the WebSocket path either.
 */
@Component
public class DocumentRoomRegistry {

    private final DocumentService documents;
    private final Map<String, DocumentRoom> rooms = new ConcurrentHashMap<>();

    public DocumentRoomRegistry(DocumentService documents) {
        this.documents = documents;
    }

    public DocumentRoom getOrCreate(String documentId, String ownerEmail) {
        DocumentRoom existing = rooms.get(documentId);
        if (existing != null) {
            return existing;
        }
        // Access check first: owners and collaborators enter; strangers 404.
        // Write access is enforced separately on every submitted op.
        Document doc = documents.getForRead(ownerEmail, documentId);
        DocumentRoom fresh = new DocumentRoom(documentId, doc.getContent(), doc.getRevision());
        DocumentRoom raced = rooms.putIfAbsent(documentId, fresh);
        return raced != null ? raced : fresh;
    }

    public DocumentRoom get(String documentId) {
        return rooms.get(documentId);
    }

    public void removeIfEmpty(String documentId) {
        DocumentRoom room = rooms.get(documentId);
        if (room != null && room.isEmpty()) {
            rooms.remove(documentId, room);
        }
    }
}
