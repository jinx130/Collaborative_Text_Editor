package com.yourname.editor.auth;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Token auth: signup/login return a JWT the browser keeps in sessionStorage
 * (per tab) and sends as {@code Authorization: Bearer}. Logout is purely
 * client-side (discard the token). No server sessions exist.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record SignupRequest(String email, String name, String password) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record UserDto(String id, String email, String name) {
        static UserDto from(User user) {
            return new UserDto(user.getId(), user.getEmail(), user.getName());
        }
    }

    public record AuthResponse(UserDto user, String token) {
    }

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@RequestBody(required = false) SignupRequest body) {
        String email = body == null || body.email() == null ? "" : body.email().trim().toLowerCase();
        String name = body == null || body.name() == null || body.name().isBlank() ? "Anonymous" : body.name().trim();
        String password = body == null ? null : body.password();
        if (!email.contains("@") || email.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "valid email required");
        }
        if (password == null || password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be at least 6 characters");
        }
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }
        User user = users.save(new User(email, name, passwordEncoder.encode(password)));
        return new AuthResponse(UserDto.from(user), jwt.issue(email));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody(required = false) LoginRequest body) {
        String email = body == null || body.email() == null ? "" : body.email().trim().toLowerCase();
        String password = body == null ? null : body.password();
        User user = users.findByEmail(email).orElse(null);
        if (user == null || password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password");
        }
        return new AuthResponse(UserDto.from(user), jwt.issue(email));
    }

    @PostMapping("/logout")
    public void logout() {
        // Stateless: the client discards its token. Kept as an endpoint so
        // the UI has one place to call (and to clear any residual context).
        SecurityContextHolder.clearContext();
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        User user = CurrentUser.require(authentication, users);
        return UserDto.from(user);
    }
}
