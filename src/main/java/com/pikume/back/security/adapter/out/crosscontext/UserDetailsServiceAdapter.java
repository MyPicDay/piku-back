package com.pikume.back.security.adapter.out.crosscontext;

import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.user.application.dto.UserIdentityView;
import com.pikume.back.user.application.port.in.QueryUserIdentityUseCase;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceAdapter implements UserDetailsService {

	private final QueryUserIdentityUseCase queryUserIdentityUseCase;

	public UserDetailsServiceAdapter(QueryUserIdentityUseCase queryUserIdentityUseCase) {
		this.queryUserIdentityUseCase = queryUserIdentityUseCase;
	}

	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		UserIdentityView user = queryUserIdentityUseCase.queryUserIdentityById(userId)
				.orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));

		return UserPrincipal.withAvatarPath(
				user.id(),
				user.nickname(),
				user.avatarPath());
	}
}
