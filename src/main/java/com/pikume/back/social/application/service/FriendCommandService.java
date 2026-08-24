package com.pikume.back.social.application.service;

import com.pikume.back.social.application.dto.FriendRemovalResult;
import com.pikume.back.social.application.dto.FriendRequestResult;
import com.pikume.back.social.application.event.SocialNotificationEvent;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.port.in.CancelFriendRequestUseCase;
import com.pikume.back.social.application.port.in.RejectFriendRequestUseCase;
import com.pikume.back.social.application.port.in.RemoveFriendshipUseCase;
import com.pikume.back.social.application.port.in.SendFriendRequestUseCase;
import com.pikume.back.social.application.port.out.LoadFriendshipsPort;
import com.pikume.back.social.application.port.out.LoadPendingFriendRequestsPort;
import com.pikume.back.social.application.port.out.PublishSocialNotificationEventPort;
import com.pikume.back.social.application.port.out.RecordFriendRequestPort;
import com.pikume.back.social.application.port.out.RecordFriendshipPort;
import com.pikume.back.social.application.port.out.VerifySocialParticipantPort;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendCommandService implements SendFriendRequestUseCase, RejectFriendRequestUseCase,
		CancelFriendRequestUseCase, RemoveFriendshipUseCase {

	private final LoadFriendshipsPort loadFriendshipsPort;
	private final RecordFriendshipPort recordFriendshipPort;
	private final LoadPendingFriendRequestsPort loadPendingFriendRequestsPort;
	private final RecordFriendRequestPort recordFriendRequestPort;
	private final VerifySocialParticipantPort verifySocialParticipantPort;
	private final PublishSocialNotificationEventPort publishSocialNotificationEventPort;

	@Override
	@Transactional
	public FriendRequestResult sendFriendRequest(String fromUserId, String toUserId) {
		verifyParticipant(fromUserId);
		verifyParticipant(toUserId);
		if (fromUserId.equals(toUserId)) {
			throw new SocialException(SocialErrorCode.SELF_FRIEND_REQUEST);
		}
		if (loadFriendshipsPort.friendshipExists(fromUserId, toUserId)) {
			throw new SocialException(SocialErrorCode.ALREADY_FRIENDS);
		}

		var reverseRequestId = new FriendRequestID(toUserId, fromUserId);
		var reverseRequest = loadPendingFriendRequestsPort.loadPendingRequest(reverseRequestId);
		if (reverseRequest.isPresent()) {
			recordFriendRequestPort.closePendingRequest(reverseRequest.get());
			recordFriendshipPort.establishFriendship(new Friend(fromUserId, toUserId));
			publishSocialNotificationEventPort.publish(
					new SocialNotificationEvent.FriendAccepted(toUserId, fromUserId));
			return new FriendRequestResult(true, "친구 요청을 수락했습니다.");
		}

		boolean recorded = recordFriendRequestPort.tryRecordPendingRequest(
				new FriendRequest(fromUserId, toUserId));
		if (!recorded) {
			throw new SocialException(SocialErrorCode.DUPLICATE_FRIEND_REQUEST);
		}
		publishSocialNotificationEventPort.publish(
				new SocialNotificationEvent.FriendRequest(toUserId, fromUserId));
		return new FriendRequestResult(false, "친구 요청을 보냈습니다.");
	}

	@Override
	@Transactional
	public FriendRequestResult rejectFriendRequest(String toUserId, String fromUserId) {
		var requestId = new FriendRequestID(fromUserId, toUserId);
		if (!loadPendingFriendRequestsPort.pendingRequestExists(requestId)) {
			throw new SocialException(SocialErrorCode.FRIEND_REQUEST_NOT_FOUND);
		}
		recordFriendRequestPort.closePendingRequest(requestId);
		return new FriendRequestResult(false, "친구 요청을 거절했습니다.");
	}

	@Override
	@Transactional
	public FriendRequestResult cancelFriendRequest(String fromUserId, String toUserId) {
		var requestId = new FriendRequestID(fromUserId, toUserId);
		if (!loadPendingFriendRequestsPort.pendingRequestExists(requestId)) {
			throw new SocialException(SocialErrorCode.SENT_FRIEND_REQUEST_NOT_FOUND);
		}
		recordFriendRequestPort.closePendingRequest(requestId);
		return new FriendRequestResult(false, "친구 요청을 취소했습니다.");
	}

	@Override
	@Transactional
	public FriendRemovalResult removeFriend(String currentUserId, String targetUserId) {
		if (!loadFriendshipsPort.friendshipExists(currentUserId, targetUserId)) {
			throw new SocialException(SocialErrorCode.FRIEND_NOT_FOUND);
		}
		recordFriendshipPort.removeFriendship(currentUserId, targetUserId);
		return new FriendRemovalResult(true, "친구 관계가 해제되었습니다.");
	}

	private void verifyParticipant(String userId) {
		if (!verifySocialParticipantPort.participantExists(userId)) {
			throw new SocialException(SocialErrorCode.INVALID_FRIEND_PARTICIPANT);
		}
	}
}
