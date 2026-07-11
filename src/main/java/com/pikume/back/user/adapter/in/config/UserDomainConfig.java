package com.pikume.back.user.adapter.in.config;

import com.pikume.back.user.domain.service.NicknamePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfig {

	@Bean
	public NicknamePolicy nicknamePolicy() {
		return new NicknamePolicy();
	}
}
