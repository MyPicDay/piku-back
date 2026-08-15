package com.pikume.back.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.user.application.dto.UserSearchResult;
import com.pikume.back.user.application.dto.AvatarCharacterSelection;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.port.in.SearchUserUseCase;
import com.pikume.back.user.application.port.out.SearchUserPort;
import com.pikume.back.user.domain.User;

import java.util.Map;

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
	private final UserAvatarReferenceResolver userAvatarReferenceResolver;

	@Override
	public PageResult<UserSearchResult> searchUsers(String keyword, PageQuery pageQuery) {
		String formattedKeyword = "%" + keyword + "%";

		PageResult<User> users = searchUserPort.searchUsers(formattedKeyword, pageQuery);
		Map<AvatarCharacterSelection, UserAvatarReference> avatarReferences = userAvatarReferenceResolver
				.resolveRequired(users.getContent().stream()
						.map(user -> new AvatarCharacterSelection(user.getId(), user.getCharacterId()))
						.toList());

		return users.map(user -> new UserSearchResult(
				user.getId(),
				user.getNickname(),
				avatarReferences.get(new AvatarCharacterSelection(user.getId(), user.getCharacterId()))));
	}
}
