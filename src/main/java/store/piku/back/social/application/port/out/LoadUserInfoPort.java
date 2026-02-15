package store.piku.back.social.application.port.out;

import java.util.Optional;

/**
 * Identity Context에서 사용자 정보를 조회하는 cross-context port.
 */
public interface LoadUserInfoPort {

	record UserInfo(String userId, String nickname, String avatar) {
	}

	Optional<UserInfo> findUserInfoById(String userId);
}
