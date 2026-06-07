package com.pikume.back.social.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.in.QueryDiaryVisibilityUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.social.application.port.out.LoadDiaryInfoPort;

import java.util.Objects;
import java.util.Optional;

/**
 * Diary Context의 DiaryRepository를 래핑하여
 * Social Context에서 일기 정보를 조회하는 cross-context 어댑터.
 */
@Component
@RequiredArgsConstructor
public class DiaryAdapterForSocial implements LoadDiaryInfoPort {

	private final QueryDiaryVisibilityUseCase queryDiaryVisibilityUseCase;

	@Override
	public boolean existsVisibleById(Long diaryId, String viewerId) {
		return queryDiaryVisibilityUseCase.existsVisibleById(diaryId, viewerId);
	}

	@Override
	public Optional<String> findVisibleOwnerUserIdByDiaryId(Long diaryId, String viewerId) {
		return queryDiaryVisibilityUseCase.findVisibleOwnerUserIdByDiaryId(diaryId, viewerId);
	}

	@Override
	public Optional<DiaryInfo> findVisibleDiaryInfoByDiaryId(Long diaryId, String viewerId) {
		return queryDiaryVisibilityUseCase.findVisibleDiaryById(diaryId, viewerId)
				.map(diary -> new DiaryInfo(
						diary.diaryId(),
						diary.userId(),
						diary.status() == DiaryVisibility.ANONYMOUS,
						Objects.equals(diary.userId(), viewerId)));
	}
}
