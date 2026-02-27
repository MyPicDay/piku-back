package com.pikume.back.user.domain.vo;

/**
 * 아바타 Value Object
 * 아바타 이미지 경로를 관리합니다.
 */
public record Avatar(String path) {

	public boolean isEmpty() {
		return path == null || path.isBlank();
	}

	public boolean isSameAs(Avatar other) {
		if (other == null || other.isEmpty()) {
			return this.isEmpty();
		}
		return this.path != null && this.path.equals(other.path);
	}
}
