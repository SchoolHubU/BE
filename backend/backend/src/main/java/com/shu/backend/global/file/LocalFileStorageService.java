package com.shu.backend.global.file;

import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload-dir.student-card}")
    private String studentCardUploadDir; // uploads/student-card 같은 값

    @Value("${server.port}")
    private int serverPort;  // 8080

    @Override
    public String uploadStudentCardImage(MultipartFile file) {

        String originalFilename = file.getOriginalFilename();
        String ext = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedFileName = UUID.randomUUID() + ext;

        try {
            Path uploadPath = Paths.get(studentCardUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetPath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath);

            // 일단은 로컬 접근 URL 간단히
            return "http://localhost:" + serverPort + "/uploads/student-card/" + storedFileName;

        } catch (IOException e) {
            throw new UserException(UserErrorStatus.USER_STUDENT_CARD_UPLOAD_FAIL);
        }
    }
}