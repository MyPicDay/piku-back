package com.pikume.back.notification.adapter.out.crosscontext;

import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.notification.application.port.out.LoadNotificationSendersPort;
import com.pikume.back.notification.application.readmodel.NotificationSenderView;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserAdapterForNotification implements LoadNotificationSendersPort {

	private final QueryUserSummaryUseCase queryUserSummaryUseCase;
	private final ResolveObjectUrlPort resolveObjectUrlPort;

	@Override
	public Map<String, NotificationSenderView> loadNotificationSenders(Set<String> senderIds) {
		if (senderIds == null || senderIds.isEmpty()) {
			return Map.of();
		}
		return queryUserSummaryUseCase.queryUserSummaries(senderIds).entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> {
							String avatarPath = entry.getValue().avatarPath();
							return new NotificationSenderView(
									entry.getKey(),
									entry.getValue().nickname(),
									avatarPath == null
											? null
											: resolveAvatarUrl(avatarPath));
						}));
	}

	private String resolveAvatarUrl(String storedPath) {
		UserAvatarReference reference = UserAvatarReference.fromStoredPath(storedPath);
		if (reference.isEmpty() || reference.absoluteUrl()) {
			return reference.value();
		}
		return resolveObjectUrlPort.resolveObjectUrl(reference.value(), reference.publiclyAccessible());
	}
}
