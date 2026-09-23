package com.yourname.editor.collaboration;

import com.yourname.editor.document.DocumentOperationEntity;
import com.yourname.editor.document.DocumentOperationRepository;
import com.yourname.editor.document.DocumentService;
import com.yourname.editor.ot.Operation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Applies validated/transformed ops to the room, persists the resulting
 * content + revision, and logs each op with the author's account id so the
 * database records who changed which words.Could not delete document.
 */
@Service
public class CollaborationService {

    private final DocumentRoomRegistry rooms;
    private final DocumentService documents;
    private final DocumentOperationRepository operationLog;
    private final ObjectMapper mapper;

    public CollaborationService(DocumentRoomRegistry rooms, DocumentService documents,
            DocumentOperationRepository operationLog, ObjectMapper mapper) {
        this.rooms = rooms;
        this.documents = documents;
        this.operationLog = operationLog;
        this.mapper = mapper;
    }

    public DocumentRoom join(String documentId, String requesterEmail) {
        return rooms.getOrCreate(documentId, requesterEmail);
    }

    public String accessRole(String documentId, String requesterEmail) {
        return documents.access(requesterEmail, documentId).name();
    }

    public DocumentRoom getRoom(String documentId) {
        return rooms.get(documentId);
    }

    public void leave(String documentId, String sessionId) {
        DocumentRoom room = rooms.get(documentId);
        if (room != null) {
            room.removeSession(sessionId);
            rooms.removeIfEmpty(documentId);
        }
    }

    @Transactional
    public DocumentRoom.SubmitResult submitOperation(String documentId, String requesterEmail, String userId,
            Operation operation, long baseRevision) {
        // Write-access enforced here AND in updateContent: viewers get 403,
        // strangers get 404 — on both REST and the live socket.
        DocumentRoom room = rooms.getOrCreate(documentId, requesterEmail);
        DocumentRoom.SubmitResult result = room.submit(operation, baseRevision);
        if (!result.noop()) {
            documents.updateContent(requesterEmail, documentId, room.getContent(), room.getRevision());
            try {
                operationLog.save(new DocumentOperationEntity(documentId, result.revision(),
                        mapper.writeValueAsString(result.transformed()), userId));
            } catch (Exception e) {
                throw new IllegalStateException("could not serialize operation", e);
            }
        }
        return result;
    }
}
