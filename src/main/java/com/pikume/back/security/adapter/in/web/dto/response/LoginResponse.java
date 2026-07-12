package com.pikume.back.security.adapter.in.web.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponse {
	private String message;
	private UserInfo user;
	public LoginResponse(String message, UserInfo user) { this.message = message; this.user = user; }
}
