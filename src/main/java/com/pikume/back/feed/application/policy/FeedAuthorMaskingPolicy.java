package com.pikume.back.feed.application.policy;

import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.readmodel.FeedAuthorView;

import java.util.Objects;

@Component
public class FeedAuthorMaskingPolicy {

	private static final String ANONYMOUS_NICKNAME = "익명";
	private static final String UNKNOWN_NICKNAME = "알 수 없음";

	public AuthorPresentation present(
			FeedVisibility visibility,
			String authorId,
			FeedAuthorView author,
			FeedFriendStatus friendStatus,
			String currentUserId
	) {
		boolean owner = Objects.equals(authorId, currentUserId);
		if (visibility == FeedVisibility.ANONYMOUS) {
			return new AuthorPresentation(
					ANONYMOUS_NICKNAME,
					null,
					null,
					FeedFriendStatus.ANONYMOUS,
					owner);
		}

		return new AuthorPresentation(
				author != null ? author.nickname() : UNKNOWN_NICKNAME,
				author != null ? author.avatarUrl() : null,
				authorId,
				friendStatus,
				owner);
	}

	public record AuthorPresentation(
			String nickname,
			String avatarUrl,
			String userId,
			FeedFriendStatus friendStatus,
			boolean owner
	) {
	}
}
