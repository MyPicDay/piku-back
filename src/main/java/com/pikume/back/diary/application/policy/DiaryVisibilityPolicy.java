package com.pikume.back.diary.application.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.out.LoadFriendshipForDiaryPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DiaryVisibilityPolicy {

	private final LoadFriendshipForDiaryPort loadFriendshipForDiaryPort;

	public boolean isHiddenFromViewer(Diary diary, String viewerId) {
		return isHiddenFromViewer(diary.getUserId(), diary.getStatus(), viewerId);
	}

	public boolean isHiddenFromViewer(String ownerUserId, DiaryVisibility visibility, String viewerId) {
		return !canView(ownerUserId, visibility, viewerId);
	}

	public List<DiaryVisibility> visibleStatusesForOwner(String ownerUserId, String viewerId) {
		return switch (resolveRelation(ownerUserId, viewerId)) {
			case OWNER -> List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS, DiaryVisibility.PRIVATE);
			case FRIEND -> List.of(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS);
			case STRANGER -> List.of(DiaryVisibility.PUBLIC);
		};
	}

	public boolean isOwner(String ownerUserId, String viewerId) {
		return Objects.equals(ownerUserId, viewerId);
	}

	private boolean canView(String ownerUserId, DiaryVisibility visibility, String viewerId) {
		return switch (visibility) {
			case PUBLIC -> true;
			case PRIVATE -> isOwner(ownerUserId, viewerId);
			case FRIENDS -> switch (resolveRelation(ownerUserId, viewerId)) {
				case OWNER, FRIEND -> true;
				case STRANGER -> false;
			};
		};
	}

	private ViewerRelation resolveRelation(String ownerUserId, String viewerId) {
		if (isOwner(ownerUserId, viewerId)) {
			return ViewerRelation.OWNER;
		}
		if (viewerId == null) {
			return ViewerRelation.STRANGER;
		}
		return loadFriendshipForDiaryPort.areFriends(ownerUserId, viewerId)
				? ViewerRelation.FRIEND
				: ViewerRelation.STRANGER;
	}

	private enum ViewerRelation {
		OWNER,
		FRIEND,
		STRANGER
	}
}
