package com.pikume.back.user.domain.vo;

import com.pikume.back.user.domain.exception.InvalidEmailException;

import java.util.regex.Pattern;

/**
 * 이메일 Value Object
 * 이메일 형식 검증 로직을 캡슐화합니다.
 */
public record Email(String value) {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

	public Email {
		if (value == null || value.isBlank()) {
			throw new InvalidEmailException("이메일은 필수 값입니다.");
		}
		if (!EMAIL_PATTERN.matcher(value).matches()) {
			throw new InvalidEmailException("올바르지 않은 이메일 형식입니다.");
		}
	}
}
