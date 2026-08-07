package com.application.Servease.provider.mapper;

import com.application.Servease.provider.dto.UploadDocumentResponse;
import com.application.Servease.provider.entity.ProviderDocument;
import org.springframework.stereotype.Component;

@Component
public class ProviderDocumentMapper {

    public UploadDocumentResponse toResponse(ProviderDocument document) {

        return UploadDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .documentNumber(document.getDocumentNumber())
                .documentUrl(document.getDocumentUrl())
                .verified(document.getVerified())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}