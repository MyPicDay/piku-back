package com.pikume.back.feed.adapter.out.crosscontext;

import com.pikume.back.feed.application.port.out.LoadFeedAuthorsPort;
import com.pikume.back.feed.application.readmodel.FeedAuthorView;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserAdapterForFeed implements LoadFeedAuthorsPort {

	private final QueryUserSummaryUseCase queryUserSummaryUseCase;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public Map<String, FeedAuthorView> loadAuthors(Set<String> userIds) {
		return queryUserSummaryUseCase.queryUserSummaries(userIds).entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> new FeedAuthorView(
								entry.getKey(),
								entry.getValue().nickname(),
								entry.getValue().avatarPath() != null
										? imagePathToUrlConverter.userAvatarImageUrl(entry.getValue().avatarPath())
										: null)));
	}
}
