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
				resolveAvatarUrl(result.avatarObjectKey(), objectUrlPort));
	}

	private static String resolveAvatarUrl(String storedPath, ResolveObjectUrlPort objectUrlPort) {
		UserAvatarReference reference = UserAvatarReference.fromStoredPath(storedPath);
		if (reference.isEmpty() || reference.absoluteUrl()) {
			return reference.value();
		}
		return objectUrlPort.resolveObjectUrl(reference.value(), reference.publiclyAccessible());
	}
}
