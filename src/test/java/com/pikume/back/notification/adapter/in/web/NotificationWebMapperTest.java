package com.pikume.back.notification.adapter.in.web;

import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.adapter.in.web.dto.NotificationPageResponse;
import com.pikume.back.notification.application.dto.NotificationKind;
import com.pikume.back.notification.application.dto.NotificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationWebMapper")
class NotificationWebMapperTest {

	private final NotificationWebMapper mapper = new NotificationWebMapper();

	@Test
	@DisplayName("Application Page를 기존 Spring Page JSON과 같은 Web 응답으로 변환한다")
	void mapsNotificationPageMetadata() {
		NotificationResult item = new NotificationResult(
				1L,
				"님이 일기에 댓글을 달았습니다.",
				"보낸이",
				null,
				NotificationKind.COMMENT,
				10L,
				null,
				false,
				null,
				null,
				"owner-id");
		PageRequest pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

		NotificationPageResponse response = mapper.toPageResponse(
				new PageResult<>(List.of(item), 1, 2, 5),
				pageable);

		assertThat(response.content()).singleElement().satisfies(content -> {
			assertThat(content.id()).isEqualTo(1L);
			assertThat(content.type()).isEqualTo(NotificationKind.COMMENT);
			assertThat(content.avatarUrl()).isNull();
		});
		assertThat(response.number()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(2);
		assertThat(response.totalElements()).isEqualTo(5);
		assertThat(response.totalPages()).isEqualTo(3);
		assertThat(response.numberOfElements()).isEqualTo(1);
		assertThat(response.first()).isFalse();
		assertThat(response.last()).isFalse();
		assertThat(response.empty()).isFalse();
		assertThat(response.sort().sorted()).isTrue();
		assertThat(response.sort().unsorted()).isFalse();
		assertThat(response.sort().empty()).isFalse();
		assertThat(response.pageable().pageNumber()).isEqualTo(1);
		assertThat(response.pageable().pageSize()).isEqualTo(2);
		assertThat(response.pageable().offset()).isEqualTo(2);
		assertThat(response.pageable().paged()).isTrue();
		assertThat(response.pageable().unpaged()).isFalse();
		assertThat(response.pageable().sort()).isEqualTo(response.sort());
	}
}
