package com.yourname.editor.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findByOwnerEmailOrderByUpdatedAtDesc(String ownerEmail);

    Optional<Document> findByIdAndOwnerEmail(String id, String ownerEmail);

    boolean existsByIdAndOwnerEmail(String id, String ownerEmail);
}
