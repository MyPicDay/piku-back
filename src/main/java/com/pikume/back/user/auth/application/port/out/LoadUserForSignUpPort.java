package com.pikume.back.user.auth.application.port.out;

import com.pikume.back.user.domain.User;

import java.util.Optional;

public interface LoadUserForSignUpPort {

	boolean existsByEmail(String email);

	Optional<User> findByEmail(String email);

	User save(User user);
}
