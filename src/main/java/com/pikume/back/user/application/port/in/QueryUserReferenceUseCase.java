package com.pikume.back.user.application.port.in;

import com.pikume.back.user.application.dto.UserReferenceView;
import com.pikume.back.user.application.exception.UserNotFoundException;

import java.util.Optional;

public interface QueryUserReferenceUseCase {

	Optional<UserReferenceView> findUserReference(String userId);

	default UserReferenceView getUserReference(String userId) {
		return findUserReference(userId).orElseThrow(UserNotFoundException::new);
	}
}
