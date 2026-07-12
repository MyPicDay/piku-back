package com.pikume.back.user.auth.application.service;

import com.pikume.back.user.auth.application.port.in.QueryAllowedEmailUseCase;
import com.pikume.back.user.auth.application.port.out.LoadAllowedEmailDomainPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AllowedEmailQueryService implements QueryAllowedEmailUseCase {

	private final LoadAllowedEmailDomainPort loadAllowedEmailDomainPort;

	@Override
	public boolean isEmailAllowed(String email) {
		if (!StringUtils.hasText(email) || !email.contains("@")) {
			return false;
		}
		return loadAllowedEmailDomainPort.existsByDomain(email.substring(email.indexOf('@') + 1));
	}

	@Override
	public List<String> getAllowedEmailDomains() {
		return loadAllowedEmailDomainPort.loadAllDomains();
	}
}
