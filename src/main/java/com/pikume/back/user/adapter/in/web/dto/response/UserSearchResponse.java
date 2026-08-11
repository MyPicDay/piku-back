package com.pikume.back.user.adapter.in.web.dto.response;

import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.dto.UserSearchResult;

public record UserSearchResponse(
		String userId,
		String nickname,
		String avatar
) {

	public static UserSearchResponse from(UserSearchResult result, ResolveObjectUrlPort objectUrlPort) {
		return new UserSearchResponse(
				result.id(),
				result.nickname(),
				resolveAvatarUrl(result.avatarReference(), objectUrlPort));
	}

	private static String resolveAvatarUrl(
			UserAvatarReference reference,
			ResolveObjectUrlPort objectUrlPort) {
		if (reference == null) {
			return null;
		}
		if (reference.absoluteUrl()) {
			return reference.value();
		}
		return objectUrlPort.resolveObjectUrl(reference.value(), reference.publiclyAccessible());
	}
}
