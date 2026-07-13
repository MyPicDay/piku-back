package com.pikume.back.user.application.port.in;

import com.pikume.back.user.application.dto.UserIdentityView;

import java.util.Optional;

public interface QueryUserIdentityUseCase {

	Optional<UserIdentityView> queryUserIdentityByEmail(String email);

	Optional<UserIdentityView> queryUserIdentityById(String userId);
}
