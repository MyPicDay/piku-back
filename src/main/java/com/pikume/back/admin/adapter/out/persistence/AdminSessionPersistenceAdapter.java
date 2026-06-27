package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.application.port.out.CountAdminSessionsPort;
import com.pikume.back.admin.application.port.out.LoadAdminSessionPort;
import com.pikume.back.admin.application.port.out.SaveAdminSessionPort;
import com.pikume.back.admin.application.port.out.TouchAdminSessionPort;
import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionPhase;
import com.pikume.back.admin.domain.AdminSessionStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class AdminSessionPersistenceAdapter implements
		LoadAdminSessionPort,
		SaveAdminSessionPort,
		TouchAdminSessionPort,
		CountAdminSessionsPort {

	private final AdminSessionJpaRepository adminSessionJpaRepository;
	private final MeterRegistry meterRegistry;

	@Override
	public Optional<AdminSession> findById(String sessionId) {
		return recordLookup("session_id", () -> adminSessionJpaRepository.findById(sessionId));
	}

	@Override
	public Optional<AdminSession> findBySessionTokenHash(String sessionTokenHash) {
		return recordLookup(
				"token_hash", () -> adminSessionJpaRepository.findBySessionTokenHash(sessionTokenHash));
	}

	@Override
	public Optional<AdminSession> findBySessionTokenHashForUpdate(String sessionTokenHash) {
		return recordLookup(
				"token_hash_for_update",
				() -> adminSessionJpaRepository.findBySessionTokenHashForUpdate(sessionTokenHash));
	}

	@Override
	public List<AdminSession> findActiveByAdminId(String adminId) {
		return recordLookup(
				"active_by_admin",
				() -> adminSessionJpaRepository.findByAdminIdAndStatus(adminId, AdminSessionStatus.ACTIVE));
	}

	@Override
	public AdminSession save(AdminSession adminSession) {
		try {
			return adminSessionJpaRepository.saveAndFlush(adminSession);
		} catch (DataAccessException exception) {
			throw new AdminAuthenticationStoreException(
					"관리자 세션 저장소를 사용할 수 없습니다.", exception);
		}
	}

	@Override
	@Transactional
	public boolean touchAuthenticated(String sessionTokenHash, long authenticationVersion,
			LocalDateTime now, LocalDateTime newIdleExpiresAt) {
		Optional<AdminSession> session = recordLookup(
				"touch", () -> adminSessionJpaRepository.findBySessionTokenHash(sessionTokenHash));
		if (session.isEmpty()
				|| !session.get().isAuthenticated()
				|| !session.get().hasAuthenticationVersion(authenticationVersion)
				|| !session.get().isActiveAt(now)) {
			return false;
		}
		session.get().touch(now, newIdleExpiresAt);
		return true;
	}

	@Override
	public long countActiveAuthenticatedAt(LocalDateTime now) {
		return recordLookup(
				"count_active",
				() -> adminSessionJpaRepository
						.countByStatusAndPhaseAndAbsoluteExpiresAtAfterAndIdleExpiresAtAfter(
								AdminSessionStatus.ACTIVE,
								AdminSessionPhase.AUTHENTICATED,
								now,
								now));
	}

	private <T> T recordLookup(String operation, Supplier<T> lookup) {
		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			return lookup.get();
		} catch (DataAccessException exception) {
			throw new AdminAuthenticationStoreException(
					"관리자 세션 저장소를 사용할 수 없습니다.", exception);
		} finally {
			sample.stop(Timer.builder("admin.session.database.lookup")
					.tag("operation", operation)
					.register(meterRegistry));
		}
	}
}
