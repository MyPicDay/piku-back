package com.pikume.back.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.user.application.dto.UserSearchResult;
import com.pikume.back.user.application.port.in.SearchUserUseCase;
import com.pikume.back.user.application.port.out.UserQueryPort;

/**
 * 사용자 검색 Application Service
 * SearchUserUseCase를 구현합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserSearchService implements SearchUserUseCase {

	private final UserQueryPort userQueryPort;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public PageResult<UserSearchResult> searchByKeyword(String keyword, PageQuery pageQuery) {
		String formattedKeyword = "%" + keyword + "%";

		return userQueryPort.searchByName(formattedKeyword, pageQuery)
				.map(user -> {
					String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(user.getAvatar());
					return new UserSearchResult(user.getId(), user.getNickname(), avatarUrl);
				});
	}
}
