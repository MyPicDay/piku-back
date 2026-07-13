package com.pikume.back.user.application.port.out;

import com.pikume.back.user.domain.User;

import java.util.Optional;

public interface LoadUserForAuthenticationPort {

	Optional<User> loadForLogin(String email);

	Optional<User> loadForSession(String userId);
}
