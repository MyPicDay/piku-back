package com.pikume.back.social.adapter.out.crosscontext;

import com.pikume.back.social.application.event.SocialNotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringSocialNotificationEventPublisherTest {

	@InjectMocks private SpringSocialNotificationEventPublisher publisher;
	@Mock private ApplicationEventPublisher applicationEventPublisher;

	@Test
	void publishesOnlyTypedSocialNotificationEvent() {
		SocialNotificationEvent event = new SocialNotificationEvent.FriendRequest("receiver", "sender");

		publisher.publish(event);

		verify(applicationEventPublisher).publishEvent(event);
	}
}
