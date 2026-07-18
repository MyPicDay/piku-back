package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.DiaryGalleryRow;
import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.port.out.LoadDiaryGalleryPort;
import com.pikume.back.diary.application.port.out.ResolveDiaryPhotoUrlPort;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryGalleryQueryService")
class DiaryGalleryQueryServiceTest {

	@InjectMocks private DiaryGalleryQueryService service;
	@Mock private LoadDiaryGalleryPort loadDiaryPort;
	@Mock private ResolveDiaryPhotoUrlPort photoUrlPort;
	@Mock private DiaryVisibilityPolicy visibilityPolicy;
	@Mock private DiaryGalleryCursorTokenCodec cursorCodec;

	@Test
	@DisplayName("limit보다 하나 더 조회해 다음 cursor를 만든다")
	void createsNextCursor() {
		given(visibilityPolicy.visibleStatusesForOwner("owner", "viewer"))
				.willReturn(List.of(DiaryVisibility.PUBLIC));
		given(loadDiaryPort.findGalleryRows("owner", Set.of(DiaryVisibility.PUBLIC), null, null, 2))
				.willReturn(List.of(
						new DiaryGalleryRow(2L, "cover-2.jpg", LocalDate.of(2026, 7, 2), 1L, DiaryVisibility.PUBLIC),
						new DiaryGalleryRow(1L, "cover-1.jpg", LocalDate.of(2026, 7, 1), 1L, DiaryVisibility.PUBLIC)));
		given(photoUrlPort.resolve("cover-2.jpg")).willReturn("https://images/cover-2.jpg");
		given(cursorCodec.encode(any())).willReturn("next");

		var page = service.findGallery("owner", "viewer", null, 1);

		assertThat(page.items()).hasSize(1);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextCursor()).isEqualTo("next");
	}
}
