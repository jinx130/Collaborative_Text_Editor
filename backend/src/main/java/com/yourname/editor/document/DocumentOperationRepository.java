package com.yourname.editor.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentOperationRepository extends JpaRepository<DocumentOperationEntity, Long> {

    List<DocumentOperationEntity> findByDocumentIdOrderByRevisionAsc(String documentId);
}
