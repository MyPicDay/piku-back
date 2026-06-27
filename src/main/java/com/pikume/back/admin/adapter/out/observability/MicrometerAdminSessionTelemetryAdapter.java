package com.pikume.back.admin.adapter.out.observability;

import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.domain.AdminSessionPhase;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MicrometerAdminSessionTelemetryAdapter implements AdminSessionTelemetryPort {

	private final MeterRegistry meterRegistry;

	@Override
	public void cacheHit() {
		meterRegistry.counter("admin.session.cache.hit").increment();
	}

	@Override
	public void databaseFallback() {
		meterRegistry.counter("admin.session.database.fallback").increment();
	}

	@Override
	public void sessionStoreUnavailable() {
		meterRegistry.counter("admin.session.store.unavailable").increment();
		log.error("event=admin_session_store_unavailable outcome=failed");
	}

	@Override
	public void sessionIssued() {
		meterRegistry.counter("admin.session.issued").increment();
		log.info("event=admin_session_issued outcome=success");
	}

	@Override
	public void sessionRevoked() {
		meterRegistry.counter("admin.session.revoked").increment();
		log.info("event=admin_session_revoked outcome=success");
	}

	@Override
	public void csrfRejected(String reason) {
		meterRegistry.counter("admin.security.csrf.rejected", "reason", reason).increment();
		log.warn("event=admin_csrf_rejected outcome=denied reason={}", reason);
	}

	@Override
	public void originRejected() {
		meterRegistry.counter("admin.security.origin.rejected").increment();
		log.warn("event=admin_origin_rejected outcome=denied");
	}

	@Override
	public void cacheOperationFailed(String operation) {
		meterRegistry.counter("admin.session.cache.failure", "operation", operation).increment();
		log.warn("event=admin_session_cache_failed outcome=fallback operation={}", operation);
	}

	@Override
	public void loginSucceeded(String flow, String adminId) {
		meterRegistry.counter("admin.authentication.login.succeeded", "flow", flow).increment();
		log.info("event=admin_login_succeeded outcome=success adminId={} flow={}", adminId, flow);
	}

	@Override
	public void loginRejected(String flow, String reason) {
		meterRegistry.counter(
				"admin.authentication.login.rejected", "flow", flow, "reason", reason).increment();
		log.warn("event=admin_login_failed outcome=denied flow={} reason={}", flow, reason);
	}

	@Override
	public void otpSucceeded(String flow, String adminId) {
		meterRegistry.counter("admin.authentication.otp.succeeded", "flow", flow).increment();
		log.info("event=admin_otp_succeeded outcome=success adminId={} flow={}", adminId, flow);
	}

	@Override
	public void otpRejected(String flow, String reason) {
		meterRegistry.counter(
				"admin.authentication.otp.rejected", "flow", flow, "reason", reason).increment();
		log.warn("event=admin_otp_failed outcome=denied flow={} reason={}", flow, reason);
	}

	@Override
	public void phaseChanged(String sessionId, String adminId, AdminSessionPhase fromPhase,
			AdminSessionPhase toPhase, LocalDateTime transitionedAt) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					recordPhaseChange(sessionId, adminId, fromPhase, toPhase, transitionedAt);
				}
			});
			return;
		}
		recordPhaseChange(sessionId, adminId, fromPhase, toPhase, transitionedAt);
	}

	private void recordPhaseChange(String sessionId, String adminId, AdminSessionPhase fromPhase,
			AdminSessionPhase toPhase, LocalDateTime transitionedAt) {
		meterRegistry.counter(
				"admin.session.phase.changed",
				"from", fromPhase.name(),
				"to", toPhase.name()).increment();
		log.info(
				"event=admin_session_phase_changed outcome=success sessionId={} adminId={} fromPhase={} toPhase={} transitionedAt={}",
				sessionId, adminId, fromPhase, toPhase, transitionedAt);
	}
}
