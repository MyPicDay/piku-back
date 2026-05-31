package com.pikume.back.security.adapter.in.web;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.security.application.dto.AuthenticatedUserInfo;
import com.pikume.back.security.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserResponseMapper {

	private final ImagePathToUrlConverter imagePathToUrlConverter;

	public UserInfo toDisplayUserInfo(AuthenticatedUserInfo userInfo) {
		if (userInfo == null) {
			return null;
		}
		return new UserInfo(
				userInfo.id(),
				userInfo.email(),
				userInfo.nickname(),
				imagePathToUrlConverter.userAvatarImageUrl(userInfo.avatarPath()));
	}

	public UserInfo toDisplayUserInfo(CustomUserDetails userDetails) {
		if (userDetails == null) {
			return null;
		}
		return new UserInfo(
				userDetails.getId(),
				userDetails.getEmail(),
				userDetails.getNickname(),
				imagePathToUrlConverter.userAvatarImageUrl(userDetails.getAvatarPath()));
	}
}
