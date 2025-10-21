package store.piku.back.diary.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@RequiredArgsConstructor
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private final String type;
    private final String endpoint;
    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final String bucket;
    private final String publicUrl;
}