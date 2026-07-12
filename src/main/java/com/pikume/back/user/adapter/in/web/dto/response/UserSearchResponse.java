package com.pikume.back.user.adapter.in.web.dto.response;

import com.pikume.back.user.application.dto.UserSearchResult;
import com.pikume.back.global.util.ImagePathToUrlConverter;

public record UserSearchResponse(
		String userId,
		String nickname,
		String avatar
) {

	public static UserSearchResponse from(UserSearchResult result, ImagePathToUrlConverter imageConverter) {
		return new UserSearchResponse(
				result.id(),
				result.nickname(),
				imageConverter.userAvatarImageUrl(result.avatarObjectKey()));
	}
}
