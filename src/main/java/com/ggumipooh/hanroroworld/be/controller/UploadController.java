package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/uploads")
public class UploadController {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.public-url}")
    private String publicUrl;

    /**
     * 1단계: presigned URL 발급
     */
    @PostMapping("/prepare")
    public ResponseEntity<?> prepare(@RequestBody PrepareRequest req) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        String ext = extractExtension(req.filename());
        String objectKey = "gallery/" + userId + "/" + UUID.randomUUID() + ext;

        var presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(b -> b.bucket(bucketName).key(objectKey).contentType(req.mimeType()))
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignReq).url().toString();

        return ResponseEntity.ok(Map.of(
                "objectKey", objectKey,
                "presignedUrl", presignedUrl
        ));
    }

    /**
     * 2단계: 업로드 완료 확인 (HEAD로 파일 존재 확인)
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody ConfirmRequest req) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        // 본인 파일인지 확인
        if (!req.objectKey().startsWith("gallery/" + userId + "/")) {
            return ResponseEntity.status(403).body("forbidden");
        }

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(req.objectKey())
                    .build());
        } catch (NoSuchKeyException e) {
            return ResponseEntity.badRequest().body("file_not_found");
        }

        String fileUrl = publicUrl + "/" + req.objectKey();
        return ResponseEntity.ok(Map.of("url", fileUrl));
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return "." + filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    record PrepareRequest(String filename, String mimeType) {}
    record ConfirmRequest(String objectKey) {}
}
