package com.pikume.back.user.application.port.in;

import com.pikume.back.user.application.dto.UserIdentityView;

import java.util.Optional;

public interface QueryUserIdentityUseCase {

	Optional<UserIdentityView> findByEmail(String email);

	Optional<UserIdentityView> findById(String userId);
}
