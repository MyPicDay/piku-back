package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.DiaryGalleryCursor;
import com.pikume.back.diary.application.dto.DiaryGalleryItemView;
import com.pikume.back.diary.application.dto.DiaryGalleryPage;
import com.pikume.back.diary.application.dto.DiaryGalleryRow;
import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.port.in.GetDiaryGalleryUseCase;
import com.pikume.back.diary.application.port.out.LoadDiaryGalleryPort;
import com.pikume.back.diary.application.port.out.ResolveDiaryPhotoUrlPort;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryGalleryQueryService implements GetDiaryGalleryUseCase {

	private final LoadDiaryGalleryPort loadDiaryPort;
	private final ResolveDiaryPhotoUrlPort photoUrlPort;
	private final DiaryVisibilityPolicy visibilityPolicy;
	private final DiaryGalleryCursorTokenCodec cursorCodec;

	@Override
	public DiaryGalleryPage<DiaryGalleryItemView> findGallery(
			String userId,
			String viewerId,
			String cursorToken,
			int limit) {
		DiaryGalleryCursor cursor = cursorCodec.decode(cursorToken);
		Set<DiaryVisibility> statuses = Set.copyOf(visibilityPolicy.visibleStatusesForOwner(userId, viewerId));
		List<DiaryGalleryRow> rows = loadDiaryPort.findGalleryRows(
				userId,
				statuses,
				cursor == null ? null : cursor.date(),
				cursor == null ? null : cursor.diaryId(),
				limit + 1);
		boolean hasNext = rows.size() > limit;
		List<DiaryGalleryRow> pageRows = hasNext ? rows.subList(0, limit) : rows;
		List<DiaryGalleryItemView> items = pageRows.stream().map(this::toItem).toList();
		String nextCursor = hasNext && !pageRows.isEmpty()
				? cursorCodec.encode(toCursor(pageRows.get(pageRows.size() - 1)))
				: null;
		return new DiaryGalleryPage<>(items, nextCursor, hasNext);
	}

	private DiaryGalleryItemView toItem(DiaryGalleryRow row) {
		return new DiaryGalleryItemView(
				row.diaryId(),
				photoUrlPort.resolve(row.coverPhotoPath()),
				row.date(),
				row.imageCount(),
				row.status());
	}

	private DiaryGalleryCursor toCursor(DiaryGalleryRow row) {
		return new DiaryGalleryCursor(row.date(), row.diaryId());
	}
}
