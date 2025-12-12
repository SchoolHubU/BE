package com.shu.backend.global.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadStudentCardImage(MultipartFile file);
}
