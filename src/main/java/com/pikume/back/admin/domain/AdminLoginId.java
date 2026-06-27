package com.pikume.back.admin.domain;

import java.util.regex.Pattern;

import com.pikume.back.admin.domain.exception.AdminDomainException;

public final class AdminLoginId {

	private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z0-9_-]{4,15}$");

	private AdminLoginId() {
	}

	public static String normalize(String value) {
		if (value == null || value.isBlank()) {
			throw new AdminDomainException("관리자 로그인 아이디는 필수입니다.");
		}

		String normalized = value.trim();
		if (!LOGIN_ID_PATTERN.matcher(normalized).matches()) {
			throw new AdminDomainException("관리자 로그인 아이디 형식이 올바르지 않습니다.");
		}
		return normalized;
	}
}
