package com.yourname.editor.document;

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

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:5173")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentDto> list() {
        return service.list().stream().map(DocumentDto::from).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public DocumentDto get(@PathVariable String id) {
        return DocumentDto.from(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto create(@RequestBody(required = false) DocumentDto.CreateRequest body) {
        String title = (body == null) ? "Untitled" : body.title();
        String content = (body == null) ? "" : body.content();
        return DocumentDto.from(service.create(title, content));
    }

    @PutMapping("/{id}")
    public DocumentDto update(@PathVariable String id,
            @RequestBody(required = false) DocumentDto.UpdateRequest body) {
        String title = (body == null) ? null : body.title();
        String content = (body == null) ? null : body.content();
        return DocumentDto.from(service.update(id, title, content));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
