package com.pikume.back.notification.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
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
		return queryDiaryReadUseCase.getRepresentPhotoPaths(Set.of(diaryId)).values().stream()
				.findFirst()
				.map(path -> resolveImageUrlPort.getPhotoUrl(path, true))
				.orElse(null);
	}
}
