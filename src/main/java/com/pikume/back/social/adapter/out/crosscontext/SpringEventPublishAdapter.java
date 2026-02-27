package com.pikume.back.social.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.pikume.back.social.application.port.out.PublishEventPort;

/**
 * Spring의 ApplicationEventPublisher를 래핑하여
 * 도메인 이벤트를 발행하는 어댑터.
 */
@Component
@RequiredArgsConstructor
public class SpringEventPublishAdapter implements PublishEventPort {

	private final ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void publish(Object event) {
		applicationEventPublisher.publishEvent(event);
	}
}
