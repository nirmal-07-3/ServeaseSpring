package com.application.Servease.provider.repository;

import com.application.Servease.provider.entity.ProviderDocument;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderDocumentRepository extends JpaRepository<ProviderDocument, Long> {

    Optional<ProviderDocument> findByProviderAndDocumentType(
            ProviderProfile provider,
            DocumentType documentType
    );

    List<ProviderDocument> findByProvider(ProviderProfile provider);

}