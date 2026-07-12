package com.pikume.back.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.user.application.dto.UserSearchResult;
import com.pikume.back.user.application.port.in.SearchUserUseCase;
import com.pikume.back.user.application.port.out.SearchUserPort;

/**
 * 사용자 검색 Application Service
 * SearchUserUseCase를 구현합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserSearchService implements SearchUserUseCase {

	private final SearchUserPort searchUserPort;

	@Override
	public PageResult<UserSearchResult> searchByKeyword(String keyword, PageQuery pageQuery) {
		String formattedKeyword = "%" + keyword + "%";

		return searchUserPort.searchByName(formattedKeyword, pageQuery)
				.map(user -> new UserSearchResult(user.getId(), user.getNickname(), user.getAvatar()));
	}
}
