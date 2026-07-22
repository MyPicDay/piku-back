package com.pikume.back.social.adapter.out.crosscontext;

import com.pikume.back.diary.application.port.in.QueryVisibleDiaryReferenceUseCase;
import com.pikume.back.social.application.port.out.ResolveInteractionDiaryPort;
import com.pikume.back.social.application.readmodel.InteractionDiaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DiaryAdapterForSocial implements ResolveInteractionDiaryPort {

	private final QueryVisibleDiaryReferenceUseCase queryVisibleDiaryReferenceUseCase;

	@Override
	public Optional<InteractionDiaryView> resolveVisibleDiary(Long diaryId, String viewerId) {
		return queryVisibleDiaryReferenceUseCase.queryVisibleDiaryReference(diaryId, viewerId)
				.map(diary -> new InteractionDiaryView(
						diary.diaryId(),
						diary.ownerUserId(),
						diary.anonymous(),
						Objects.equals(diary.ownerUserId(), viewerId)));
	}
}
