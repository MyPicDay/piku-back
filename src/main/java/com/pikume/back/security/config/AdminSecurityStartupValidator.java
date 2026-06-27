package com.pikume.back.security.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AdminSecurityStartupValidator {

	private final AdminSecurityProperties properties;
	private final Environment environment;

	@PostConstruct
	void validate() {
		if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
			properties.validateProduction();
		}
	}
}
