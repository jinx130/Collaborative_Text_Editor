package com.yourname.editor.collaboration.protocol;

import com.yourname.editor.ot.Operation;
import java.util.List;

/**
 * Messages sent from browser client to the OT server.
 * Types: JOIN_DOCUMENT, OPERATION, LEAVE_DOCUMENT, PRESENCE.
 */
public class ClientMessage {

    private String type;
    private String documentId;
    private String clientId;
    private String token;
    private String name;
    private Long baseRevision;
    private Operation operation;
    private Integer cursor;
    private Integer selectionStart;
    private Integer selectionEnd;

    public ClientMessage() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBaseRevision() {
        return baseRevision;
    }

    public void setBaseRevision(Long baseRevision) {
        this.baseRevision = baseRevision;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Integer getCursor() {
        return cursor;
    }

    public void setCursor(Integer cursor) {
        this.cursor = cursor;
    }

    public Integer getSelectionStart() {
        return selectionStart;
    }

    public void setSelectionStart(Integer selectionStart) {
        this.selectionStart = selectionStart;
    }

    public Integer getSelectionEnd() {
        return selectionEnd;
    }

    public void setSelectionEnd(Integer selectionEnd) {
        this.selectionEnd = selectionEnd;
    }

    public record PresenceUser(
            String clientId,
            String name,
            String color,
            int cursor) {
    }

    public static String colorFor(String clientId) {
        int hash = 0;
        if (clientId != null) {
            for (char ch : clientId.toCharArray()) {
                hash = ((hash << 5) - hash) + ch;
            }
        }
        int hue = Math.abs(hash) % 360;
        return "hsl(" + hue + ", 70%, 50%)";
    }

    public static PresenceUser toPresenceUser(String clientId, String name, int cursor) {
        return new PresenceUser(clientId, name == null ? "Anonymous" : name, colorFor(clientId), cursor);
    }

    public List<String> validate() {
        return List.of();
    }
}
