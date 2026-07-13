package com.pikume.back.user.application.port.in;

import com.pikume.back.user.application.dto.UserReferenceView;
import com.pikume.back.user.application.exception.UserNotFoundException;

import java.util.Optional;

public interface QueryUserReferenceUseCase {

	Optional<UserReferenceView> queryUserReference(String userId);

	default UserReferenceView requireUserReference(String userId) {
		return queryUserReference(userId).orElseThrow(UserNotFoundException::new);
	}
}
