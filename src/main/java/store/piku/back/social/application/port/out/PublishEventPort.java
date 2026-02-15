package store.piku.back.social.application.port.out;

/**
 * 도메인 이벤트를 발행하는 포트.
 * ApplicationEventPublisher를 래핑합니다.
 */
public interface PublishEventPort {

	void publish(Object event);
}
