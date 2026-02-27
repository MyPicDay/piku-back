package com.pikume.back.user.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pikume.back.user.domain.User;

/**
 * 사용자 쿼리 Outbound Port (검색, 존재 확인 등)
 */
public interface UserQueryPort {

	/**
	 * 닉네임으로 사용자 존재 여부를 확인합니다.
	 */
	boolean existsByNickname(String nickname);

	/**
	 * 이메일로 사용자 존재 여부를 확인합니다.
	 */
	boolean existsByEmail(String email);

	/**
	 * 키워드로 사용자를 검색합니다.
	 */
	Page<User> searchByName(String keyword, Pageable pageable);
}
