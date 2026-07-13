package com.pikume.back.security.adapter.out.password;

import com.pikume.back.user.auth.application.port.out.PasswordProtectionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordProtectionAdapter implements PasswordProtectionPort {

	private final PasswordEncoder passwordEncoder;

	@Override
	public String protect(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String protectedPassword) {
		return passwordEncoder.matches(rawPassword, protectedPassword);
	}
}
