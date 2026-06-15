package com.pikume.back.security.adapter.out.crosscontext;

import com.pikume.back.security.application.dto.AuthUserView;
import com.pikume.back.security.application.port.out.LoadUserForAuthPort;
import com.pikume.back.user.application.dto.UserIdentityView;
import com.pikume.back.user.application.port.in.QueryUserIdentityUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAdapterForSecurity implements LoadUserForAuthPort {

	private final QueryUserIdentityUseCase queryUserIdentityUseCase;

	@Override
	public Optional<AuthUserView> findByEmail(String email) {
		return queryUserIdentityUseCase.findByEmail(email)
				.map(this::toAuthUserView);
	}

	@Override
	public Optional<AuthUserView> findById(String userId) {
		return queryUserIdentityUseCase.findById(userId)
				.map(this::toAuthUserView);
	}

	private AuthUserView toAuthUserView(UserIdentityView user) {
		return new AuthUserView(
				user.id(),
				user.passwordHash(),
				user.nickname(),
				user.avatarPath());
	}
}
