package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.DiaryPhotoView;
import com.pikume.back.diary.application.dto.DiarySummaryView;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QueryDiaryReadUseCase {

	Map<Long, DiarySummaryView> getDiarySummaries(Set<Long> diaryIds);

	List<DiaryPhotoView> getDiaryPhotos(Set<Long> diaryIds);

	Map<Long, String> getRepresentPhotoPaths(Set<Long> diaryIds);
}
