package com.pikume.back.user.auth.adapter.out.persistence;

import com.pikume.back.user.auth.application.port.out.LoadAllowedEmailDomainPort;
import com.pikume.back.user.auth.domain.AllowedEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AllowedEmailDomainPersistenceAdapter implements LoadAllowedEmailDomainPort {

	private final AllowedEmailDomainJpaRepository repository;

	@Override
	public boolean isAllowedEmailDomain(String domain) {
		return repository.existsByDomain(domain);
	}

	@Override
	public List<String> loadAllowedEmailDomains() {
		return repository.findAll().stream().map(AllowedEmail::getDomain).toList();
	}
}
