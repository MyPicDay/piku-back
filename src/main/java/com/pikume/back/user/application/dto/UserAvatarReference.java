package com.pikume.back.user.application.dto;

public record UserAvatarReference(
		String value,
		boolean absoluteUrl,
		boolean publiclyAccessible
) {

	public UserAvatarReference {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("사용자 아바타 이미지 참조는 비어 있을 수 없습니다.");
		}
	}
}
