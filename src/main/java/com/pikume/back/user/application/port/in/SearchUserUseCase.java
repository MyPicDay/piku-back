package com.pikume.back.user.application.port.in;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.user.application.dto.UserSearchResult;

/**
 * 사용자 검색 유스케이스 (Inbound Port)
 */
public interface SearchUserUseCase {

	/**
	 * 키워드로 사용자를 검색합니다.
	 */
	PageResult<UserSearchResult> searchByKeyword(String keyword, PageQuery pageQuery);
}
