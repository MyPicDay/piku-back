package com.pikume.back.social.application.readmodel;

public record InteractionDiaryView(
		Long diaryId,
		String ownerUserId,
		boolean anonymous,
		boolean viewerOwner) {
}
