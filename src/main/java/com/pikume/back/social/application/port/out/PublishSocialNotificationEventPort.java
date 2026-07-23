package com.pikume.back.social.application.port.out;

import com.pikume.back.social.application.event.SocialNotificationEvent;

public interface PublishSocialNotificationEventPort {
	void publish(SocialNotificationEvent event);
}
