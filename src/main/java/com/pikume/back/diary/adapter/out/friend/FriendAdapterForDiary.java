package com.pikume.back.diary.adapter.out.friend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.out.LoadFriendshipForDiaryPort;
import com.pikume.back.social.application.port.in.FriendUseCase;

@Component
@RequiredArgsConstructor
public class FriendAdapterForDiary implements LoadFriendshipForDiaryPort {

	private final FriendUseCase friendUseCase;

	@Override
	public boolean areFriends(String ownerUserId, String viewerUserId) {
		return friendUseCase.areFriends(ownerUserId, viewerUserId);
	}
}
