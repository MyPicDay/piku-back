package com.pikume.back.social.application.service;

import com.pikume.back.social.application.event.SocialNotificationEvent;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.port.out.LoadFriendshipsPort;
import com.pikume.back.social.application.port.out.LoadPendingFriendRequestsPort;
import com.pikume.back.social.application.port.out.PublishSocialNotificationEventPort;
import com.pikume.back.social.application.port.out.RecordFriendRequestPort;
import com.pikume.back.social.application.port.out.RecordFriendshipPort;
import com.pikume.back.social.application.port.out.VerifySocialParticipantPort;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendCommandServiceTest {

	@InjectMocks
	private FriendCommandService service;
	@Mock private LoadFriendshipsPort loadFriendshipsPort;
	@Mock private RecordFriendshipPort recordFriendshipPort;
	@Mock private LoadPendingFriendRequestsPort loadPendingFriendRequestsPort;
	@Mock private RecordFriendRequestPort recordFriendRequestPort;
	@Mock private VerifySocialParticipantPort verifySocialParticipantPort;
	@Mock private PublishSocialNotificationEventPort eventPort;

	@Test
	void recordsAndPublishesNewRequest() {
		given(verifySocialParticipantPort.participantExists("from")).willReturn(true);
		given(verifySocialParticipantPort.participantExists("to")).willReturn(true);
		given(loadPendingFriendRequestsPort.loadPendingRequest(new FriendRequestID("to", "from")))
				.willReturn(Optional.empty());
		given(recordFriendRequestPort.tryRecordPendingRequest(any())).willReturn(true);

		var result = service.sendFriendRequest("from", "to");

		assertThat(result.accepted()).isFalse();
		verify(eventPort).publish(new SocialNotificationEvent.FriendRequest("to", "from"));
	}

	@Test
	void acceptsReversePendingRequest() {
		FriendRequest reverse = new FriendRequest("to", "from");
		given(verifySocialParticipantPort.participantExists("from")).willReturn(true);
		given(verifySocialParticipantPort.participantExists("to")).willReturn(true);
		given(loadPendingFriendRequestsPort.loadPendingRequest(new FriendRequestID("to", "from")))
				.willReturn(Optional.of(reverse));

		var result = service.sendFriendRequest("from", "to");

		assertThat(result.accepted()).isTrue();
		verify(recordFriendRequestPort).closePendingRequest(reverse);
		verify(recordFriendshipPort).establishFriendship(any());
		verify(eventPort).publish(new SocialNotificationEvent.FriendAccepted("to", "from"));
	}

	@Test
	void rejectsDuplicatePendingRequestWithoutPublishingAgain() {
		given(verifySocialParticipantPort.participantExists("from")).willReturn(true);
		given(verifySocialParticipantPort.participantExists("to")).willReturn(true);
		given(loadPendingFriendRequestsPort.loadPendingRequest(any())).willReturn(Optional.empty());
		given(recordFriendRequestPort.tryRecordPendingRequest(any())).willReturn(false);

		assertThatThrownBy(() -> service.sendFriendRequest("from", "to"))
				.isInstanceOfSatisfying(SocialException.class, exception -> {
					assertThat(exception.getErrorCode()).isEqualTo(SocialErrorCode.DUPLICATE_FRIEND_REQUEST);
					assertThat(exception).hasMessage("이미 친구 요청을 보냈습니다.");
				});

		verify(eventPort, never()).publish(any());
	}

	@Test
	void rejectsSelfRequestWithApplicationError() {
		given(verifySocialParticipantPort.participantExists("same")).willReturn(true);

		assertThatThrownBy(() -> service.sendFriendRequest("same", "same"))
				.isInstanceOfSatisfying(SocialException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(SocialErrorCode.SELF_FRIEND_REQUEST));
	}
}
