package com.yourname.editor.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Optional persistent operation log (§11): one row per applied op.
 */
@Entity
@Table(name = "document_operations")
public class DocumentOperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String documentId;

    @Column(nullable = false)
    private long revision;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String operationJson;

    private String userId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DocumentOperationEntity() {
    }

    public DocumentOperationEntity(String documentId, long revision, String operationJson, String userId) {
        this.documentId = documentId;
        this.revision = revision;
        this.operationJson = operationJson;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getDocumentId() { return documentId; }
    public long getRevision() { return revision; }
    public String getOperationJson() { return operationJson; }
    public String getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
}
