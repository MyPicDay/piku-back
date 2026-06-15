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
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		AuthUserView user = loadUserForAuthPort.findById(userId)
				.orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));

		return CustomUserDetails.withAvatarPath(
				user.id(),
				user.nickname(),
				user.avatarPath());
	}
}
