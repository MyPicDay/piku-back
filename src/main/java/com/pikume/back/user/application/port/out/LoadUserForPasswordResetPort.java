package com.pikume.back.user.application.port.out;

import com.pikume.back.user.domain.User;

import java.util.Optional;

public interface LoadUserForPasswordResetPort {

	Optional<User> loadPasswordResetUser(String email);
}
