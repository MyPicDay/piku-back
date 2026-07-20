package com.pikume.back.notification.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.notification.application.port.out.LoadNotificationSendersPort;
import com.pikume.back.notification.application.readmodel.NotificationSenderView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserAdapterForNotification implements LoadNotificationSendersPort {

	private final QueryUserSummaryUseCase queryUserSummaryUseCase;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

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
											: imagePathToUrlConverter.userAvatarImageUrl(avatarPath));
						}));
	}
}
