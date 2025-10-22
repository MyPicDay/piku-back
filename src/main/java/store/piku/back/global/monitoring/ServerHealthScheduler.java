package store.piku.back.global.monitoring;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerHealthScheduler {

    private static final String METRIC_STATUS = "health_check_status";
    private static final String METRIC_SUCCESS = "health_check_success_total";
    private static final String METRIC_FAILURE = "health_check_failure_total";
    private static final String METRIC_LATENCY = "health_check_latency";

    private final WebClient.Builder webClientBuilder;
    private final HealthCheckProperties properties;
    private final MeterRegistry meterRegistry;

    private final Map<String, AtomicInteger> statusGauges = new ConcurrentHashMap<>();
    private final Map<String, Timer> latencyTimers = new ConcurrentHashMap<>();

    @PostConstruct
    void initializeMeters() {
        if (CollectionUtils.isEmpty(properties.getTargets())) {
            log.warn("No health-check targets configured. Add entries under monitoring.health-check.targets.");
            return;
        }

        properties.getTargets().forEach(target -> {
            if (!StringUtils.hasText(target.getName()) || target.getUrl() == null) {
                log.warn("Skipped invalid health-check target configuration: {}", target);
                return;
            }
            statusGauges.computeIfAbsent(target.getName(), name ->
                    meterRegistry.gauge(METRIC_STATUS, Tags.of("target", name), new AtomicInteger(0)));
            latencyTimers.computeIfAbsent(target.getName(), name ->
                    Timer.builder(METRIC_LATENCY)
                            .description("Latency of health check invocations")
                            .tags("target", name)
                            .register(meterRegistry));
        });
    }

    @Scheduled(fixedDelayString = "${monitoring.health-check.interval:PT30S}")
    public void runHealthChecks() {
        if (CollectionUtils.isEmpty(properties.getTargets())) {
            return;
        }

        properties.getTargets().forEach(target -> {
            if (!StringUtils.hasText(target.getName()) || target.getUrl() == null) {
                return;
            }
            checkTarget(target);
        });
    }

    private void checkTarget(HealthCheckProperties.Target target) {
        String targetName = target.getName();
        Duration timeout = properties.getTimeout() != null ? properties.getTimeout() : Duration.ofSeconds(5);

        WebClient client = webClientBuilder.build();
        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = false;
        try {
            client.get()
                    .uri(target.getUrl())
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(timeout)
                    .onErrorResume(throwable -> Mono.error(new IllegalStateException("Health endpoint error", throwable)))
                    .block();
            success = true;
            log.debug("Health check succeeded for {} ({})", targetName, target.getUrl());
        } catch (Exception ex) {
            log.warn("Health check failed for {} ({}): {}", targetName, target.getUrl(), ex.getMessage());
        } finally {
            sample.stop(getLatencyTimer(targetName));
        }

        recordOutcome(targetName, success);
    }

    private void recordOutcome(String targetName, boolean success) {
        AtomicInteger statusGauge = statusGauges.computeIfAbsent(targetName, name ->
                meterRegistry.gauge(METRIC_STATUS, Tags.of("target", name), new AtomicInteger(0)));
        statusGauge.set(success ? 1 : 0);

        Counter counter = meterRegistry.counter(success ? METRIC_SUCCESS : METRIC_FAILURE, "target", targetName);
        counter.increment();
    }

    private Timer getLatencyTimer(String targetName) {
        return latencyTimers.computeIfAbsent(targetName, name ->
                Timer.builder(METRIC_LATENCY)
                        .description("Latency of health check invocations")
                        .tags("target", name)
                        .register(meterRegistry));
    }

}
