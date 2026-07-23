package com.pikume.back.social.adapter.out.crosscontext;

import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.social.application.port.out.LoadSocialParticipantProfilesPort;
import com.pikume.back.social.application.port.out.VerifySocialParticipantPort;
import com.pikume.back.social.application.readmodel.SocialParticipantProfile;
import com.pikume.back.user.application.dto.UserAvatarReference;
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
	private final ResolveObjectUrlPort resolveObjectUrlPort;

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
										? resolveAvatarUrl(user.avatarPath())
										: null)));
	}

	private String resolveAvatarUrl(String storedPath) {
		UserAvatarReference reference = UserAvatarReference.fromStoredPath(storedPath);
		if (reference.isEmpty() || reference.absoluteUrl()) {
			return reference.value();
		}
		return resolveObjectUrlPort.resolveObjectUrl(reference.value(), reference.publiclyAccessible());
	}
}
