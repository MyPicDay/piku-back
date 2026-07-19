package com.pikume.back.feed.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.pikume.back.feed.application.dto.FeedSortMode;
import com.pikume.back.feed.application.exception.InvalidFeedSortException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FeedSortRequestMapper")
class FeedSortRequestMapperTest {

	private final FeedSortRequestMapper mapper = new FeedSortRequestMapper();

	@Nested
	@DisplayName("map")
	class Map {

		@Test
		@DisplayName("sort가 없거나 공백이면 추천순으로 변환한다")
		void mapsMissingSortToRecommended() {
			assertThat(mapper.map(null)).isEqualTo(FeedSortMode.RECOMMENDED);
			assertThat(mapper.map("  ")).isEqualTo(FeedSortMode.RECOMMENDED);
		}

		@Test
		@DisplayName("HTTP 정렬 문자열을 Feed 정렬 모드로 변환한다")
		void mapsHttpSortValue() {
			assertThat(mapper.map(" recommended ")).isEqualTo(FeedSortMode.RECOMMENDED);
			assertThat(mapper.map("LATEST")).isEqualTo(FeedSortMode.LATEST);
		}

		@Test
		@DisplayName("알 수 없는 정렬 문자열은 거부한다")
		void rejectsUnknownSortValue() {
			assertThatThrownBy(() -> mapper.map("unknown"))
					.isInstanceOf(InvalidFeedSortException.class);
		}
	}
}
