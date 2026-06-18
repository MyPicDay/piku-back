package com.pikume.back.admin.adapter.out.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MicrometerAdminSessionTelemetryAdapter")
class MicrometerAdminSessionTelemetryAdapterTest {

	@Test
	@DisplayName("관리자 세션 폴백과 보안 거부 이벤트를 메트릭으로 기록한다")
	void recordsSessionAndSecurityMetrics() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		MicrometerAdminSessionTelemetryAdapter telemetry = new MicrometerAdminSessionTelemetryAdapter(registry);

		telemetry.databaseFallback();
		telemetry.csrfRejected("token_mismatch");
		telemetry.originRejected();
		telemetry.cacheOperationFailed("read");
		telemetry.loginSucceeded("official", "admin-1");
		telemetry.loginRejected("official", "invalid_credentials");
		telemetry.otpSucceeded("official", "admin-1");
		telemetry.otpRejected("official", "blocked");

		assertThat(registry.counter("admin.session.database.fallback").count()).isEqualTo(1);
		assertThat(registry.counter("admin.security.csrf.rejected", "reason", "token_mismatch").count()).isEqualTo(1);
		assertThat(registry.counter("admin.security.origin.rejected").count()).isEqualTo(1);
		assertThat(registry.counter("admin.session.cache.failure", "operation", "read").count()).isEqualTo(1);
		assertThat(registry.counter("admin.authentication.login.succeeded", "flow", "official").count()).isEqualTo(1);
		assertThat(registry.counter(
				"admin.authentication.login.rejected", "flow", "official", "reason", "invalid_credentials").count())
				.isEqualTo(1);
		assertThat(registry.counter("admin.authentication.otp.succeeded", "flow", "official").count()).isEqualTo(1);
		assertThat(registry.counter(
				"admin.authentication.otp.rejected", "flow", "official", "reason", "blocked").count())
				.isEqualTo(1);
	}
}
