package store.piku.back.social.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.social.adapter.in.web.dto.FriendRemoveDTO;
import store.piku.back.social.adapter.in.web.dto.FriendRequestResponseDto;
import store.piku.back.social.application.port.out.*;
import store.piku.back.social.domain.event.SocialEvent;
import store.piku.back.social.domain.friend.FriendRequest;
import store.piku.back.social.domain.friend.exception.FriendException;
import store.piku.back.social.domain.friend.exception.FriendNotFoundException;
import store.piku.back.social.domain.friend.exception.FriendRequestNotFoundException;
import store.piku.back.social.domain.friend.vo.FriendRequestID;
import store.piku.back.social.domain.friend.vo.FriendStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

	@InjectMocks
	private FriendService friendService;

	@Mock
	private LoadFriendPort loadFriendPort;

	@Mock
	private SaveFriendPort saveFriendPort;

	@Mock
	private LoadFriendRequestPort loadFriendRequestPort;

	@Mock
	private SaveFriendRequestPort saveFriendRequestPort;

	@Mock
	private LoadUserInfoPort loadUserInfoPort;

	@Mock
	private PublishEventPort publishEventPort;

	@Mock
	private ImagePathToUrlConverter imagePathToUrlConverter;

	private final RequestMetaInfo requestMetaInfo = new RequestMetaInfo(
			"https", "localhost", 8080, "localhost:8080",
			"https://localhost:8080/api/friends", "TestAgent", "127.0.0.1");

	@Nested
	@DisplayName("areFriends - 친구 여부 확인")
	class AreFriends {

		@Test
		@DisplayName("친구인 경우 true를 반환한다")
		void returnsTrue() {
			given(loadFriendPort.existsFriendship("user-a", "user-b")).willReturn(true);

			assertThat(friendService.areFriends("user-a", "user-b")).isTrue();
		}

		@Test
		@DisplayName("친구가 아닌 경우 false를 반환한다")
		void returnsFalse() {
			given(loadFriendPort.existsFriendship("user-a", "user-c")).willReturn(false);

			assertThat(friendService.areFriends("user-a", "user-c")).isFalse();
		}
	}

	@Nested
	@DisplayName("sendFriendRequest - 친구 요청")
	class SendFriendRequest {

		@Test
		@DisplayName("새 친구 요청을 보내고 이벤트를 발행한다")
		void sendNewRequest() {
			given(loadUserInfoPort.findUserInfoById("from-user"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("from-user", "발신자", null)));
			given(loadUserInfoPort.findUserInfoById("to-user"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("to-user", "수신자", null)));
			given(loadFriendPort.existsFriendship("from-user", "to-user")).willReturn(false);
			given(loadFriendPort.existsFriendship("to-user", "from-user")).willReturn(false);
			given(loadFriendRequestPort.findById(new FriendRequestID("to-user", "from-user")))
					.willReturn(Optional.empty());

			FriendRequestResponseDto response = friendService.sendFriendRequest("from-user", "to-user", requestMetaInfo);

			assertThat(response.isAccepted()).isFalse();
			assertThat(response.getMessage()).contains("보냈습니다");
			then(saveFriendRequestPort).should().save(any(FriendRequest.class));
			then(publishEventPort).should().publish(any(SocialEvent.FriendRequestEvent.class));
		}

		@Test
		@DisplayName("상대방이 이미 보낸 요청이 있으면 자동 수락한다")
		void autoAcceptExistingRequest() {
			FriendRequest existingRequest = new FriendRequest("to-user", "from-user");
			given(loadUserInfoPort.findUserInfoById("from-user"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("from-user", "발신자", null)));
			given(loadUserInfoPort.findUserInfoById("to-user"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("to-user", "수신자", null)));
			given(loadFriendPort.existsFriendship("from-user", "to-user")).willReturn(false);
			given(loadFriendPort.existsFriendship("to-user", "from-user")).willReturn(false);
			given(loadFriendRequestPort.findById(new FriendRequestID("to-user", "from-user")))
					.willReturn(Optional.of(existingRequest));

			FriendRequestResponseDto response = friendService.sendFriendRequest("from-user", "to-user", requestMetaInfo);

			assertThat(response.isAccepted()).isTrue();
			assertThat(response.getMessage()).contains("수락");
			then(saveFriendRequestPort).should().delete(existingRequest);
			then(saveFriendPort).should().save(any());
			then(publishEventPort).should().publish(any(SocialEvent.FriendAcceptedEvent.class));
		}

		@Test
		@DisplayName("자신에게 친구 요청하면 예외 발생")
		void failsSelfRequest() {
			given(loadUserInfoPort.findUserInfoById("user-a"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-a", "유저", null)));

			assertThatThrownBy(() -> friendService.sendFriendRequest("user-a", "user-a", requestMetaInfo))
					.isInstanceOf(FriendException.class)
					.hasMessageContaining("자신에게");
		}

		@Test
		@DisplayName("이미 친구인 사용자에게 요청하면 예외 발생")
		void failsAlreadyFriends() {
			given(loadUserInfoPort.findUserInfoById("user-a"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-a", "유저A", null)));
			given(loadUserInfoPort.findUserInfoById("user-b"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-b", "유저B", null)));
			given(loadFriendPort.existsFriendship("user-a", "user-b")).willReturn(true);

			assertThatThrownBy(() -> friendService.sendFriendRequest("user-a", "user-b", requestMetaInfo))
					.isInstanceOf(FriendException.class)
					.hasMessageContaining("이미 친구");
		}

		@Test
		@DisplayName("존재하지 않는 사용자에게 요청하면 예외 발생")
		void failsUserNotFound() {
			given(loadUserInfoPort.findUserInfoById("user-a"))
					.willReturn(Optional.of(new LoadUserInfoPort.UserInfo("user-a", "유저", null)));
			given(loadUserInfoPort.findUserInfoById("ghost-id")).willReturn(Optional.empty());

			assertThatThrownBy(() -> friendService.sendFriendRequest("user-a", "ghost-id", requestMetaInfo))
					.isInstanceOf(FriendException.class)
					.hasMessageContaining("찾을 수 없습니다");
		}
	}

	@Nested
	@DisplayName("rejectFriendRequest - 친구 요청 거절")
	class RejectFriendRequest {

		@Test
		@DisplayName("친구 요청을 거절한다")
		void rejectSuccess() {
			FriendRequestID id = new FriendRequestID("from-user", "to-user");
			given(loadFriendRequestPort.existsById(id)).willReturn(true);

			FriendRequestResponseDto response = friendService.rejectFriendRequest("to-user", "from-user");

			assertThat(response.isAccepted()).isFalse();
			assertThat(response.getMessage()).contains("거절");
			then(saveFriendRequestPort).should().deleteById(id);
		}

		@Test
		@DisplayName("존재하지 않는 요청을 거절하면 예외 발생")
		void failsRequestNotFound() {
			FriendRequestID id = new FriendRequestID("from-user", "to-user");
			given(loadFriendRequestPort.existsById(id)).willReturn(false);

			assertThatThrownBy(() -> friendService.rejectFriendRequest("to-user", "from-user"))
					.isInstanceOf(FriendRequestNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("cancelFriendRequest - 친구 요청 취소")
	class CancelFriendRequest {

		@Test
		@DisplayName("보낸 친구 요청을 취소한다")
		void cancelSuccess() {
			FriendRequestID id = new FriendRequestID("from-user", "to-user");
			given(loadFriendRequestPort.existsById(id)).willReturn(true);

			FriendRequestResponseDto response = friendService.cancelFriendRequest("from-user", "to-user");

			assertThat(response.isAccepted()).isFalse();
			assertThat(response.getMessage()).contains("취소");
			then(saveFriendRequestPort).should().deleteById(id);
		}

		@Test
		@DisplayName("존재하지 않는 요청을 취소하면 예외 발생")
		void failsRequestNotFound() {
			FriendRequestID id = new FriendRequestID("from-user", "to-user");
			given(loadFriendRequestPort.existsById(id)).willReturn(false);

			assertThatThrownBy(() -> friendService.cancelFriendRequest("from-user", "to-user"))
					.isInstanceOf(FriendRequestNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("getFriendshipStatus - 친구 관계 상태 조회")
	class GetFriendshipStatus {

		@Test
		@DisplayName("이미 친구면 FRIENDS를 반환한다")
		void returnsFriends() {
			given(loadFriendPort.existsFriendship("me", "other")).willReturn(true);

			assertThat(friendService.getFriendshipStatus("me", "other")).isEqualTo(FriendStatus.FRIENDS);
		}

		@Test
		@DisplayName("내가 요청을 보냈으면 REQUESTED를 반환한다")
		void returnsRequested() {
			given(loadFriendPort.existsFriendship("me", "other")).willReturn(false);
			given(loadFriendRequestPort.findByFromUserIdAndToUserId("me", "other"))
					.willReturn(Optional.of(new FriendRequest("me", "other")));

			assertThat(friendService.getFriendshipStatus("me", "other")).isEqualTo(FriendStatus.REQUESTED);
		}

		@Test
		@DisplayName("상대가 요청을 보냈으면 RECEIVED를 반환한다")
		void returnsReceived() {
			given(loadFriendPort.existsFriendship("me", "other")).willReturn(false);
			given(loadFriendRequestPort.findByFromUserIdAndToUserId("me", "other")).willReturn(Optional.empty());
			given(loadFriendRequestPort.findByFromUserIdAndToUserId("other", "me"))
					.willReturn(Optional.of(new FriendRequest("other", "me")));

			assertThat(friendService.getFriendshipStatus("me", "other")).isEqualTo(FriendStatus.RECEIVED);
		}

		@Test
		@DisplayName("아무 관계도 없으면 NONE을 반환한다")
		void returnsNone() {
			given(loadFriendPort.existsFriendship("me", "other")).willReturn(false);
			given(loadFriendRequestPort.findByFromUserIdAndToUserId("me", "other")).willReturn(Optional.empty());
			given(loadFriendRequestPort.findByFromUserIdAndToUserId("other", "me")).willReturn(Optional.empty());

			assertThat(friendService.getFriendshipStatus("me", "other")).isEqualTo(FriendStatus.NONE);
		}
	}

	@Nested
	@DisplayName("countFriends / getFriends - 친구 수 및 목록 조회")
	class FriendQueries {

		@Test
		@DisplayName("친구 수를 조회한다")
		void countFriends() {
			given(loadFriendPort.countByUserId("user-id")).willReturn(5);

			assertThat(friendService.countFriends("user-id")).isEqualTo(5);
		}

		@Test
		@DisplayName("친구 ID 목록을 조회한다")
		void getFriends() {
			given(loadFriendPort.findFriendIds("user-id")).willReturn(List.of("friend-1", "friend-2"));

			List<String> friends = friendService.getFriends("user-id");

			assertThat(friends).containsExactly("friend-1", "friend-2");
		}
	}

	@Nested
	@DisplayName("removeFriend - 친구 삭제")
	class RemoveFriend {

		@Test
		@DisplayName("친구를 삭제한다")
		void removeSuccess() {
			given(loadFriendPort.existsFriendship("me", "target")).willReturn(true);

			FriendRemoveDTO result = friendService.removeFriend("me", "target");

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getMessage()).contains("해제");
			then(saveFriendPort).should().deleteByUserIds("me", "target");
		}

		@Test
		@DisplayName("친구가 아닌 사용자를 삭제하면 예외 발생")
		void failsNotFriends() {
			given(loadFriendPort.existsFriendship("me", "stranger")).willReturn(false);

			assertThatThrownBy(() -> friendService.removeFriend("me", "stranger"))
					.isInstanceOf(FriendNotFoundException.class);
		}
	}
}
