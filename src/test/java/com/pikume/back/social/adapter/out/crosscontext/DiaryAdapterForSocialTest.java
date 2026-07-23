package com.pikume.back.social.adapter.out.crosscontext;

import com.pikume.back.diary.application.dto.VisibleDiaryReferenceView;
import com.pikume.back.diary.application.port.in.QueryVisibleDiaryReferenceUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DiaryAdapterForSocialTest {

	@InjectMocks private DiaryAdapterForSocial adapter;
	@Mock private QueryVisibleDiaryReferenceUseCase queryVisibleDiaryReferenceUseCase;

	@Test
	void translatesDiaryReferenceWithoutDiaryDomainType() {
		given(queryVisibleDiaryReferenceUseCase.queryVisibleDiaryReference(1L, "owner"))
				.willReturn(Optional.of(new VisibleDiaryReferenceView(1L, "owner", true)));

		var result = adapter.resolveVisibleDiary(1L, "owner").orElseThrow();

		assertThat(result.anonymous()).isTrue();
		assertThat(result.viewerOwner()).isTrue();
	}
}
