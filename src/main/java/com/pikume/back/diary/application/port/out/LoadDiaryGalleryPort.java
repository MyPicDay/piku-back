package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.application.dto.DiaryGalleryRow;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LoadDiaryGalleryPort {

	List<DiaryGalleryRow> findGalleryRows(
			String ownerId,
			Collection<DiaryVisibility> statuses,
			LocalDate cursorDate,
			Long cursorDiaryId,
			int limit);
}
