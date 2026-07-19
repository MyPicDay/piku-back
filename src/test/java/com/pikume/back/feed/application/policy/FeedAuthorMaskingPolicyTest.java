package com.pikume.back.feed.application.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.readmodel.FeedAuthorView;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeedAuthorMaskingPolicy")
class FeedAuthorMaskingPolicyTest {

	private final FeedAuthorMaskingPolicy policy = new FeedAuthorMaskingPolicy();

	@Nested
	@DisplayName("present")
	class Present {

		@Test
		@DisplayName("익명 일기는 작성자 정보를 숨기고 작성자 여부만 계산한다")
		void masksAnonymousAuthor() {
			FeedAuthorView author = new FeedAuthorView(
					"writer-id",
					"writer",
					"https://cdn.example/avatar.png");

			FeedAuthorMaskingPolicy.AuthorPresentation presentation = policy.present(
					FeedVisibility.ANONYMOUS,
					"writer-id",
					author,
					FeedFriendStatus.FRIENDS,
					"writer-id");

			assertThat(presentation.nickname()).isEqualTo("익명");
			assertThat(presentation.avatarUrl()).isNull();
			assertThat(presentation.userId()).isNull();
			assertThat(presentation.friendStatus()).isEqualTo(FeedFriendStatus.ANONYMOUS);
			assertThat(presentation.owner()).isTrue();
		}

		@Test
		@DisplayName("공개 일기는 작성자 정보와 친구 상태를 유지한다")
		void preservesVisibleAuthor() {
			FeedAuthorView author = new FeedAuthorView(
					"writer-id",
					"writer",
					"https://cdn.example/avatar.png");

			FeedAuthorMaskingPolicy.AuthorPresentation presentation = policy.present(
					FeedVisibility.PUBLIC,
					"writer-id",
					author,
					FeedFriendStatus.FRIENDS,
					"viewer-id");

			assertThat(presentation.nickname()).isEqualTo("writer");
			assertThat(presentation.avatarUrl()).isEqualTo("https://cdn.example/avatar.png");
			assertThat(presentation.userId()).isEqualTo("writer-id");
			assertThat(presentation.friendStatus()).isEqualTo(FeedFriendStatus.FRIENDS);
			assertThat(presentation.owner()).isFalse();
		}
	}
}
