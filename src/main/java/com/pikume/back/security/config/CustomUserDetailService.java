package com.pikume.back.security.config;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.user.application.dto.UserIdentityView;
import com.pikume.back.user.application.port.in.QueryUserIdentityUseCase;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {
	private final QueryUserIdentityUseCase queryUserIdentityUseCase;

	public CustomUserDetailService(QueryUserIdentityUseCase queryUserIdentityUseCase) {
		this.queryUserIdentityUseCase = queryUserIdentityUseCase;
	}

	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		UserIdentityView user = queryUserIdentityUseCase.queryUserIdentityById(userId)
				.orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));

		return CustomUserDetails.withAvatarPath(
				user.id(),
				user.nickname(),
				user.avatarPath());
	}
}
