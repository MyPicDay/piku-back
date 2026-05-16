package com.pikume.back.user.adapter.in.web.dto.response;

import com.pikume.back.user.application.dto.UserSearchResult;

public record UserSearchResponse(
		String userId,
		String nickname,
		String avatar
) {

	public static UserSearchResponse from(UserSearchResult result) {
		return new UserSearchResponse(
				result.id(),
				result.nickname(),
				result.avatar());
	}
}
