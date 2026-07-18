package com.pikume.back.diary.adapter.out.crosscontext;

import com.pikume.back.diary.application.port.out.LoadFriendshipForDiaryPort;
import com.pikume.back.social.application.port.in.FriendUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FriendAdapterForDiary implements LoadFriendshipForDiaryPort {

	private final FriendUseCase friendUseCase;

	@Override
	public boolean areFriends(String ownerUserId, String viewerUserId) {
		return friendUseCase.areFriends(ownerUserId, viewerUserId);
	}

	@Override
	public List<String> findFriendIds(String userId) {
		return friendUseCase.getFriends(userId);
	}
}
