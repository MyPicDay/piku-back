package com.pikume.back.user.application.dto;

/**
 * 사용자 검색 결과 DTO
 */
public record UserSearchResult(
		String id,
		String nickname,
		String avatar) {
}
