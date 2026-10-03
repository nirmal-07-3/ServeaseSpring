package com.application.Servease.provider.service;

import com.application.Servease.provider.dto.ProviderAdminDetailsResponse;
import com.application.Servease.provider.dto.ProviderApprovalResponse;
import com.application.Servease.provider.dto.UploadDocumentResponse;

import java.util.List;

public interface ProviderAdminService {

    List<ProviderApprovalResponse> getPendingProviders();

    ProviderApprovalResponse approveProvider(Long providerId);

    ProviderApprovalResponse rejectProvider(Long providerId);

    ProviderAdminDetailsResponse getProviderDetails(Long providerId);

    UploadDocumentResponse verifyDocument(Long documentId);
}