package com.yourname.editor.document;

import com.yourname.editor.auth.CurrentUser;
import com.yourname.editor.auth.User;
import com.yourname.editor.auth.UserRepository;
import com.yourname.editor.document.DocumentCollaborator.Role;
import com.yourname.editor.document.DocumentService.Access;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "")// your app vercel somthing i dont exactly know
public class DocumentController {

    private final DocumentService service;
    private final UserRepository users;

    public DocumentController(DocumentService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    private User current(Authentication authentication) {
        return CurrentUser.require(authentication, users);
    }

    private String roleOf(User user, String documentId) {
        Access access = service.access(user.getEmail(), documentId);
        return access.name();
    }

    @GetMapping
    public List<DocumentDto> list(Authentication authentication) {
        User owner = current(authentication);
        return service.list(owner).stream()
                .map(doc -> DocumentDto.from(doc, Access.OWNER.name()))
                .collect(Collectors.toList());
    }

    @GetMapping("/shared")
    public List<DocumentDto> shared(Authentication authentication) {
        User user = current(authentication);
        return service.listShared(user.getEmail()).stream()
                .map(e -> DocumentDto.from(e.document(), e.role().name()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public DocumentDto get(@PathVariable String id, Authentication authentication) {
        User user = current(authentication);
        return DocumentDto.from(service.get(user, id), roleOf(user, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto create(@RequestBody(required = false) DocumentDto.CreateRequest body,
            Authentication authentication) {
        String title = (body == null) ? "Untitled" : body.title();
        String content = (body == null) ? "" : body.content();
        return DocumentDto.from(service.create(current(authentication), title, content), Access.OWNER.name());
    }

    @PutMapping("/{id}")
    public DocumentDto update(@PathVariable String id,
            @RequestBody(required = false) DocumentDto.UpdateRequest body,
            Authentication authentication) {
        User user = current(authentication);
        String title = (body == null) ? null : body.title();
        String content = (body == null) ? null : body.content();
        return DocumentDto.from(service.update(user, id, title, content), roleOf(user, id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, Authentication authentication) {
        service.delete(current(authentication), id);
    }

    @GetMapping("/{id}/shares")
    public List<DocumentDto.ShareResponse> shares(@PathVariable String id, Authentication authentication) {
        User user = current(authentication);
        return service.listShares(user, id).stream()
                .map(e -> new DocumentDto.ShareResponse(e.userId(), e.email(), e.name(), e.role().name()))
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/shares")
    public DocumentDto.ShareResponse share(@PathVariable String id,
            @RequestBody(required = false) DocumentDto.ShareRequest body,
            Authentication authentication) {
        User user = current(authentication);
        String email = body == null ? null : body.email();
        Role role;
        try {
            role = Role.valueOf(String.valueOf(body == null ? null : body.role()).toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role must be VIEWER or EDITOR");
        }
        DocumentService.ShareEntry entry = service.share(user, id, email, role);
        return new DocumentDto.ShareResponse(entry.userId(), entry.email(), entry.name(), entry.role().name());
    }

    @DeleteMapping("/{id}/shares/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unshare(@PathVariable String id, @PathVariable String userId, Authentication authentication) {
        service.unshare(current(authentication), id, userId);
    }
}
