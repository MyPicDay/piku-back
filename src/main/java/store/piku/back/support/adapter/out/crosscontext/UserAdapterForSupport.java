package store.piku.back.support.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.support.application.port.out.LoadUserInfoForSupportPort;
import store.piku.back.user._legacy.UserReader;

@Component
@RequiredArgsConstructor
public class UserAdapterForSupport implements LoadUserInfoForSupportPort {

	private final UserReader userReader;

	@Override
	public boolean existsById(String userId) {
		try {
			userReader.getUserById(userId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
