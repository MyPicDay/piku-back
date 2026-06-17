package com.pikume.back.admin.adapter.out.security;

import com.pikume.back.admin.application.port.out.AdminRefreshTokenClaims;
import com.pikume.back.admin.application.port.out.AdminTokenPort;
import com.pikume.back.security.jwt.AdminAuthConstants;
import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.security.jwt.SecurityTokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAdminTokenAdapter implements AdminTokenPort {

	private final JwtProvider jwtProvider;

	@Override
	public Optional<AdminRefreshTokenClaims> readValidRefreshToken(String refreshToken) {
		try {
			if (!jwtProvider.validateToken(refreshToken)
					|| jwtProvider.getTokenType(refreshToken) != SecurityTokenType.ADMIN_REFRESH) {
				return Optional.empty();
			}
			return Optional.of(new AdminRefreshTokenClaims(
					jwtProvider.getUserIdFromToken(refreshToken),
					jwtProvider.getAdminSessionIdFromToken(refreshToken)));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	@Override
	public String generateOnboardingToken(String adminId) {
		return jwtProvider.generateAdminOnboardingToken(adminId);
	}

	@Override
	public String generateOtpChallengeToken(String adminId) {
		return jwtProvider.generateAdminOtpChallengeToken(adminId);
	}

	@Override
	public String generateAccessToken(String adminId, String role, String sessionId) {
		return jwtProvider.generateAdminAccessToken(adminId, role, sessionId);
	}

	@Override
	public String generateRefreshToken(String adminId, String sessionId) {
		return jwtProvider.generateAdminRefreshToken(adminId, sessionId);
	}

	@Override
	public Duration onboardingTokenTtl() {
		return Duration.ofMillis(AdminAuthConstants.ONBOARDING_TOKEN_EXPIRATION_TIME);
	}

	@Override
	public Duration otpChallengeTokenTtl() {
		return Duration.ofMillis(AdminAuthConstants.OTP_CHALLENGE_TOKEN_EXPIRATION_TIME);
	}

	@Override
	public Duration accessTokenTtl() {
		return Duration.ofMillis(AdminAuthConstants.ACCESS_TOKEN_EXPIRATION_TIME);
	}

	@Override
	public Duration refreshTokenIdleTtl() {
		return Duration.ofMillis(AdminAuthConstants.REFRESH_TOKEN_IDLE_EXPIRATION_TIME);
	}

	@Override
	public Duration refreshTokenAbsoluteTtl() {
		return Duration.ofMillis(AdminAuthConstants.REFRESH_TOKEN_ABSOLUTE_EXPIRATION_TIME);
	}
}
