package com.pikume.back.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.user.application.dto.ProfilePreviewResult;
import com.pikume.back.user.application.dto.UserProfileResult;
import com.pikume.back.user.application.exception.UserNotFoundException;
import com.pikume.back.user.application.port.in.GetUserProfileUseCase;
import com.pikume.back.user.application.port.out.LoadUserAccountPort;
import com.pikume.back.user.application.port.out.QueryProfileDiaryMetricsPort;
import com.pikume.back.user.application.port.out.QueryProfileSocialMetricsPort;
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

	private final LoadUserAccountPort loadUserAccountPort;
	private final QueryProfileSocialMetricsPort socialMetricsPort;
	private final QueryProfileDiaryMetricsPort diaryMetricsPort;

	@Override
	public ProfilePreviewResult getProfilePreview(String profileId, String currentUserId) {
		User profile = loadUserAccountPort.findById(profileId)
					.orElseThrow(UserNotFoundException::new);

		int friendCount = socialMetricsPort.countFriends(profileId);
		long diaryCount = diaryMetricsPort.countVisibleDiaries(profileId, currentUserId);
		String friendshipStatus = socialMetricsPort.getFriendshipStatus(currentUserId, profileId);

		log.info("event=profile_preview_loaded outcome=success userId={} friendCount={} diaryCount={} friendStatus={}",
				profileId, friendCount, diaryCount, friendshipStatus);

		return new ProfilePreviewResult(profileId, profile.getNickname(), profile.getAvatar(), friendCount, diaryCount,
				friendshipStatus);
	}

	@Override
	public UserProfileResult getUserProfile(String profileId, String currentUserId) {
		ProfilePreviewResult preview = getProfilePreview(profileId, currentUserId);
		boolean isOwner = profileId.equals(currentUserId);
		List<UserProfileResult.MonthlyDiaryCount> monthlyDiaryCount = diaryMetricsPort
				.getVisibleMonthlyDiaryCounts(profileId, currentUserId)
				.stream()
				.map(count -> new UserProfileResult.MonthlyDiaryCount(count.year(), count.month(), count.count()))
				.toList();

		return new UserProfileResult(
				preview.id(),
				preview.nickname(),
				preview.avatarObjectKey(),
				preview.friendCount(),
				preview.diaryCount(),
				preview.friendStatus(),
				isOwner,
				monthlyDiaryCount);
	}
}
