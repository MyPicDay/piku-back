package com.pikume.back.social.application.service;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.dto.FriendshipStatusResult;
import com.pikume.back.social.application.port.out.LoadFriendshipsPort;
import com.pikume.back.social.application.port.out.LoadPendingFriendRequestsPort;
import com.pikume.back.social.application.port.out.LoadSocialParticipantProfilesPort;
import com.pikume.back.social.application.readmodel.FriendReferenceView;
import com.pikume.back.social.application.readmodel.SocialParticipantProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FriendQueryServiceTest {

	@InjectMocks private FriendQueryService service;
	@Mock private LoadFriendshipsPort loadFriendshipsPort;
	@Mock private LoadPendingFriendRequestsPort loadPendingFriendRequestsPort;
	@Mock private LoadSocialParticipantProfilesPort loadSocialParticipantProfilesPort;

	@Test
	void enrichesFriendPageWithTranslatedProfile() {
		PageQuery query = new PageQuery(0, 10, List.of());
		given(loadFriendshipsPort.loadFriendPage("me", query))
				.willReturn(new PageResult<>(List.of(new FriendReferenceView("friend")), 0, 10, 1));
		given(loadSocialParticipantProfilesPort.loadProfiles(Set.of("friend")))
				.willReturn(Map.of("friend", new SocialParticipantProfile("friend", "친구", "https://avatar")));

		var result = service.queryFriendPage(query, "me").content().get(0);

		assertThat(result.nickname()).isEqualTo("친구");
		assertThat(result.avatar()).isEqualTo("https://avatar");
	}

	@Test
	void classifiesFriendshipFactsWithoutDomainEnum() {
		given(loadFriendshipsPort.loadFriendIdsWithin("me", Set.of("friend", "requested")))
				.willReturn(List.of("friend"));
		given(loadPendingFriendRequestsPort.loadRequestedTargetIds("me", Set.of("requested")))
				.willReturn(List.of("requested"));

		Map<String, FriendshipStatusResult> result = service.queryFriendshipStatuses(
				"me", Set.of("friend", "requested"));

		assertThat(result).containsEntry("friend", FriendshipStatusResult.FRIENDS)
				.containsEntry("requested", FriendshipStatusResult.REQUESTED);
	}
}
