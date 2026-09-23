package com.yourname.editor.document;

import com.yourname.editor.auth.User;
import com.yourname.editor.auth.UserRepository;
import com.yourname.editor.document.DocumentCollaborator.Role;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

    public enum Access {
        NONE,
        VIEWER,
        EDITOR,
        OWNER;

        public boolean canRead() {
            return this != NONE;
        }

        public boolean canWrite() {
            return this == OWNER || this == EDITOR;
        }
    }

    public record SharedEntry(Document document, Role role) {
    }

    public record ShareEntry(String userId, String email, String name, Role role) {
    }

    private final DocumentRepository repository;
    private final DocumentCollaboratorRepository collaborators;
    private final UserRepository users;

    public DocumentService(DocumentRepository repository, DocumentCollaboratorRepository collaborators,
            UserRepository users) {
        this.repository = repository;
        this.collaborators = collaborators;
        this.users = users;
    }

    /** Effective access for an account on a document (NONE = invisible, 404). */
    @Transactional(readOnly = true)
    public Access access(String requesterEmail, String documentId) {
        return accessInternal(requesterEmail, documentId);
    }

    private Access accessInternal(String requesterEmail, String documentId) {
        Document doc = repository.findById(documentId).orElse(null);
        if (doc == null) {
            return Access.NONE;
        }
        if (doc.getOwner() != null && requesterEmail.equals(doc.getOwner().getEmail())) {
            return Access.OWNER;
        }
        return collaborators.findByDocumentIdAndUserEmail(documentId, requesterEmail)
                .map(c -> c.getRole() == Role.EDITOR ? Access.EDITOR : Access.VIEWER)
                .orElse(Access.NONE);
    }

    /** Private by default: a user lists only their own documents. */
    public List<Document> list(User owner) {
        return repository.findByOwnerEmailOrderByUpdatedAtDesc(owner.getEmail());
    }

    /** Documents others shared with this account (any role). */
    @Transactional(readOnly = true)
    public List<SharedEntry> listShared(String requesterEmail) {
        return collaborators.findByUserEmail(requesterEmail).stream()
                .map(c -> new SharedEntry(c.getDocument(), c.getRole()))
                .toList();
    }

    /** 404 for invisible docs (other users' unshared docs and missing alike). */
    @Transactional(readOnly = true)
    public Document get(User owner, String id) {
        return getForRead(owner.getEmail(), id);
    }

    @Transactional(readOnly = true)
    public Document getForRead(String requesterEmail, String id) {
        Access access = accessInternal(requesterEmail, id);
        if (!access.canRead()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    @Transactional(readOnly = true)
    public Document getOwned(String ownerEmail, String id) {
        return repository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    @Transactional
    public Document create(User owner, String title, String content) {
        String t = (title == null || title.isBlank()) ? "Untitled" : title.trim();   // i have made a change there title.trin()
        String c = (content == null) ? "" : content;
        Document doc = new Document(t, c);
        doc.setOwner(owner);
        return repository.save(doc);
    }

    @Transactional
    public Document update(User owner, String id, String title, String content) {
        Access access = accessInternal(owner.getEmail(), id);
        if (!access.canRead()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        if (!access.canWrite()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "viewers cannot edit");
        }
        Document doc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (title != null) {
            String t = title.isBlank() ? "Untitled" : title.trim();
            doc.setTitle(t);
        }
        if (content != null) {
            doc.setContent(content);
        }
        return repository.save(doc);
    }

    @Transactional
    public void delete(User owner, String id) {
        Access access = accessInternal(owner.getEmail(), id);
        if (!access.canRead()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        if (access != Access.OWNER) {
            // Only owners delete; editors who lose interest just leave it
            // (unshare yourself via DELETE /shares/{yourId} is owner-managed,
            // so editors simply stop opening it).
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only the owner can delete");
        }
        repository.deleteById(id);
    }

    /** Persists OT-room state after a live edit (write access re-checked). */
    @Transactional
    public void updateContent(String requesterEmail, String id, String content, long revision) {
        Access access = accessInternal(requesterEmail, id);
        if (!access.canRead()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        if (!access.canWrite()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "viewers cannot edit");
        }
        Document doc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        doc.setContent(content);
        doc.setRevision(revision);
        repository.save(doc);
    }

    // ---- Sharing (owner manages, invite by email) ----

    @Transactional(readOnly = true)
    public List<ShareEntry> listShares(User requester, String documentId) {
        // Invisible docs 404; any reader may see the collaborator list.
        getForRead(requester.getEmail(), documentId);
        return collaborators.findByDocumentId(documentId).stream()
                .map(c -> new ShareEntry(c.getUser().getId(), c.getUser().getEmail(), c.getUser().getName(),
                        c.getRole()))
                .toList();
    }

    @Transactional
    public ShareEntry share(User owner, String documentId, String targetEmail, Role role) {
        Document doc = repository.findById(documentId).orElse(null);
        if (doc == null || doc.getOwner() == null || !owner.getEmail().equals(doc.getOwner().getEmail())) {
            // Non-owners get 404 here (don't reveal), owners of missing docs too.
            Access access = doc == null ? Access.NONE : accessInternal(owner.getEmail(), documentId);
            if (!access.canRead()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only the owner can share");
        }
        User target = users.findByEmail(targetEmail == null ? "" : targetEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        if (target.getEmail().equals(owner.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "you already own this document");
        }
        DocumentCollaborator existing =
                collaborators.findByDocumentIdAndUserEmail(documentId, target.getEmail()).orElse(null);
        if (existing != null) {
            existing.setRole(role);
            collaborators.save(existing);
            return new ShareEntry(target.getId(), target.getEmail(), target.getName(), role);
        }
        DocumentCollaborator created = collaborators.save(new DocumentCollaborator(doc, target, role));
        return new ShareEntry(target.getId(), target.getEmail(), target.getName(), created.getRole());
    }

    @Transactional
    public void unshare(User owner, String documentId, String targetUserId) {
        Document doc = repository.findById(documentId).orElse(null);
        if (doc == null || doc.getOwner() == null || !owner.getEmail().equals(doc.getOwner().getEmail())) {
            Access access = doc == null ? Access.NONE : accessInternal(owner.getEmail(), documentId);
            if (!access.canRead()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only the owner can unshare");
        }
        collaborators.deleteByDocumentIdAndUserId(documentId, targetUserId);
    }
}
