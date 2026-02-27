package com.pikume.back.feed.adapter.out.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.out.LoadUserForDiaryPort;
import com.pikume.back.feed.application.port.out.LoadUserForFeedPort;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.util.ImagePathToUrlConverter;

@Component
@RequiredArgsConstructor
public class UserAdapterForFeed implements LoadUserForFeedPort {

	private final LoadUserForDiaryPort loadUserForDiaryPort;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public String getUserAvatar(String userId) {
		return loadUserForDiaryPort.getUserAvatar(userId);
	}

	@Override
	public String getUserNickname(String userId) {
		return loadUserForDiaryPort.getUserNickname(userId);
	}

	@Override
	public String getUserAvatarUrl(String avatar, RequestMetaInfo requestMetaInfo) {
		return imagePathToUrlConverter.userAvatarImageUrl(avatar, requestMetaInfo);
	}
}
