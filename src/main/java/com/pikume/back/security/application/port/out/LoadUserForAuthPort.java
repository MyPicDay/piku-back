package com.pikume.back.security.application.port.out;

import com.pikume.back.user.domain.User;

import java.util.Optional;

public interface LoadUserForAuthPort {

	Optional<User> findByEmail(String email);
}
