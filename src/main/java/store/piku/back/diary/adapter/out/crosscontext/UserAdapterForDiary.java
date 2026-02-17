package store.piku.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.diary.application.port.out.LoadUserForDiaryPort;
import store.piku.back.user._legacy.UserReader;
import store.piku.back.user.domain.User;

@Component
@RequiredArgsConstructor
public class UserAdapterForDiary implements LoadUserForDiaryPort {

	private final UserReader userReader;

	@Override
	public String getUserNickname(String userId) {
		User user = userReader.getUserById(userId);
		return user.getNickname();
	}

	@Override
	public String getUserAvatar(String userId) {
		User user = userReader.getUserById(userId);
		return user.getAvatar();
	}

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
