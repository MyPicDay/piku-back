package store.piku.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.diary.application.port.out.LoadUserForDiaryPort;
import store.piku.back.user.application.port.out.LoadUserPort;
import store.piku.back.user.domain.User;
import store.piku.back.global.exception.BusinessException;
import store.piku.back.global.error.ErrorCode;

@Component
@RequiredArgsConstructor
public class UserAdapterForDiary implements LoadUserForDiaryPort {

	private final LoadUserPort loadUserPort;

	@Override
	public String getUserNickname(String userId) {
		User user = loadUserPort.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return user.getNickname();
	}

	@Override
	public String getUserAvatar(String userId) {
		User user = loadUserPort.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return user.getAvatar();
	}

	@Override
	public boolean existsById(String userId) {
		return loadUserPort.findById(userId).isPresent();
	}
}
