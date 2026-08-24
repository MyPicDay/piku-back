package com.pikume.back.social.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.notification.adapter.out.persistence.NotificationJpaRepository;
import com.pikume.back.social.adapter.out.persistence.FriendJpaRepository;
import com.pikume.back.social.adapter.out.persistence.FriendRequestJpaRepository;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.port.out.RecordFriendRequestPort;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriendQueryServiceIntegrationTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private FriendQueryService friendQueryService;

	@Autowired
	private FriendCommandService friendCommandService;

	@Autowired
	private FriendJpaRepository friendJpaRepository;

	@Autowired
	private FriendRequestJpaRepository friendRequestJpaRepository;

	@Autowired
	private RecordFriendRequestPort recordFriendRequestPort;

	@Autowired
	private NotificationJpaRepository notificationJpaRepository;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Test
	@DisplayName("친구 목록 조회는 row 수가 커져도 쿼리 수가 일정하게 유지된다")
	void friendListQueryCountStaysBounded() {
		User me = saveUser("me");
		User friend1 = saveUser("friend1");
		User friend2 = saveUser("friend2");
		User friend3 = saveUser("friend3");

		friendJpaRepository.save(new Friend(me.getId(), friend1.getId()));
		friendJpaRepository.save(new Friend(me.getId(), friend2.getId()));
		friendJpaRepository.save(new Friend(me.getId(), friend3.getId()));

		long oneItemQueries = measurePreparedStatements(() ->
				friendQueryService.queryFriendPage(PageQuery.of(0, 1), me.getId()));
		long threeItemQueries = measurePreparedStatements(() ->
				friendQueryService.queryFriendPage(PageQuery.of(0, 3), me.getId()));

		assertThat(threeItemQueries)
				.as("친구 row 수가 늘어도 사용자 조회를 배치로 제한해야 한다")
				.isEqualTo(oneItemQueries);
	}

	@Test
	@DisplayName("친구 요청 목록 조회는 row 수가 커져도 쿼리 수가 일정하게 유지된다")
	void friendRequestQueryCountStaysBounded() {
		User me = saveUser("request-me");
		User requester1 = saveUser("requester1");
		User requester2 = saveUser("requester2");
		User requester3 = saveUser("requester3");

		friendRequestJpaRepository.save(new FriendRequest(requester1.getId(), me.getId()));
		friendRequestJpaRepository.save(new FriendRequest(requester2.getId(), me.getId()));
		friendRequestJpaRepository.save(new FriendRequest(requester3.getId(), me.getId()));

		long oneItemQueries = measurePreparedStatements(() ->
				friendQueryService.queryReceivedFriendRequestPage(PageQuery.of(0, 1), me.getId()));
		long threeItemQueries = measurePreparedStatements(() ->
				friendQueryService.queryReceivedFriendRequestPage(PageQuery.of(0, 3), me.getId()));

		assertThat(threeItemQueries)
				.as("친구 요청 row 수가 늘어도 사용자 조회를 배치로 제한해야 한다")
				.isEqualTo(oneItemQueries);
	}

	@Test
	@DisplayName("친구 요청 저장 포트는 DB 중복 요청을 예외 대신 false로 반환한다")
	void friendRequestSaveIfAbsentReturnsFalseOnDuplicate() {
		FriendRequestID id = new FriendRequestID("duplicate-from", "duplicate-to");

		boolean firstSaved = recordFriendRequestPort.tryRecordPendingRequest(
				new FriendRequest("duplicate-from", "duplicate-to"));
		boolean duplicateSaved = recordFriendRequestPort.tryRecordPendingRequest(
				new FriendRequest("duplicate-from", "duplicate-to"));

		assertThat(firstSaved).isTrue();
		assertThat(duplicateSaved).isFalse();
		assertThat(friendRequestJpaRepository.findById(id)).isPresent();
	}

	@Test
	@DisplayName("친구 요청 저장 포트는 호출한 트랜잭션에 참여한다")
	void friendRequestSaveIfAbsentParticipatesInCurrentTransaction() {
		FriendRequestID id = new FriendRequestID("tx-from", "tx-to");

		boolean saved = recordFriendRequestPort.tryRecordPendingRequest(new FriendRequest("tx-from", "tx-to"));
		assertThat(saved).isTrue();

		TestTransaction.flagForRollback();
		TestTransaction.end();
		TestTransaction.start();

		assertThat(friendRequestJpaRepository.findById(id)).isEmpty();
	}

	@Test
	@DisplayName("친구 요청과 알림 이력 저장은 같은 트랜잭션에 참여한다")
	void friendRequestAndNotificationParticipateInCurrentTransaction() {
		User fromUser = saveUser("tx-flow-from");
		User toUser = saveUser("tx-flow-to");
		FriendRequestID id = new FriendRequestID(fromUser.getId(), toUser.getId());

		var result = friendCommandService.sendFriendRequest(fromUser.getId(), toUser.getId());

		assertThat(result.accepted()).isFalse();
		assertThat(friendRequestJpaRepository.findById(id)).isPresent();
		assertThat(notificationJpaRepository.existsActiveFriendRequestByReceiverId(toUser.getId())).isTrue();

		TestTransaction.flagForRollback();
		TestTransaction.end();
		TestTransaction.start();

		assertThat(friendRequestJpaRepository.findById(id)).isEmpty();
		assertThat(notificationJpaRepository.existsActiveFriendRequestByReceiverId(toUser.getId())).isFalse();
	}

	@Test
	@DisplayName("같은 방향의 중복 친구 요청은 트랜잭션 내부에서 Social 오류로 종료된다")
	void duplicateFriendRequestEndsWithSocialErrorInsideTransaction() {
		User fromUser = saveUser("dup-flow-from");
		User toUser = saveUser("dup-flow-to");

		friendCommandService.sendFriendRequest(fromUser.getId(), toUser.getId());

		assertThatThrownBy(() -> friendCommandService.sendFriendRequest(fromUser.getId(), toUser.getId()))
				.isInstanceOfSatisfying(SocialException.class, exception -> {
					assertThat(exception.getErrorCode()).isEqualTo(SocialErrorCode.DUPLICATE_FRIEND_REQUEST);
					assertThat(exception).hasMessage("이미 친구 요청을 보냈습니다.");
				});
	}

	private User saveUser(String suffix) {
		return userJpaRepository.save(new User(
				suffix + "@example.com",
				"encoded-password",
				"nick-" + suffix,
				1L));
	}
}
