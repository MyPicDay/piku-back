package com.pikume.back.user.domain.service;

import com.pikume.back.user.domain.exception.InvalidPasswordException;

import java.util.regex.Pattern;

public class PasswordPolicy {

	private static final Pattern PASSWORD_PATTERN =
			Pattern.compile("^(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]+$");

	public void validate(String rawPassword) {
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new InvalidPasswordException("비밀번호는 필수 값입니다.");
		}
		if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
			throw new InvalidPasswordException("올바르지 않은 비밀번호 형식입니다.");
		}
	}
}
