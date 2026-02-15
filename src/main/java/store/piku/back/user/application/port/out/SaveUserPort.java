package store.piku.back.user.application.port.out;

import store.piku.back.user.domain.User;

/**
 * 사용자 저장 Outbound Port
 */
public interface SaveUserPort {

	/**
	 * 사용자를 저장합니다.
	 */
	User save(User user);
}
