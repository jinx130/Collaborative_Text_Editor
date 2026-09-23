package com.yourname.editor.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentCollaboratorRepository extends JpaRepository<DocumentCollaborator, Long> {

    List<DocumentCollaborator> findByDocumentId(String documentId);

    Optional<DocumentCollaborator> findByDocumentIdAndUserEmail(String documentId, String userEmail);

    List<DocumentCollaborator> findByUserEmail(String userEmail);

    void deleteByDocumentIdAndUserId(String documentId, String userId);
}
