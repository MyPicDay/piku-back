package com.pikume.back.security.application.port.in;

import com.pikume.back.global.dto.CookieSpec;
import com.pikume.back.security.application.dto.LoginResult;
import com.pikume.back.security.dto.request.LoginRequest;

public interface LoginUseCase {

	LoginResult login(LoginRequest dto, String deviceId);

	CookieSpec newCookieRefreshToken(String refreshToken);

	CookieSpec removeCookieRefreshToken();

	void logout(String email, String deviceId);

	void logoutByRefreshToken(String refreshToken);
}
