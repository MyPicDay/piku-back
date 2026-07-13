package com.pikume.back.user.application.service;

import com.pikume.back.user.application.dto.UserIdentityView;
import com.pikume.back.user.application.port.in.QueryUserIdentityUseCase;
import com.pikume.back.user.application.port.out.LoadUserForAuthenticationPort;
import com.pikume.back.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIdentityQueryService implements QueryUserIdentityUseCase {

	private final LoadUserForAuthenticationPort loadUserForAuthenticationPort;

	@Override
	public Optional<UserIdentityView> queryUserIdentityByEmail(String email) {
		return loadUserForAuthenticationPort.loadForLogin(email)
				.map(this::toIdentityView);
	}

	@Override
	public Optional<UserIdentityView> queryUserIdentityById(String userId) {
		return loadUserForAuthenticationPort.loadForSession(userId)
				.map(this::toIdentityView);
	}

	private UserIdentityView toIdentityView(User user) {
		return new UserIdentityView(
				user.getId(),
				user.getPassword(),
				user.getNickname(),
				user.getAvatar());
	}
}
