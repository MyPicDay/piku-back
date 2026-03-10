package com.pikume.back.user.application.port.out;

import com.pikume.back.user.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 조회 Outbound Port
 */
public interface LoadUserPort {

	/**
	 * ID로 사용자를 조회합니다.
	 */
	Optional<User> findById(String userId);

	/**
	 * 이메일로 사용자를 조회합니다.
	 */
	Optional<User> findByEmail(String email);

	List<User> findAllByIds(Collection<String> userIds);
}
