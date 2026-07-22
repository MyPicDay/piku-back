package com.pikume.back.social.adapter.out.crosscontext;

import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.social.application.port.out.LoadSocialParticipantProfilesPort;
import com.pikume.back.social.application.port.out.VerifySocialParticipantPort;
import com.pikume.back.social.application.readmodel.SocialParticipantProfile;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserReferenceUseCase;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserAdapterForSocial implements VerifySocialParticipantPort, LoadSocialParticipantProfilesPort {

	private final QueryUserReferenceUseCase queryUserReferenceUseCase;
	private final QueryUserSummaryUseCase queryUserSummaryUseCase;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public boolean participantExists(String userId) {
		return queryUserReferenceUseCase.queryUserReference(userId).isPresent();
	}

	@Override
	public Map<String, SocialParticipantProfile> loadProfiles(Set<String> userIds) {
		return queryUserSummaryUseCase.queryUserSummaries(userIds).values().stream()
				.collect(Collectors.toMap(
						UserSummaryView::id,
						user -> new SocialParticipantProfile(
								user.id(),
								user.nickname(),
								user.avatarPath() != null
										? imagePathToUrlConverter.userAvatarImageUrl(user.avatarPath())
										: null)));
	}
}
