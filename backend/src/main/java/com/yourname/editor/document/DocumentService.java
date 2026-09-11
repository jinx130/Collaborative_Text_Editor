package com.yourname.editor.document;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public List<Document> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    public Document get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    @Transactional
    public Document create(String title, String content) {
        String t = (title == null || title.isBlank()) ? "Untitled" : title.trim();   // i have made a change there title.trin()
        String c = (content == null) ? "" : content;
        return repository.save(new Document(t, c));
    }

    @Transactional
    public Document update(String id, String title, String content) {
        Document doc = get(id);
        if (title != null) {
            String t = title.isBlank() ? "Untitled" : title.trim();
            doc.setTitle(t);
        }
        if (content != null) {
            doc.setContent(content);
        }
        return repository.save(doc);
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        repository.deleteById(id);
    }
}
