package com.pikume.back.user.application.service;

import com.pikume.back.user.application.dto.UserReferenceView;
import com.pikume.back.user.application.port.in.QueryUserReferenceUseCase;
import com.pikume.back.user.application.port.out.LoadUserAccountPort;
import com.pikume.back.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReferenceQueryService implements QueryUserReferenceUseCase {

	private final LoadUserAccountPort loadUserAccountPort;

	@Override
	public Optional<UserReferenceView> findUserReference(String userId) {
		return loadUserAccountPort.findById(userId).map(this::toReferenceView);
	}

	private UserReferenceView toReferenceView(User user) {
		return new UserReferenceView(user.getId(), user.getNickname(), user.getAvatar());
	}
}
