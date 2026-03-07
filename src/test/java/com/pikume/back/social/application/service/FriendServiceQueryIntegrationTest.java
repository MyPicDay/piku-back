package com.pikume.back.social.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import com.pikume.back.social.adapter.out.persistence.FriendJpaRepository;
import com.pikume.back.social.adapter.out.persistence.FriendRequestJpaRepository;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;

class FriendServiceQueryIntegrationTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private FriendService friendService;

	@Autowired
	private FriendJpaRepository friendJpaRepository;

	@Autowired
	private FriendRequestJpaRepository friendRequestJpaRepository;

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
				friendService.findFriendList(PageRequest.of(0, 1), me.getId(), REQUEST_META_INFO));
		long threeItemQueries = measurePreparedStatements(() ->
				friendService.findFriendList(PageRequest.of(0, 3), me.getId(), REQUEST_META_INFO));

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
				friendService.findFriendRequests(PageRequest.of(0, 1), me.getId(), REQUEST_META_INFO));
		long threeItemQueries = measurePreparedStatements(() ->
				friendService.findFriendRequests(PageRequest.of(0, 3), me.getId(), REQUEST_META_INFO));

		assertThat(threeItemQueries)
				.as("친구 요청 row 수가 늘어도 사용자 조회를 배치로 제한해야 한다")
				.isEqualTo(oneItemQueries);
	}

	private User saveUser(String suffix) {
		return userJpaRepository.save(new User(
				suffix + "@example.com",
				"encoded-password",
				"nick-" + suffix,
				"avatars/" + suffix + ".png"));
	}
}
