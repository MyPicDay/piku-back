package com.pikume.back.social.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.notification.adapter.out.persistence.NotificationJpaRepository;
import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.social.adapter.out.persistence.FriendJpaRepository;
import com.pikume.back.social.adapter.out.persistence.FriendRequestJpaRepository;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Friend request Problem Details integration")
class FriendRequestProblemDetailsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private FriendJpaRepository friendJpaRepository;

	@Autowired
	private FriendRequestJpaRepository friendRequestJpaRepository;

	@Autowired
	private NotificationJpaRepository notificationJpaRepository;

	@BeforeEach
	void clearDataBeforeTest() {
		clearData();
	}

	@AfterEach
	void clearDataAfterTest() {
		clearData();
	}

	@Test
	@DisplayName("커밋된 친구 요청을 다시 보내면 실제 HTTP 경계에서 409 Problem Details를 반환한다")
	void duplicateFriendRequestReturnsProblemDetailsAfterTransactionCompletion() throws Exception {
		User requester = saveUser("requester");
		User target = saveUser("target");
		String requestBody = objectMapper.writeValueAsString(Map.of("toUserId", target.getId()));
		UserPrincipal principal = new UserPrincipal(requester.getId(), requester.getNickname());

		mockMvc.perform(post("/api/relation")
					.with(user(principal))
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isOk());

		assertThat(friendRequestJpaRepository.count()).isEqualTo(1);
		assertThat(notificationJpaRepository.count()).isEqualTo(1);

		mockMvc.perform(post("/api/relation")
					.with(user(principal))
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type")
						.value("https://api.pikume.com/problems/social/duplicate-friend-request"))
				.andExpect(jsonPath("$.title").value("Conflict"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("이미 친구 요청을 보냈습니다."))
				.andExpect(jsonPath("$.instance").value("/api/relation"));

		assertThat(friendRequestJpaRepository.count()).isEqualTo(1);
		assertThat(notificationJpaRepository.count()).isEqualTo(1);
	}

	private User saveUser(String suffix) {
		return userJpaRepository.saveAndFlush(new User(
				suffix + "@example.com",
				"encoded-password",
				"nick-" + suffix,
				1L));
	}

	private void clearData() {
		notificationJpaRepository.deleteAll();
		friendRequestJpaRepository.deleteAll();
		friendJpaRepository.deleteAll();
		userJpaRepository.deleteAll();
	}
}
