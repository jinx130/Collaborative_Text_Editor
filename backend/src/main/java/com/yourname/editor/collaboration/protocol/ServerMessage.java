package com.yourname.editor.collaboration.protocol;

import com.yourname.editor.ot.Operation;
import java.util.List;

/**
 * Messages sent from OT server to browser clients.
 * Types: JOINED, ACK, REMOTE_OPERATION, PRESENCE, ERROR.
 */
public class ServerMessage {

    private String type;
    private String documentId;
    private Long revision;
    private String content;
    private Operation operation;
    private String senderId;
    private String message;
    private String role;
    private List<ClientMessage.PresenceUser> users;

    public ServerMessage() {
    }

    public static ServerMessage joined(String documentId, long revision, String content, String role) {
        ServerMessage m = new ServerMessage();
        m.type = "JOINED";
        m.documentId = documentId;
        m.revision = revision;
        m.content = content;
        m.role = role;
        return m;
    }

    public static ServerMessage ack(String documentId, long revision) {
        ServerMessage m = new ServerMessage();
        m.type = "ACK";
        m.documentId = documentId;
        m.revision = revision;
        return m;
    }

    public static ServerMessage remoteOperation(String documentId, Operation operation, long revision, String senderId) {
        ServerMessage m = new ServerMessage();
        m.type = "REMOTE_OPERATION";
        m.documentId = documentId;
        m.operation = operation;
        m.revision = revision;
        m.senderId = senderId;
        return m;
    }

    public static ServerMessage presence(String documentId, List<ClientMessage.PresenceUser> users) {
        ServerMessage m = new ServerMessage();
        m.type = "PRESENCE";
        m.documentId = documentId;
        m.users = users;
        return m;
    }

    public static ServerMessage error(String documentId, String message) {
        ServerMessage m = new ServerMessage();
        m.type = "ERROR";
        m.documentId = documentId;
        m.message = message;
        return m;
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

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<ClientMessage.PresenceUser> getUsers() {
        return users;
    }

    public void setUsers(List<ClientMessage.PresenceUser> users) {
        this.users = users;
    }
}
