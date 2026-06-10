package com.pikume.back.notification.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.port.out.ResolveImageUrlPort;
import com.pikume.back.notification.application.port.out.LoadDiaryForNotificationPort;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DiaryAdapterForNotification implements LoadDiaryForNotificationPort {

	private final QueryDiaryReadUseCase queryDiaryReadUseCase;
	private final ResolveImageUrlPort resolveImageUrlPort;

	@Override
	public String getDiaryThumbnailUrl(Long diaryId) {
		DiaryNotificationInfo diaryInfo = getDiaryNotificationInfo(diaryId);
		return diaryInfo != null ? diaryInfo.thumbnailUrl() : null;
	}

	@Override
	public DiaryNotificationInfo getDiaryNotificationInfo(Long diaryId) {
		if (diaryId == null) {
			return null;
		}
		DiarySummaryView diary = queryDiaryReadUseCase.getDiarySummaries(Set.of(diaryId)).get(diaryId);
		if (diary == null) {
			return null;
		}
		boolean anonymous = diary.status() == DiaryVisibility.ANONYMOUS;
		return queryDiaryReadUseCase.getRepresentPhotoPaths(Set.of(diaryId)).values().stream()
				.findFirst()
				.map(path -> resolveImageUrlPort.getPhotoUrl(path, true))
				.map(thumbnailUrl -> new DiaryNotificationInfo(diaryId, thumbnailUrl, anonymous))
				.orElse(new DiaryNotificationInfo(diaryId, null, anonymous));
	}
}
