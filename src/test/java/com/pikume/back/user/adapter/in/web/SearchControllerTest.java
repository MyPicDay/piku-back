package com.pikume.back.user.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.user.application.dto.UserSearchResult;
import com.pikume.back.user.application.port.in.SearchUserUseCase;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchController")
class SearchControllerTest {

	@InjectMocks
	private SearchController searchController;

	@Mock
	private SearchUserUseCase searchUserUseCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(searchController)
				.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
				.build();
	}

	@Test
	@DisplayName("GET /api/search는 keyword/page/size 계약으로 검색 결과를 반환한다")
	void searchUsersReturnsPagedResults() throws Exception {
		PageQuery pageQuery = PageQuery.of(0, 20);
		PageResult<UserSearchResult> result = new PageResult<>(
				List.of(new UserSearchResult("user-1", "테스트유저", "https://cdn.example/avatar.png")),
				0,
				20,
				1);
		given(searchUserUseCase.searchByKeyword("test", pageQuery)).willReturn(result);

		mockMvc.perform(get("/api/search")
						.param("keyword", "test")
						.param("page", "0")
						.param("size", "20")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].userId").value("user-1"))
				.andExpect(jsonPath("$.content[0].nickname").value("테스트유저"))
				.andExpect(jsonPath("$.content[0].avatar").value("https://cdn.example/avatar.png"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.number").value(0));

		verify(searchUserUseCase).searchByKeyword("test", pageQuery);
	}
}
