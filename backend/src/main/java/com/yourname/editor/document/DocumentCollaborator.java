package com.yourname.editor.document;

import com.yourname.editor.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Email-invite sharing: one row per person a document is shared with.
 * VIEWER can read (live read-only feed); EDITOR can also write.
 * Only the owner manages this list.
 */
@Entity
@Table(
        name = "document_collaborators",
        uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "user_id"}))
public class DocumentCollaborator {

    public enum Role {
        VIEWER,
        EDITOR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected DocumentCollaborator() {
    }

    public DocumentCollaborator(Document document, User user, Role role) {
        this.document = document;
        this.user = user;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public User getUser() {
        return user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
