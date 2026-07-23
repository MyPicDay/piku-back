package com.pikume.back.feed.adapter.out.crosscontext;

import com.pikume.back.feed.application.port.out.LoadFeedAuthorsPort;
import com.pikume.back.feed.application.readmodel.FeedAuthorView;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.user.application.dto.UserAvatarReference;
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
	private final ResolveObjectUrlPort resolveObjectUrlPort;

	@Override
	public Map<String, FeedAuthorView> loadAuthors(Set<String> userIds) {
		return queryUserSummaryUseCase.queryUserSummaries(userIds).entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> new FeedAuthorView(
								entry.getKey(),
								entry.getValue().nickname(),
								entry.getValue().avatarPath() != null
										? resolveAvatarUrl(entry.getValue().avatarPath())
										: null)));
	}

	private String resolveAvatarUrl(String storedPath) {
		UserAvatarReference reference = UserAvatarReference.fromStoredPath(storedPath);
		if (reference.isEmpty() || reference.absoluteUrl()) {
			return reference.value();
		}
		return resolveObjectUrlPort.resolveObjectUrl(reference.value(), reference.publiclyAccessible());
	}
}
