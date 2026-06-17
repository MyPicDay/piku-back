package com.pikume.back.admin.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface AdminTokenPort {

	Optional<AdminRefreshTokenClaims> readValidRefreshToken(String refreshToken);

	String generateOnboardingToken(String adminId);

	String generateOtpChallengeToken(String adminId);

	String generateAccessToken(String adminId, String role, String sessionId);

	String generateRefreshToken(String adminId, String sessionId);

	Duration onboardingTokenTtl();

	Duration otpChallengeTokenTtl();

	Duration accessTokenTtl();

	Duration refreshTokenIdleTtl();

	Duration refreshTokenAbsoluteTtl();
}
