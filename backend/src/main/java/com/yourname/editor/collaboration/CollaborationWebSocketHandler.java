package com.yourname.editor.collaboration;

import com.yourname.editor.auth.JwtService;
import com.yourname.editor.auth.User;
import com.yourname.editor.auth.UserRepository;
import com.yourname.editor.collaboration.protocol.ClientMessage;
import com.yourname.editor.collaboration.protocol.ServerMessage;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Live sync handler (OTServer + broadcast/ack), now authenticated:
 * only logged-in sessions get past the handshake, every join re-checks
 * document ownership, presence names come from the account (client-sent
 * names are ignored), and each op is logged with the author's account id.
 */
@Component
public class CollaborationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper;
    private final CollaborationService collaboration;
    private final PresenceTracker presence;
    private final UserRepository users;
    private final JwtService jwt;
    private final Map<String, String> sessionToDoc = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToClient = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToEmail = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUserId = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> liveSessions = new ConcurrentHashMap<>();

    public CollaborationWebSocketHandler(ObjectMapper mapper, CollaborationService collaboration,
            PresenceTracker presence, UserRepository users, JwtService jwt) {
        this.mapper = mapper;
        this.collaboration = collaboration;
        this.presence = presence;
        this.users = users;
        this.jwt = jwt;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        liveSessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        liveSessions.remove(session.getId());
        String docId = sessionToDoc.remove(session.getId());
        String clientId = sessionToClient.remove(session.getId());
        sessionToEmail.remove(session.getId());
        sessionToUserId.remove(session.getId());
        if (docId != null) {
            collaboration.leave(docId, session.getId());
            if (clientId != null) {
                List<ClientMessage.PresenceUser> usersLeft = presence.remove(docId, clientId);
                broadcast(docId, ServerMessage.presence(docId, usersLeft), null);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        liveSessions.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ClientMessage in;
        try {
            in = mapper.readValue(message.getPayload(), ClientMessage.class);
        } catch (Exception e) {
            String payload = message.getPayload();
            String snippet = payload == null ? "null"
                    : payload.length() > 200 ? payload.substring(0, 200) + "..." : payload;
            System.err.println("WARN: rejecting unparsable WS payload: " + snippet + " (" + e.getMessage() + ")");
            e.printStackTrace(System.err);
            send(session, ServerMessage.error(null, "invalid message (expected JSON JOIN_DOCUMENT/OPERATION)"));
            return;
        }
        if (in.getType() == null) {
            send(session, ServerMessage.error(in.getDocumentId(), "missing type"));
            return;
        }
        switch (in.getType()) {
            case "JOIN_DOCUMENT" -> handleJoin(session, in);
            case "OPERATION" -> handleOperation(session, in);
            case "LEAVE_DOCUMENT" -> handleLeave(session, in);
            case "PRESENCE" -> handlePresence(session, in);
            default -> send(session, ServerMessage.error(in.getDocumentId(), "unknown type " + in.getType()));
        }
    }

    /**
     * Email for a socket: the per-message JWT wins (each tab carries its own
     * account), then the WS principal, then the already-joined mapping.
     */
    private String resolveEmail(WebSocketSession session, ClientMessage in) {
        if (in != null && in.getToken() != null && !in.getToken().isBlank()) {
            String email = jwt.verify(in.getToken());
            if (email != null && !email.isBlank()) {
                sessionToEmail.put(session.getId(), email);
                return email;
            }
        }
        Principal principal = session.getPrincipal();
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()
                && !principal.getName().equals("anonymousUser")) {
            return principal.getName();
        }
        String known = sessionToEmail.get(session.getId());
        if (known != null && !known.isBlank()) {
            return known;
        }
        return null;
    }

    private void handleJoin(WebSocketSession session, ClientMessage in) throws Exception {
        if (in.getDocumentId() == null) {
            send(session, ServerMessage.error(null, "documentId required"));
            return;
        }
        String email = resolveEmail(session, in);
        if (email == null) {
            send(session, ServerMessage.error(in.getDocumentId(), "login required"));
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("login required"));
            return;
        }
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            send(session, ServerMessage.error(in.getDocumentId(), "account not found"));
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("account not found"));
            return;
        }
        String clientId = in.getClientId() == null ? session.getId() : in.getClientId();
        DocumentRoom room;
        try {
            room = collaboration.join(in.getDocumentId(), email);
        } catch (ResponseStatusException e) {
            send(session, ServerMessage.error(in.getDocumentId(), "document not found"));
            return;
        }
        room.addSession(session.getId(), clientId);
        sessionToDoc.put(session.getId(), in.getDocumentId());
        sessionToClient.put(session.getId(), clientId);
        sessionToEmail.put(session.getId(), email);
        sessionToUserId.put(session.getId(), user.getId());
        String role = collaboration.accessRole(in.getDocumentId(), email);
        send(session, ServerMessage.joined(in.getDocumentId(), room.getRevision(), room.getContent(), role));
        // Account name wins: client-sent names are ignored.
        List<ClientMessage.PresenceUser> all = presence.update(in.getDocumentId(), clientId,
                user.getName(), in.getCursor() == null ? 0 : in.getCursor(), in.getSelectionStart(),
                in.getSelectionEnd());
        broadcast(in.getDocumentId(), ServerMessage.presence(in.getDocumentId(), all), null);
    }

    private void handleOperation(WebSocketSession session, ClientMessage in) throws Exception {
        if (in.getDocumentId() == null || in.getOperation() == null || in.getBaseRevision() == null) {
            send(session, ServerMessage.error(in.getDocumentId(), "documentId/operation/baseRevision required"));
            return;
        }
        String email = resolveEmail(session, in);
        if (email == null) {
            email = sessionToEmail.get(session.getId());
        }
        String userId = sessionToUserId.get(session.getId());
        if (email == null) {
            send(session, ServerMessage.error(in.getDocumentId(), "login required"));
            return;
        }
        String clientId = sessionToClient.getOrDefault(session.getId(),
                in.getClientId() == null ? session.getId() : in.getClientId());
        DocumentRoom.SubmitResult result;
        try {
            result = collaboration.submitOperation(in.getDocumentId(), email, userId, in.getOperation(),
                    in.getBaseRevision());
        } catch (IllegalArgumentException e) {
            send(session, ServerMessage.error(in.getDocumentId(), e.getMessage()));
            DocumentRoom room = collaboration.join(in.getDocumentId(), email);
            send(session, ServerMessage.joined(in.getDocumentId(), room.getRevision(), room.getContent(),
                    collaboration.accessRole(in.getDocumentId(), email)));
            return;
        } catch (ResponseStatusException e) {
            String reason = e.getReason() == null ? "request failed" : e.getReason();
            send(session, ServerMessage.error(in.getDocumentId(), reason));
            return;
        }
        send(session, ServerMessage.ack(in.getDocumentId(), result.revision()));
        if (!result.noop()) {
            broadcast(in.getDocumentId(),
                    ServerMessage.remoteOperation(in.getDocumentId(), result.transformed(), result.revision(), clientId),
                    session.getId());
        }
    }

    private void handleLeave(WebSocketSession session, ClientMessage in) throws Exception {
        String docId = in.getDocumentId() == null ? sessionToDoc.get(session.getId()) : in.getDocumentId();
        if (docId != null) {
            collaboration.leave(docId, session.getId());
            String clientId = sessionToClient.get(session.getId());
            if (clientId != null) {
                broadcast(docId, ServerMessage.presence(docId, presence.remove(docId, clientId)), null);
            }
        }
        sessionToDoc.remove(session.getId());
    }

    private void handlePresence(WebSocketSession session, ClientMessage in) throws Exception {
        if (in.getDocumentId() == null) {
            return;
        }
        String email = resolveEmail(session, in);
        if (email == null) {
            email = sessionToEmail.get(session.getId());
        }
        if (email == null) {
            return;
        }
        String displayName = users.findByEmail(email).map(User::getName).orElse("Anonymous");
        String clientId = sessionToClient.getOrDefault(session.getId(),
                in.getClientId() == null ? session.getId() : in.getClientId());
        List<ClientMessage.PresenceUser> all = presence.update(in.getDocumentId(), clientId, displayName,
                in.getCursor() == null ? 0 : in.getCursor(), in.getSelectionStart(), in.getSelectionEnd());
        broadcast(in.getDocumentId(), ServerMessage.presence(in.getDocumentId(), all), null);
    }

    private void send(WebSocketSession session, ServerMessage msg) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
        }
    }

    private void broadcast(String docId, ServerMessage msg, String excludeSessionId) {
        String payload;
        try {
            payload = mapper.writeValueAsString(msg);
        } catch (Exception e) {
            return;
        }
        DocumentRoom room = collaboration.getRoom(docId);
        if (room == null) {
            return;
        }
        for (String sessionId : room.getSessionIds()) {
            if (excludeSessionId != null && excludeSessionId.equals(sessionId)) {
                continue;
            }
            WebSocketSession s = liveSessions.get(sessionId);
            if (s != null && s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(payload));
                } catch (Exception ignored) {
                    // drop failed sends; close handler cleans up
                }
            }
        }
    }
}
