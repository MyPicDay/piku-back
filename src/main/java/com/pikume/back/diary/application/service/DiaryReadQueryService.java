package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.application.dto.DiaryPhotoView;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.dto.DiaryVisibilityScope;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.application.port.out.LoadDiaryReadPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryReadQueryService implements QueryDiaryReadUseCase {

	private final LoadDiaryReadPort loadDiaryPort;

	@Override
	public Map<Long, DiarySummaryView> getDiarySummaries(Set<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return Map.of();
		}
		return loadDiaryPort.findActiveByIds(diaryIds).stream().collect(Collectors.toMap(
				Diary::getId,
				diary -> new DiarySummaryView(
						diary.getId(),
						diary.getUserId(),
						toVisibilityScope(diary.getStatus()),
						diary.getContent(),
						diary.getDate(),
						diary.getCreatedAt()),
				(left, right) -> left));
	}

	@Override
	public List<DiaryPhotoView> getDiaryPhotos(Set<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return List.of();
		}
		return loadDiaryPort.findPhotoRowsByDiaryIds(diaryIds).stream()
				.map(row -> new DiaryPhotoView(row.diaryId(), row.displayObjectKey(), row.represent()))
				.toList();
	}

	@Override
	public Map<Long, String> getRepresentPhotoPaths(Set<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return Map.of();
		}
		return loadDiaryPort.findPhotoRowsByDiaryIds(diaryIds).stream()
				.filter(DiaryPhotoRow::represent)
				.collect(Collectors.toMap(DiaryPhotoRow::diaryId, DiaryPhotoRow::displayObjectKey, (left, right) -> left));
	}

	private DiaryVisibilityScope toVisibilityScope(DiaryVisibility visibility) {
		return switch (visibility) {
			case PUBLIC -> DiaryVisibilityScope.PUBLIC;
			case FRIENDS -> DiaryVisibilityScope.FRIENDS;
			case PRIVATE -> DiaryVisibilityScope.PRIVATE;
			case ANONYMOUS -> DiaryVisibilityScope.ANONYMOUS;
		};
	}
}
