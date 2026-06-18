package com.pikume.back.admin.adapter.out.observability;

import com.pikume.back.admin.application.port.out.CountAdminSessionsPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminSessionMetricsBinder {

	private final CountAdminSessionsPort countAdminSessionsPort;

	public AdminSessionMetricsBinder(
			MeterRegistry meterRegistry,
			CountAdminSessionsPort countAdminSessionsPort) {
		this.countAdminSessionsPort = countAdminSessionsPort;
		Gauge.builder("admin.session.active", this, AdminSessionMetricsBinder::activeAuthenticatedSessions)
				.register(meterRegistry);
	}

	private double activeAuthenticatedSessions() {
		try {
			return countAdminSessionsPort.countActiveAuthenticatedAt(LocalDateTime.now());
		} catch (RuntimeException exception) {
			return Double.NaN;
		}
	}
}
