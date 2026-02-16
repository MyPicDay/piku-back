package store.piku.back.user.auth.adapter.out.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.user.auth.application.port.out.LoadUserForSignUpPort;
import store.piku.back.user.domain.User;
import store.piku.back.user.adapter.out.persistence.UserJpaRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAdapterForAuth implements LoadUserForSignUpPort {

	private final UserJpaRepository userJpaRepository;

	@Override
	public boolean existsByEmail(String email) {
		return userJpaRepository.existsByEmail(email);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return userJpaRepository.findByEmail(email);
	}

	@Override
	public User save(User user) {
		return userJpaRepository.save(user);
	}
}
