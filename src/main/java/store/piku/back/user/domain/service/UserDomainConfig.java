package store.piku.back.user.domain.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * User 도메인 서비스 Bean 설정
 */
@Configuration
public class UserDomainConfig {

	@Bean
	public NicknamePolicy nicknamePolicy() {
		return new NicknamePolicy();
	}
}
