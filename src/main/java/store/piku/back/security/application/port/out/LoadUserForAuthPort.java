package store.piku.back.security.application.port.out;

import store.piku.back.user.domain.User;

import java.util.Optional;

public interface LoadUserForAuthPort {

	Optional<User> findByEmail(String email);
}
