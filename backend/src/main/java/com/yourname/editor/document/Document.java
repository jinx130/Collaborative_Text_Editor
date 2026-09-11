package com.yourname.editor.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for a collaborative document (§11, §12).
 */
@Entity
@Table(name = "documents")
public class Document {

    @Id
    private  String id;

    @Column(nullable = false)
    private String title = "Untitled";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content = "";

    @Column(nullable = false)
    private long revision = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
    protected Document() {

    }
    public Document(String title, String content) {
        this.id = UUID.randomUUID().toString();   // this
        this.title = title == null ? "Untitled" : title;
        this.content = content == null ? "" : content;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();    // this is for safety like if id is not generated in constructor
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }


    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getRevision() { return revision; }
    public void setRevision(long revision) { this.revision = revision; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
