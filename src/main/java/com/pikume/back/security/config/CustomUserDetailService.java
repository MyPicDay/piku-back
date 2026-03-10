package com.pikume.back.security.config;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.security.application.dto.AuthUserView;
import com.pikume.back.security.application.port.out.LoadUserForAuthPort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {
	private final LoadUserForAuthPort loadUserForAuthPort;

	public CustomUserDetailService(LoadUserForAuthPort loadUserForAuthPort) {
		this.loadUserForAuthPort = loadUserForAuthPort;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		AuthUserView user = loadUserForAuthPort.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("이메일 없음"));

		return new CustomUserDetails(
				user.id(),
				user.email(),
				user.nickname());
	}
}
