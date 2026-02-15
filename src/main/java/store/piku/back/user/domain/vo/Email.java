package store.piku.back.user.domain.vo;

import java.util.regex.Pattern;

/**
 * 이메일 Value Object
 * 이메일 형식 검증 로직을 캡슐화합니다.
 */
public record Email(String value) {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

	public Email {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("이메일은 필수 값입니다.");
		}
		if (!EMAIL_PATTERN.matcher(value).matches()) {
			throw new IllegalArgumentException("올바르지 않은 이메일 형식입니다: " + value);
		}
	}
}
