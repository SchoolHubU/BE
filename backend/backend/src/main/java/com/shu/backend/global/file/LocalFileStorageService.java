package com.shu.backend.global.file;

import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
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
    private String studentCardUploadDir;

    @Value("${file.upload-dir.chat}")
    private String chatUploadDir;

    @Value("${server.port}")
    private int serverPort;

    @Override
    public String uploadStudentCardImage(MultipartFile file) {
        return upload(file, studentCardUploadDir, "/uploads/student-card/",
                () -> new UserException(UserErrorStatus.USER_STUDENT_CARD_UPLOAD_FAIL));
    }

    @Override
    public String uploadChatImage(MultipartFile file) {
        return upload(file, chatUploadDir, "/uploads/chat/",
                () -> new ChatMessageException(ChatMessageErrorStatus.CHAT_IMAGE_UPLOAD_FAIL));
    }

    private String upload(
            MultipartFile file,
            String uploadDir,
            String publicPath,
            java.util.function.Supplier<RuntimeException> onFail
    ) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedFileName = UUID.randomUUID() + ext;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetPath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath);

            return "http://localhost:" + serverPort + publicPath + storedFileName;

        } catch (IOException e) {
            throw onFail.get();
        }
    }
}