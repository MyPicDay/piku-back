package com.pikume.back.security.adapter.out.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.security.application.dto.AuthUserView;
import com.pikume.back.security.application.port.out.LoadUserForAuthPort;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAdapterForSecurity implements LoadUserForAuthPort {

	private final UserJpaRepository userJpaRepository;

	@Override
	public Optional<AuthUserView> findByEmail(String email) {
		return userJpaRepository.findByEmail(email)
				.map(user -> new AuthUserView(
						user.getId(),
						user.getEmail(),
						user.getPassword(),
						user.getNickname(),
						user.getAvatar()));
	}
}
