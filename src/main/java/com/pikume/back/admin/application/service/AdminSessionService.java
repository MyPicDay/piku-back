package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminSessionSecurityUseCase;
import com.pikume.back.admin.application.port.out.AdminSessionCacheEntry;
import com.pikume.back.admin.application.port.out.AdminSessionCachePort;
import com.pikume.back.admin.application.port.out.AdminSessionCredentialPort;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.application.port.out.AdminSessionLifecyclePort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.LoadAdminSessionPort;
import com.pikume.back.admin.application.port.out.SaveAdminSessionPort;
import com.pikume.back.admin.application.port.out.TouchAdminSessionPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionPhase;
import com.pikume.back.admin.domain.exception.AdminDomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminSessionService implements AdminSessionSecurityUseCase, AdminSessionLifecyclePort {

	private static final long PRE_AUTHENTICATION_MINUTES = 10;
	private static final long IDLE_TIMEOUT_MINUTES = 30;
	private static final long ABSOLUTE_TIMEOUT_HOURS = 8;

	private final LoadAdminSessionPort loadAdminSessionPort;
	private final SaveAdminSessionPort saveAdminSessionPort;
	private final TouchAdminSessionPort touchAdminSessionPort;
	private final AdminSessionCachePort adminSessionCachePort;
	private final AdminSessionCredentialPort adminSessionCredentialPort;
	private final LoadAdminAccountPort loadAdminAccountPort;
	private final AdminSessionTelemetryPort telemetryPort;

	@Transactional
	@Override
	public AdminSessionCredentials initialize(LocalDateTime now) {
		String sessionToken = adminSessionCredentialPort.generate();
		String csrfToken = adminSessionCredentialPort.generate();
		AdminSession session = AdminSession.startAnonymous(
				adminSessionCredentialPort.hash(sessionToken),
				adminSessionCredentialPort.hash(csrfToken),
				now,
				now.plusMinutes(PRE_AUTHENTICATION_MINUTES));
		saveAdminSessionPort.save(session);
		telemetryPort.sessionIssued();
		return new AdminSessionCredentials(sessionToken, csrfToken);
	}

	@Transactional
	@Override
	public AuthenticatedAdminSession authenticate(String rawSessionToken, LocalDateTime now) {
		String sessionTokenHash = adminSessionCredentialPort.hash(rawSessionToken);
		AdminSessionCacheEntry entry = loadSessionEntry(sessionTokenHash, now);
		AdminAccount admin = loadCurrentAdmin(entry.adminId());

		if (admin.getStatus() != AdminAccountStatus.ACTIVE
				|| entry.authenticationVersion() != admin.getAuthenticationVersion()) {
			evictQuietly(sessionTokenHash);
			throw unauthenticated();
		}

		LocalDateTime newIdleExpiresAt = now.plusMinutes(IDLE_TIMEOUT_MINUTES);
		boolean touched;
		try {
			touched = touchAdminSessionPort.touchAuthenticated(
					sessionTokenHash,
					admin.getAuthenticationVersion(),
					now,
					newIdleExpiresAt);
		} catch (RuntimeException exception) {
			throw sessionStoreUnavailable(exception);
		}
		if (!touched) {
			evictQuietly(sessionTokenHash);
			throw unauthenticated();
		}

		LocalDateTime effectiveIdleExpiresAt = newIdleExpiresAt.isAfter(entry.absoluteExpiresAt())
				? entry.absoluteExpiresAt()
				: newIdleExpiresAt;
		AdminSessionCacheEntry refreshed = new AdminSessionCacheEntry(
				entry.sessionId(), entry.adminId(), entry.phase(),
				entry.sessionTokenHash(), entry.csrfTokenHash(), entry.authenticationVersion(),
				entry.absoluteExpiresAt(), effectiveIdleExpiresAt);
		putQuietly(refreshed);

		return new AuthenticatedAdminSession(
				entry.sessionId(), entry.adminId(), admin.getRole());
	}

	@Transactional(readOnly = true)
	@Override
	public void validateCsrf(String rawSessionToken, String rawCsrfToken, LocalDateTime now) {
		String sessionTokenHash = adminSessionCredentialPort.hash(rawSessionToken);
		AdminSession session;
		try {
			session = loadAdminSessionPort.findBySessionTokenHash(sessionTokenHash)
					.orElseThrow(this::unauthenticated);
		} catch (AdminException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw sessionStoreUnavailable(exception);
		}
		if (!session.isActiveAt(now)) {
			throw unauthenticated();
		}
		if (!adminSessionCredentialPort.matches(rawCsrfToken, session.getCsrfTokenHash())) {
			throw new AdminException(AdminProblem.CSRF_INVALID, "관리자 CSRF 토큰이 유효하지 않습니다.");
		}
	}

	@Transactional
	@Override
	public void bindPreAuthentication(String rawSessionToken, String adminId, long authenticationVersion,
			AdminSessionPhase nextPhase, LocalDateTime now) {
		AdminSession session = loadSession(rawSessionToken);
		try {
			if (!session.isActiveAt(now)) {
				throw unauthenticated();
			}
			session.bindAdmin(adminId, authenticationVersion, nextPhase,
					now.plusMinutes(PRE_AUTHENTICATION_MINUTES), now);
			saveAdminSessionPort.save(session);
		} catch (AdminException exception) {
			throw exception;
		} catch (AdminDomainException exception) {
			throw unauthenticated();
		}
	}

	@Transactional(readOnly = true)
	@Override
	public String requirePhase(String rawSessionToken, AdminSessionPhase expectedPhase, LocalDateTime now) {
		AdminSession session = loadSession(rawSessionToken);
		if (!session.isActiveAt(now) || session.getPhase() != expectedPhase || session.getAdminId() == null) {
			throw unauthenticated();
		}
		AdminAccount admin = loadCurrentAdmin(session.getAdminId());
		if (admin.getStatus() != AdminAccountStatus.ACTIVE
				|| !session.hasAuthenticationVersion(admin.getAuthenticationVersion())) {
			throw unauthenticated();
		}
		return session.getAdminId();
	}

	@Transactional
	@Override
	public String advancePhase(String rawSessionToken, AdminSessionPhase expectedPhase,
			AdminSessionPhase nextPhase, long currentAuthenticationVersion, LocalDateTime now) {
		AdminSession session = loadSession(rawSessionToken);
		if (!session.isActiveAt(now) || session.getAdminId() == null) {
			throw unauthenticated();
		}
		try {
			session.advance(expectedPhase, nextPhase, currentAuthenticationVersion, now);
			saveAdminSessionPort.save(session);
			return session.getAdminId();
		} catch (AdminDomainException exception) {
			throw unauthenticated();
		}
	}

	@Transactional
	@Override
	public AdminSessionCredentials completeAuthentication(String rawSessionToken, AdminAccount admin,
			AdminSessionPhase expectedPhase, LocalDateTime now) {
		String oldTokenHash = adminSessionCredentialPort.hash(rawSessionToken);
		AdminSession session = loadSessionByHash(oldTokenHash);
		if (!session.isActiveAt(now)
				|| session.getPhase() != expectedPhase
				|| !session.belongsTo(admin.getId())) {
			throw unauthenticated();
		}

		loadAdminSessionPort.findActiveByAdminId(admin.getId()).stream()
				.filter(active -> !active.getId().equals(session.getId()))
				.filter(AdminSession::isAuthenticated)
				.forEach(active -> {
					active.revoke(now);
					saveAdminSessionPort.save(active);
					evictQuietly(active.getSessionTokenHash());
					telemetryPort.sessionRevoked();
				});

		String newSessionToken = adminSessionCredentialPort.generate();
		String newCsrfToken = adminSessionCredentialPort.generate();
		session.authenticate(
				adminSessionCredentialPort.hash(newSessionToken),
				adminSessionCredentialPort.hash(newCsrfToken),
				admin.getAuthenticationVersion(),
				now.plusHours(ABSOLUTE_TIMEOUT_HOURS),
				now.plusMinutes(IDLE_TIMEOUT_MINUTES),
				now);
		saveAdminSessionPort.save(session);
		evictQuietly(oldTokenHash);
		telemetryPort.sessionIssued();
		return new AdminSessionCredentials(newSessionToken, newCsrfToken);
	}

	@Transactional
	@Override
	public void revokeActiveSessions(String adminId, LocalDateTime now) {
		loadAdminSessionPort.findActiveByAdminId(adminId).forEach(session -> {
			session.revoke(now);
			saveAdminSessionPort.save(session);
			evictQuietly(session.getSessionTokenHash());
			telemetryPort.sessionRevoked();
		});
	}

	@Transactional
	@Override
	public void revokeCurrent(String adminId, String sessionId, LocalDateTime now) {
		loadAdminSessionPort.findById(sessionId)
				.filter(session -> session.belongsTo(adminId))
				.ifPresent(session -> {
					session.revoke(now);
					saveAdminSessionPort.save(session);
					evictQuietly(session.getSessionTokenHash());
					telemetryPort.sessionRevoked();
				});
	}

	private AdminSession loadSession(String rawSessionToken) {
		return loadSessionByHash(adminSessionCredentialPort.hash(rawSessionToken));
	}

	private AdminSession loadSessionByHash(String tokenHash) {
		try {
			return loadAdminSessionPort.findBySessionTokenHash(tokenHash).orElseThrow(this::unauthenticated);
		} catch (AdminException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw sessionStoreUnavailable(exception);
		}
	}

	private AdminSessionCacheEntry loadSessionEntry(String sessionTokenHash, LocalDateTime now) {
		AdminSessionCacheEntry cached = findCachedQuietly(sessionTokenHash);
		if (cached != null) {
			telemetryPort.cacheHit();
			if (!cached.isAuthenticatedAndActiveAt(now)) {
				evictQuietly(sessionTokenHash);
				throw unauthenticated();
			}
			return cached;
		}

		telemetryPort.databaseFallback();
		AdminSession session;
		try {
			session = loadAdminSessionPort.findBySessionTokenHash(sessionTokenHash)
					.orElseThrow(this::unauthenticated);
		} catch (AdminException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw sessionStoreUnavailable(exception);
		}

		if (!session.isAuthenticated() || !session.isActiveAt(now)) {
			throw unauthenticated();
		}
		return AdminSessionCacheEntry.from(session);
	}

	private AdminAccount loadCurrentAdmin(String adminId) {
		try {
			return loadAdminAccountPort.findById(adminId).orElseThrow(this::unauthenticated);
		} catch (AdminException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw sessionStoreUnavailable(exception);
		}
	}

	private AdminSessionCacheEntry findCachedQuietly(String tokenHash) {
		try {
			return adminSessionCachePort.findByTokenHash(tokenHash).orElse(null);
		} catch (RuntimeException ignored) {
			telemetryPort.cacheOperationFailed("read");
			return null;
		}
	}

	private void putQuietly(AdminSessionCacheEntry entry) {
		try {
			adminSessionCachePort.put(entry);
		} catch (RuntimeException ignored) {
			telemetryPort.cacheOperationFailed("write");
			// Redis는 보조 저장소이므로 DB 기반 인증 결과를 되돌리지 않는다.
		}
	}

	private void evictQuietly(String tokenHash) {
		try {
			adminSessionCachePort.evict(tokenHash);
		} catch (RuntimeException ignored) {
			telemetryPort.cacheOperationFailed("evict");
			// 현재 계정 상태와 인증 버전 검증이 최종 접근을 차단한다.
		}
	}

	private AdminException unauthenticated() {
		return new AdminException(AdminProblem.UNAUTHENTICATED, "유효한 관리자 세션이 필요합니다.");
	}

	private AdminException sessionStoreUnavailable(RuntimeException cause) {
		telemetryPort.sessionStoreUnavailable();
		AdminException exception = new AdminException(
				AdminProblem.SESSION_STORE_UNAVAILABLE,
				"관리자 세션 저장소를 확인할 수 없습니다.");
		exception.initCause(cause);
		return exception;
	}
}
