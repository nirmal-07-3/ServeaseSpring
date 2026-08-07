package com.application.Servease.common.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public String uploadFile(MultipartFile file, String folder) {

        try {

            if (file == null || file.isEmpty()) {
                throw new FileStorageException("File is empty.");
            }

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

            String fileName = UUID.randomUUID() + "_" + originalFilename;

            Path uploadPath = Paths.get(fileStorageProperties.getUploadDir(), folder);

            String contentType = file.getContentType();

            if (contentType == null ||
                    (!contentType.startsWith("image/")
                            && !contentType.equals("application/pdf"))) {

                throw new FileStorageException(
                        "Only image and PDF files are allowed."
                );
            }


            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetLocation = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return folder + "/" + fileName;

        } catch (IOException ex) {

            throw new FileStorageException("Could not store file.", ex);

        }
    }

    @Override
    public void deleteFile(String filePath) {

        try {

            Path path = Paths.get(fileStorageProperties.getUploadDir())
                    .resolve(filePath);

            Files.deleteIfExists(path);

        } catch (IOException ex) {

            throw new FileStorageException("Could not delete file.", ex);

        }
    }
}