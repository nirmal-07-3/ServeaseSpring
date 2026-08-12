package com.application.Servease.provider.service;

import com.application.Servease.provider.dto.ProviderApprovalResponse;

import java.util.List;

public interface ProviderAdminService {

    List<ProviderApprovalResponse> getPendingProviders();

    ProviderApprovalResponse approveProvider(Long providerId);

    ProviderApprovalResponse rejectProvider(Long providerId);
}