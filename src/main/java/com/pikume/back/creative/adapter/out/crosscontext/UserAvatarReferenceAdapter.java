package com.pikume.back.creative.adapter.out.crosscontext;

import com.pikume.back.creative.application.port.out.LoadUserAvatarReferencePort;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserAvatarReferenceAdapter implements LoadUserAvatarReferencePort {

	private final QueryUserSummaryUseCase queryUserSummaryUseCase;

	@Override
	public Optional<String> loadUserAvatarReference(String userId) {
		Map<String, UserSummaryView> usersById = queryUserSummaryUseCase.queryUserSummaries(Set.of(userId));
		UserSummaryView user = usersById != null ? usersById.get(userId) : null;
		if (user == null || user.avatarPath() == null || user.avatarPath().isBlank()) {
			return Optional.empty();
		}
		return Optional.of(user.avatarPath());
	}
}
