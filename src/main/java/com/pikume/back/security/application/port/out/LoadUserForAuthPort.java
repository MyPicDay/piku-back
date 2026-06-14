package com.pikume.back.security.application.port.out;

import com.pikume.back.security.application.dto.AuthUserView;

import java.util.Optional;

public interface LoadUserForAuthPort {

	Optional<AuthUserView> findByEmail(String email);

	Optional<AuthUserView> findById(String userId);
}
