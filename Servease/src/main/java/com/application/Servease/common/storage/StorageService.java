package com.application.Servease.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String uploadFile(MultipartFile file, String folder);

    void deleteFile(String filePath);

}