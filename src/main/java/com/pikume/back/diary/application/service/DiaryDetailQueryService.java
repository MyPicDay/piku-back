package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.VisibleDiaryView;
import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.port.in.QueryDiaryVisibilityUseCase;
import com.pikume.back.diary.application.port.out.LoadDiaryDetailPort;
import com.pikume.back.diary.domain.Diary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryDetailQueryService implements QueryDiaryVisibilityUseCase {

	private final LoadDiaryDetailPort loadDiaryPort;
	private final DiaryVisibilityPolicy visibilityPolicy;

	@Override
	public Optional<VisibleDiaryView> findVisibleDiaryById(Long diaryId, String viewerId) {
		return loadDiaryPort.findActiveById(diaryId)
				.filter(diary -> !visibilityPolicy.isHiddenFromViewer(diary, viewerId))
				.map(this::toView);
	}

	@Override
	public boolean existsVisibleById(Long diaryId, String viewerId) {
		return findVisibleDiaryById(diaryId, viewerId).isPresent();
	}

	@Override
	public Optional<String> findVisibleOwnerUserIdByDiaryId(Long diaryId, String viewerId) {
		return findVisibleDiaryById(diaryId, viewerId).map(VisibleDiaryView::userId);
	}

	private VisibleDiaryView toView(Diary diary) {
		return new VisibleDiaryView(
				diary.getId(),
				diary.getUserId(),
				diary.getStatus(),
				diary.getContent(),
				diary.getDate(),
				diary.getCreatedAt());
	}
}
