package com.pikume.back.social.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.social.application.port.out.LoadUserInfoPort;
import com.pikume.back.user.application.port.out.LoadUserPort;

import java.util.Optional;

/**
 * Identity Context의 LoadUserPort를 사용하여
 * Social Context에서 사용자 정보를 조회하는 cross-context 어댑터.
 */
@Component
@RequiredArgsConstructor
public class UserAdapterForSocial implements LoadUserInfoPort {

	private final LoadUserPort loadUserPort;

	@Override
	public Optional<UserInfo> findUserInfoById(String userId) {
		return loadUserPort.findById(userId)
				.map(user -> new UserInfo(user.getId(), user.getNickname(), user.getAvatar()));
	}
}
