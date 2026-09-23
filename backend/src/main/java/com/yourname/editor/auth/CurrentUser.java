package com.yourname.editor.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves the logged-in account. Principal is the email string stored at
 * login time. Controllers receive it; the WebSocket handler falls back to
 * the "userEmail" HTTP-session attribute when the WS principal is absent.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String email(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String s && !s.isBlank() && !s.equals("anonymousUser")) {
            return s;
        }
        return null;
    }

    public static User require(Authentication authentication, UserRepository users) {
        String email = email(authentication);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        }
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "account not found"));
    }
}
