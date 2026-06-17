package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.application.port.out.LoadAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.LoadAdminSessionPort;
import com.pikume.back.admin.application.port.out.SaveAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.SaveAdminSessionPort;
import com.pikume.back.admin.domain.AdminRefreshToken;
import com.pikume.back.admin.domain.AdminRefreshTokenStatus;
import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminSessionPersistenceAdapter implements
		LoadAdminSessionPort,
		SaveAdminSessionPort,
		LoadAdminRefreshTokenPort,
		SaveAdminRefreshTokenPort {

	private final AdminSessionJpaRepository adminSessionJpaRepository;
	private final AdminRefreshTokenJpaRepository adminRefreshTokenJpaRepository;

	@Override
	public Optional<AdminSession> findById(String sessionId) {
		return adminSessionJpaRepository.findById(sessionId);
	}

	@Override
	public List<AdminSession> findActiveByAdminId(String adminId) {
		return adminSessionJpaRepository.findByAdminIdAndStatus(adminId, AdminSessionStatus.ACTIVE);
	}

	@Override
	public AdminSession save(AdminSession adminSession) {
		return adminSessionJpaRepository.save(adminSession);
	}

	@Override
	public Optional<AdminRefreshToken> findByTokenHash(String tokenHash) {
		return adminRefreshTokenJpaRepository.findByTokenHash(tokenHash);
	}

	@Override
	public List<AdminRefreshToken> findActiveBySessionId(String sessionId) {
		return adminRefreshTokenJpaRepository.findBySessionIdAndStatus(sessionId, AdminRefreshTokenStatus.ACTIVE);
	}

	@Override
	public AdminRefreshToken save(AdminRefreshToken adminRefreshToken) {
		return adminRefreshTokenJpaRepository.save(adminRefreshToken);
	}
}
