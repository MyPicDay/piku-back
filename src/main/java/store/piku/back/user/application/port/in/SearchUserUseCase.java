package store.piku.back.user.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.user.application.dto.UserSearchResult;

/**
 * 사용자 검색 유스케이스 (Inbound Port)
 */
public interface SearchUserUseCase {

	/**
	 * 키워드로 사용자를 검색합니다.
	 */
	Page<UserSearchResult> searchByKeyword(String keyword, Pageable pageable, RequestMetaInfo requestMetaInfo);
}
