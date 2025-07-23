//package store.piku.back.global.config;
//
//import io.minio.MinioClient;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//
//@Configuration
//@Profile("dev")
//@RequiredArgsConstructor
//@ConfigurationProperties(prefix = "minio")
//@Getter
//@Setter
//public class MinioConfig {
//
//    @Value("${minio.url}")
//    private String url;
//
//    @Value("${minio.access-key}")
//    private String accessKey;
//
//    @Value("${minio.secret-key}")
//    private String secretKey;
//
//    private String bucket;
//
//    @Bean
//    public MinioClient minioClient() {
//        return MinioClient.builder()
//                .endpoint(url)
//                .credentials(accessKey, secretKey)
//                .build();
//    }
//
//}
