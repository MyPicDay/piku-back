package store.piku.back.social.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.social.application.port.out.LoadUserInfoPort;
import store.piku.back.user.domain.User;
import store.piku.back.user._legacy.UserReader;

import java.util.Optional;

/**
 * Identity Context의 UserReader를 래핑하여
 * Social Context에서 사용자 정보를 조회하는 cross-context 어댑터.
 */
@Component
@RequiredArgsConstructor
public class UserAdapterForSocial implements LoadUserInfoPort {

	private final UserReader userReader;

	@Override
	public Optional<UserInfo> findUserInfoById(String userId) {
		try {
			User user = userReader.getUserById(userId);
			return Optional.of(new UserInfo(user.getId(), user.getNickname(), user.getAvatar()));
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
