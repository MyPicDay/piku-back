package store.piku.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    /**
     * WebClient 빌더 빈 등록 (이미지 데이터 처리를 위한 버퍼 크기 확장)
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        // 이미지 데이터 처리를 위해 메모리 버퍼 크기를 10MB로 확장
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(500 * 1024 * 1024)) // 500MB
                .build();

        return WebClient.builder()
                .exchangeStrategies(strategies);
    }
}
