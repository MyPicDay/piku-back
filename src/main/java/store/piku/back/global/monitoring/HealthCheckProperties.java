package store.piku.back.global.monitoring;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "monitoring.health-check")
public class HealthCheckProperties {

    /**
     * Health check execution interval. ISO-8601 duration format (e.g. PT30S).
     */
    private Duration interval = Duration.ofSeconds(30);

    /**
     * Request timeout for each health check call.
     */
    private Duration timeout = Duration.ofSeconds(5);

    /**
     * Target servers to probe. Each target must expose a health endpoint.
     */
    private List<Target> targets = new ArrayList<>();

    @Getter
    @Setter
    public static class Target {
        /** Identifier for the target server used as a metric label. */
        private String name;

        /** Absolute URL to the health endpoint. */
        private URI url;
    }
}
