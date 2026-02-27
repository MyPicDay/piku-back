package com.pikume.back.security.config;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.user.domain.User;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {
	private final UserJpaRepository userRepository;

	public CustomUserDetailService(UserJpaRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("이메일 없음"));

		return new CustomUserDetails(
				user.getId(),
				user.getEmail(),
				user.getNickname());
	}
}
