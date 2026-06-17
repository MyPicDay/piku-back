package com.pikume.back.admin.domain;

import java.util.regex.Pattern;

import com.pikume.back.admin.domain.exception.AdminDomainException;

public final class AdminEmail {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private AdminEmail() {
	}

	public static String normalize(String value) {
		if (value == null || value.isBlank()) {
			throw new AdminDomainException("관리자 이메일은 필수입니다.");
		}

		String normalized = value.trim().toLowerCase();
		if (!EMAIL_PATTERN.matcher(normalized).matches()) {
			throw new AdminDomainException("올바른 관리자 이메일 형식이어야 합니다.");
		}
		return normalized;
	}
}
