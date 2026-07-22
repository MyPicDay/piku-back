package com.pikume.back.social.adapter.out.crosscontext;

import com.pikume.back.social.application.event.SocialNotificationEvent;
import com.pikume.back.social.application.port.out.PublishSocialNotificationEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringSocialNotificationEventPublisher implements PublishSocialNotificationEventPort {

	private final ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void publish(SocialNotificationEvent event) {
		applicationEventPublisher.publishEvent(event);
	}
}
