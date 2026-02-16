package store.piku.back.security.adapter.out.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.security.application.port.out.LoadUserForAuthPort;
import store.piku.back.user.adapter.out.persistence.UserJpaRepository;
import store.piku.back.user.domain.User;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAdapterForSecurity implements LoadUserForAuthPort {

	private final UserJpaRepository userJpaRepository;

	@Override
	public Optional<User> findByEmail(String email) {
		return userJpaRepository.findByEmail(email);
	}
}
