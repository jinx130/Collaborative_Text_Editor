package com.yourname.editor.document;

import java.time.Instant;

/**
 * DTO for REST + WebSocket state transfer. ownerEmail/ownerName/myRole let
 * the UI render "Shared with me" sections and viewer read-only mode.
 */
public record DocumentDto(
        String id,
        String title,
        String content,
        long revision,
        Instant updatedAt,
        String ownerEmail,
        String ownerName,
        String myRole) {

    public static DocumentDto from(Document doc, String myRole) {
        return new DocumentDto(
                doc.getId(), doc.getTitle(), doc.getContent(),
                doc.getRevision(), doc.getUpdatedAt(),
                doc.getOwner() == null ? null : doc.getOwner().getEmail(),
                doc.getOwner() == null ? null : doc.getOwner().getName(),
                myRole);
    }

    public record CreateRequest(String title, String content) {
    }

    public record UpdateRequest(String title, String content) {
    }

    public record ShareRequest(String email, String role) {
    }

    public record ShareResponse(String userId, String email, String name, String role) {
    }
}
