package store.piku.back.security.application.port.in;

import org.springframework.http.ResponseCookie;
import store.piku.back.auth.dto.TokenDto;
import store.piku.back.auth.dto.UserInfo;
import store.piku.back.auth.dto.request.LoginRequest;

public interface LoginUseCase {

	TokenDto login(LoginRequest dto, String deviceId);

	UserInfo getUserInfoByEmail(String email);

	ResponseCookie newCookieRefreshToken(String refreshToken);

	ResponseCookie removeCookieRefreshToken();

	void logout(String email, String deviceId);
}
