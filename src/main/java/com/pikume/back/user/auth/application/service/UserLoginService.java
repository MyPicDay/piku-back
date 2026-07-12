package com.pikume.back.user.auth.application.service;

import com.pikume.back.user.application.port.in.QueryUserIdentityUseCase;
import com.pikume.back.user.auth.application.dto.LoginCommand;
import com.pikume.back.user.auth.application.dto.LoginResult;
import com.pikume.back.user.auth.application.exception.InvalidCredentialsException;
import com.pikume.back.user.auth.application.port.in.LoginUseCase;
import com.pikume.back.user.auth.application.port.out.AuthenticationTokenPort;
import com.pikume.back.user.auth.application.port.out.PasswordProtectionPort;
import com.pikume.back.user.auth.application.port.out.RefreshSessionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLoginService implements LoginUseCase {

	private final QueryUserIdentityUseCase queryUserIdentityUseCase;
	private final PasswordProtectionPort passwordProtectionPort;
	private final AuthenticationTokenPort authenticationTokenPort;
	private final RefreshSessionPort refreshSessionPort;

	@Override
	public LoginResult login(LoginCommand command) {
		var user = queryUserIdentityUseCase.findByEmail(command.email())
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordProtectionPort.matches(command.password(), user.passwordHash())) {
			throw new InvalidCredentialsException();
		}
		String accessToken = authenticationTokenPort.generateAccessToken(user.id());
		String refreshToken = authenticationTokenPort.generateRefreshToken();
		refreshSessionPort.save(new RefreshSessionPort.RefreshSession(
				user.id() + "-" + command.deviceId(), refreshToken, user.id()));
		log.info("event=login_completed outcome=success userId={}", user.id());
		return new LoginResult(accessToken, refreshToken,
				new LoginResult.UserInfo(user.id(), user.nickname(), user.avatarPath()));
	}
}
