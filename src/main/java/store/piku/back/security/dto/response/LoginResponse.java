package store.piku.back.security.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.security.dto.UserInfo;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponse {
	private String message;
	private UserInfo user;

	public LoginResponse(String message, UserInfo user) {
		this.message = message;
		this.user = user;
	}
}
