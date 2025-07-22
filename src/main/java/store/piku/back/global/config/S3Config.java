package store.piku.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import store.piku.back.diary.service.StorageProperties;

@Configuration
public class S3Config {

    private final StorageProperties storageProperties;

    public S3Config(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(storageProperties.getRegion()))
                // ec2에 직접 할당
                // .credentialsProvider(
                //         StaticCredentialsProvider.create(AwsBasicCredentials.create(storageProperties.getAccessKey(), storageProperties.getSecretKey()))
                // )
                .build();
    }
}


