package com.pikume.back.user.auth.adapter.out.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.user.auth.application.port.out.LoadUserForSignUpPort;
import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.vo.Email;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAdapterForAuth implements LoadUserForSignUpPort {

	private final UserJpaRepository userJpaRepository;

	@Override
	public boolean existsByEmail(String email) {
		return userJpaRepository.existsByEmail(new Email(email));
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return userJpaRepository.findByEmail(new Email(email));
	}

	@Override
	public User save(User user) {
		return userJpaRepository.save(user);
	}
}
