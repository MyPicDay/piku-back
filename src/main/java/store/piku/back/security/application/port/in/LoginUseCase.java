package store.piku.back.security.application.port.in;

import org.springframework.http.ResponseCookie;
import store.piku.back.security.dto.TokenDto;
import store.piku.back.security.dto.UserInfo;
import store.piku.back.security.dto.request.LoginRequest;

public interface LoginUseCase {

	TokenDto login(LoginRequest dto, String deviceId);

	UserInfo getUserInfoByEmail(String email);

	ResponseCookie newCookieRefreshToken(String refreshToken);

	ResponseCookie removeCookieRefreshToken();

	void logout(String email, String deviceId);
}
