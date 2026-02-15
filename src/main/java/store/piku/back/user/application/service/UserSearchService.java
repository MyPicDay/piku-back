package store.piku.back.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.user.application.dto.UserSearchResult;
import store.piku.back.user.application.port.in.SearchUserUseCase;
import store.piku.back.user.application.port.out.UserQueryPort;

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
	public Page<UserSearchResult> searchByKeyword(String keyword, Pageable pageable, RequestMetaInfo requestMetaInfo) {
		String formattedKeyword = "%" + keyword + "%";

		return userQueryPort.searchByName(formattedKeyword, pageable)
				.map(user -> {
					String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(user.getAvatar(), requestMetaInfo);
					return new UserSearchResult(user.getId(), user.getNickname(), avatarUrl);
				});
	}
}
