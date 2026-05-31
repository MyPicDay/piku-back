package com.pikume.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import com.pikume.back.global.storage.StorageProperties;

import java.net.URI;

@Configuration
public class S3Config {

    private final StorageProperties storageProperties;

    public S3Config(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Bean(name = "s3Client")
    public S3Client s3Client() {
        String storageType = storageProperties.getType();

        if ("s3".equalsIgnoreCase(storageType)) {
            return createS3Client();
        } else {
            return createMinioClient();
        }
    }

    /**
     * AWS S3 클라이언트 생성
     */
    private S3Client createS3Client() {
        return S3Client.builder()
                .region(Region.of(storageProperties.getRegion()))
                // ec2에 역할 설정
                .build();
    }

    /**
     * MinIO 클라이언트 생성
     */
    private S3Client createMinioClient() {
        return S3Client.builder()
                .endpointOverride(URI.create(storageProperties.serverToS3BaseUrl())) // 서버 -> S3 API 경로
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        storageProperties.getAccessKey(),
                                        storageProperties.getSecretKey())))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true) // 주소 기반 접근 설정
                                .build())
                .build();
    }
}
