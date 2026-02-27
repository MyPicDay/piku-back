package com.pikume.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.out.LoadUserForDiaryPort;
import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.domain.User;
import com.pikume.back.global.exception.BusinessException;
import com.pikume.back.global.error.ErrorCode;

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
