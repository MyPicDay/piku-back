package com.pikume.back.user.adapter.out.persistence;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("UserSearchPersistenceAdapter")
class UserSearchPersistenceAdapterTest {

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("탈퇴 사용자는 닉네임 검색 결과와 전체 건수에서 제외한다")
	void excludesWithdrawnUsersFromSearchResultsAndTotalCount() {
		User activeUser = new User(
				"active@example.com",
				"password",
				"pikume-active",
				"avatars/active.png");
		User withdrawnUser = new User(
				"withdrawn@example.com",
				"password",
				"pikume-withdrawn",
				"avatars/withdrawn.png");
		withdrawnUser.withdraw();
		userJpaRepository.saveAllAndFlush(List.of(activeUser, withdrawnUser));
		entityManager.clear();

		var result = new UserSearchPersistenceAdapter(userJpaRepository)
				.searchUsers("%pikume%", PageQuery.of(0, 20));

		assertThat(result.getContent())
				.extracting(User::getId)
				.containsExactly(activeUser.getId());
		assertThat(result.getTotalElements()).isEqualTo(1L);
	}
}
