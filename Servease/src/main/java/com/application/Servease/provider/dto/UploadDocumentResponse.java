    package com.application.Servease.provider.dto;

    import com.application.Servease.provider.enums.DocumentType;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class UploadDocumentResponse {

        private Long id;

        private DocumentType documentType;

        private String documentNumber;

        private String documentUrl;

        private Boolean verified;

        private LocalDateTime uploadedAt;
    }