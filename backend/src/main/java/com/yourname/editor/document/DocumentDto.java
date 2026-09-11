package com.yourname.editor.document;

import java.time.Instant;

/**
 * DTO for REST + WebSocket state transfer.
 */
public record DocumentDto(
        String id,
        String title,
        String content,
        long revision,
        Instant updatedAt) {

    public static DocumentDto from(Document doc) {
        return new DocumentDto(
                doc.getId(), doc.getTitle(), doc.getContent(),
                doc.getRevision(), doc.getUpdatedAt());
    }

    public record CreateRequest(String title, String content) {
    }

    public record UpdateRequest(String title, String content) {
    }
}
