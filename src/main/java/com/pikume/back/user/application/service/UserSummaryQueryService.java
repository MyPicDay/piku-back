package com.pikume.back.user.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;
import com.pikume.back.user.application.port.out.LoadUserAccountPort;
import com.pikume.back.user.domain.User;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSummaryQueryService implements QueryUserSummaryUseCase {

	private final LoadUserAccountPort loadUserAccountPort;

	@Override
	public Map<String, UserSummaryView> getUserSummaries(Set<String> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}

		return loadUserAccountPort.findAllByIds(userIds).stream()
				.collect(Collectors.toMap(
						User::getId,
						user -> new UserSummaryView(user.getId(), user.getNickname(), user.getAvatar()),
						(left, right) -> left));
	}
}
