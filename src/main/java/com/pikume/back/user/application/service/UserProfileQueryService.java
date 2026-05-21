package com.pikume.back.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.user.application.dto.ProfilePreviewResult;
import com.pikume.back.user.application.dto.UserProfileResult;
import com.pikume.back.user.application.exception.UserNotFoundException;
import com.pikume.back.user.application.port.in.GetUserProfileUseCase;
import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.application.port.out.UserDiaryPort;
import com.pikume.back.user.application.port.out.UserFriendPort;
import com.pikume.back.user.domain.User;

import java.util.List;

/**
 * 프로필 조회 Application Service
 * GetUserProfileUseCase를 구현하여 프로필 조회 유스케이스를 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserProfileQueryService implements GetUserProfileUseCase {

	private final LoadUserPort loadUserPort;
	private final UserFriendPort friendPort;
	private final UserDiaryPort diaryPort;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public ProfilePreviewResult getProfilePreview(String profileId, String currentUserId,
			RequestMetaInfo requestMetaInfo) {
			User profile = loadUserPort.findById(profileId)
					.orElseThrow(UserNotFoundException::new);

		String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(profile.getAvatar(), requestMetaInfo);
		int friendCount = friendPort.countFriends(profileId);
		long diaryCount = diaryPort.countDiariesByUserId(profileId, currentUserId);
		String friendshipStatus = friendPort.getFriendshipStatus(currentUserId, profileId);

		log.info("사용자 ID {}에 대한 프로필 미리보기 조회 완료. 친구 수: {}, 일기 수: {}, 친구 상태: {}",
				profileId, friendCount, diaryCount, friendshipStatus);

		return new ProfilePreviewResult(profileId, profile.getNickname(), avatarUrl, friendCount, diaryCount,
				friendshipStatus);
	}

	@Override
	public UserProfileResult getUserProfile(String profileId, String currentUserId, RequestMetaInfo requestMetaInfo) {
		ProfilePreviewResult preview = getProfilePreview(profileId, currentUserId, requestMetaInfo);
		boolean isOwner = profileId.equals(currentUserId);
		List<UserDiaryPort.MonthlyDiaryCount> monthlyDiaryCount = diaryPort.getMonthlyDiaryCount(profileId, currentUserId);

		return new UserProfileResult(
				preview.id(),
				preview.nickname(),
				preview.avatar(),
				preview.friendCount(),
				preview.diaryCount(),
				preview.friendStatus(),
				isOwner,
				monthlyDiaryCount);
	}
}
