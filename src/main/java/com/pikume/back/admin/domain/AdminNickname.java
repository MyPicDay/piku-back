package com.pikume.back.admin.domain;

import java.util.regex.Pattern;

import com.pikume.back.admin.domain.exception.AdminDomainException;

public final class AdminNickname {

	private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9]{2,20}$");

	private AdminNickname() {
	}

	public static String normalize(String value) {
		if (value == null || value.isBlank()) {
			throw new AdminDomainException("관리자 닉네임은 필수입니다.");
		}

		String normalized = value.trim();
		if (!NICKNAME_PATTERN.matcher(normalized).matches()) {
			throw new AdminDomainException("관리자 닉네임 형식이 올바르지 않습니다.");
		}
		return normalized;
	}
}
