package store.piku.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import store.piku.back.diary.service.StorageProperties;

import java.net.URI;

@Configuration
public class S3Config {

    private final StorageProperties storageProperties;

    public S3Config(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Bean(name = "s3Client")
    @Profile("prod")
    public S3Client s3ClientProd() {
        return S3Client.builder()
                .region(Region.of(storageProperties.getRegion()))
                // ec2에 역할 설정
                .build();
    }

    @Bean(name = "s3Client")
    @Profile("dev")
    public S3Client s3ClientDev() {
        return S3Client.builder()
                .endpointOverride(URI.create(storageProperties.getEndpoint())) // MinIO 서버 주소
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        storageProperties.getAccessKey(),
                                        storageProperties.getSecretKey()
                                )
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true) // 주소 기반 접근 설정
                                .build()
                )
                .build();
    }
}