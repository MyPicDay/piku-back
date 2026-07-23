package com.pikume.back.notification.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.notification.application.port.out.LoadNotificationDiaryContextsPort;
import com.pikume.back.notification.application.readmodel.NotificationDiaryContextView;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DiaryAdapterForNotification implements LoadNotificationDiaryContextsPort {

	private final QueryDiaryReadUseCase queryDiaryReadUseCase;
	private final ResolveObjectUrlPort resolveObjectUrlPort;

	@Override
	public Map<Long, NotificationDiaryContextView> loadNotificationDiaryContexts(Set<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, DiarySummaryView> diaries = queryDiaryReadUseCase.getDiarySummaries(diaryIds);
		Map<Long, String> photoPaths = queryDiaryReadUseCase.getRepresentPhotoPaths(diaryIds);
		return diaries.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> {
							Long diaryId = entry.getKey();
							DiarySummaryView diary = entry.getValue();
							String photoPath = photoPaths.get(diaryId);
							String thumbnailUrl = photoPath == null
									? null
									: resolveObjectUrlPort.resolveObjectUrl(
											photoPath,
											photoPath.startsWith("public/"));
							return new NotificationDiaryContextView(
									diaryId,
									thumbnailUrl,
									null,
									diary.userId(),
									diary.status() == DiaryVisibility.ANONYMOUS);
						}));
	}
}
