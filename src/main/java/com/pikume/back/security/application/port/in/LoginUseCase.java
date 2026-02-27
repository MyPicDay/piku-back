package com.pikume.back.security.application.port.in;

import org.springframework.http.ResponseCookie;
import com.pikume.back.security.dto.TokenDto;
import com.pikume.back.security.dto.UserInfo;
import com.pikume.back.security.dto.request.LoginRequest;

public interface LoginUseCase {

	TokenDto login(LoginRequest dto, String deviceId);

	UserInfo getUserInfoByEmail(String email);

	ResponseCookie newCookieRefreshToken(String refreshToken);

	ResponseCookie removeCookieRefreshToken();

	void logout(String email, String deviceId);
}
