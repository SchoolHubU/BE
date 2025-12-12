//package com.shu.backend.global.file;
//
//import com.shu.backend.domain.user.exception.UserException;
//import com.shu.backend.domain.user.exception.status.UserErrorStatus;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//
//import java.io.IOException;
//import java.util.UUID;
//
////배포 후 LocalfileStorageService -> S3FileStorageService 변경
//@Service
//@Profile("prod")  //  prod 프로필일 때만 활성화됨
//@RequiredArgsConstructor
//public class S3FileStorageService implements FileStorageService {
//
//    private final S3Client s3Client;
//    private final FileStorageProperties props;
//
//    @Override
//    public String uploadStudentCardImage(MultipartFile file) {
//
//        String originalFilename = file.getOriginalFilename();
//        String ext = "";
//
//        if (originalFilename != null && originalFilename.contains(".")) {
//            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
//        }
//
//        String key = props.getStudentCardDir() + "/" + UUID.randomUUID() + ext;
//
//        try {
//            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                    .bucket(props.getBucket())
//                    .key(key)
//                    .contentType(file.getContentType())
//                    .build();
//
//            s3Client.putObject(
//                    putObjectRequest,
//                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
//            );
//
//            // CDN or S3 URL 반환
//            return props.getBaseUrl() + "/" + key;
//
//        } catch (IOException e) {
//            throw new UserException(UserErrorStatus.USER_STUDENT_CARD_UPLOAD_FAIL);
//        }
//    }
//}