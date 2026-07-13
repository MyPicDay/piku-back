package com.pikume.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.out.LoadUserForDiaryPort;
import com.pikume.back.user.application.port.in.QueryUserReferenceUseCase;

@Component
@RequiredArgsConstructor
public class UserAdapterForDiary implements LoadUserForDiaryPort {

	private final QueryUserReferenceUseCase queryUserReferenceUseCase;

	@Override
	public String getUserNickname(String userId) {
		return queryUserReferenceUseCase.getUserReference(userId).nickname();
	}

	@Override
	public String getUserAvatar(String userId) {
		return queryUserReferenceUseCase.getUserReference(userId).avatarPath();
	}

	@Override
	public boolean existsById(String userId) {
		return queryUserReferenceUseCase.findUserReference(userId).isPresent();
	}
}
