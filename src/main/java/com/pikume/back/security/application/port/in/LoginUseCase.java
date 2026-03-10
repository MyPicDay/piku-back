package com.pikume.back.security.application.port.in;

import com.pikume.back.global.dto.CookieSpec;
import com.pikume.back.security.dto.TokenDto;
import com.pikume.back.security.dto.UserInfo;
import com.pikume.back.security.dto.request.LoginRequest;

public interface LoginUseCase {

	TokenDto login(LoginRequest dto, String deviceId);

	UserInfo getUserInfoByEmail(String email);

	CookieSpec newCookieRefreshToken(String refreshToken);

	CookieSpec removeCookieRefreshToken();

	void logout(String email, String deviceId);
}
