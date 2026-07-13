package com.pikume.back.user.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.user.application.dto.UserSearchResult;
import com.pikume.back.user.application.port.out.SearchUserPort;
import com.pikume.back.user.domain.User;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSearchService")
class UserSearchServiceTest {

	@InjectMocks
	private UserSearchService userSearchService;

	@Mock
	private SearchUserPort searchUserPort;

	@Nested
	@DisplayName("searchUsers")
	class SearchByKeyword {

		@Test
		@DisplayName("키워드로 검색 시 결과가 정상 반환된다")
		void returnsSearchResults() {
			String keyword = "피쿠";
			PageQuery pageQuery = PageQuery.of(0, 10);
			User user = new User("user-1", "piku@test.com", "password", "피쿠유저", "characters/fixed/base_image_1.webp");
			PageResult<User> userPage = new PageResult<>(List.of(user), 0, 10, 1);

			given(searchUserPort.searchUsers("%피쿠%", pageQuery)).willReturn(userPage);
			PageResult<UserSearchResult> result = userSearchService.searchUsers(keyword, pageQuery);

			assertThat(result.getContent()).hasSize(1);
			UserSearchResult searchResult = result.getContent().get(0);
			assertThat(searchResult.id()).isEqualTo("user-1");
			assertThat(searchResult.nickname()).isEqualTo("피쿠유저");
			assertThat(searchResult.avatarObjectKey()).isEqualTo("characters/fixed/base_image_1.webp");
		}

		@Test
		@DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
		void returnsEmptyPageWhenNoResults() {
			String keyword = "존재하지않는유저";
			PageQuery pageQuery = PageQuery.of(0, 10);
			PageResult<User> emptyPage = new PageResult<>(Collections.emptyList(), 0, 10, 0);

			given(searchUserPort.searchUsers("%" + keyword + "%", pageQuery)).willReturn(emptyPage);

			PageResult<UserSearchResult> result = userSearchService.searchUsers(keyword, pageQuery);

			assertThat(result.getContent()).isEmpty();
			assertThat(result.getTotalElements()).isZero();
		}

		@Test
		@DisplayName("키워드에 와일드카드가 올바르게 추가된다")
		void addsWildcardToKeyword() {
			String keyword = "테스트";
			PageQuery pageQuery = PageQuery.of(0, 10);
			PageResult<User> emptyPage = new PageResult<>(Collections.emptyList(), 0, 10, 0);

			given(searchUserPort.searchUsers(anyString(), eq(pageQuery))).willReturn(emptyPage);

			userSearchService.searchUsers(keyword, pageQuery);

			then(searchUserPort).should().searchUsers("%테스트%", pageQuery);
		}

		@Test
		@DisplayName("검색 결과는 Web 기술과 무관한 아바타 object key를 유지한다")
		void keepsAvatarObjectKeyForWebMapping() {
			String keyword = "유저";
			PageQuery pageQuery = PageQuery.of(0, 10);
			User user1 = new User("user-1", "a@test.com", "pw", "유저A", "path/avatar1.png");
			User user2 = new User("user-2", "b@test.com", "pw", "유저B", "path/avatar2.png");
			PageResult<User> userPage = new PageResult<>(List.of(user1, user2), 0, 10, 2);

			given(searchUserPort.searchUsers("%유저%", pageQuery)).willReturn(userPage);
			PageResult<UserSearchResult> result = userSearchService.searchUsers(keyword, pageQuery);

			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getContent().get(0).avatarObjectKey()).isEqualTo("path/avatar1.png");
			assertThat(result.getContent().get(1).avatarObjectKey()).isEqualTo("path/avatar2.png");
		}
	}
}
