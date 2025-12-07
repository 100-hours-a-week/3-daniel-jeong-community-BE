package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.config.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {
    
    private final S3Properties s3Properties;
    
    // S3 Presigned URL 생성
    public String generatePresignedUrl(String objectKey, String contentType) {
        Region region = Region.of(s3Properties.getRegion());
        
        try (S3Presigner presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(s3Properties.getPresignedUrlExpirationMinutes()))
                    .putObjectRequest(putObjectRequest)
                    .build();
            
            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            return presignedRequest.url().toString();
        }
    }
    
    // S3 Public URL 생성 (objectKey로부터 접근 가능한 Public URL 반환)
    public String generatePublicUrl(String objectKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Properties.getBucketName(),
                s3Properties.getRegion(),
                objectKey);
    }
    
    // S3에 직접 파일 업로드 (회원가입 시)
    public String uploadFile(String objectKey, MultipartFile file) throws IOException {
        Region region = Region.of(s3Properties.getRegion());
        
        try (S3Client s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()) {
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            return generatePublicUrl(objectKey);
        }
    }
}
