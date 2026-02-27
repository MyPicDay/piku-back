package com.pikume.back.user.auth.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pikume.back.user.auth.domain.AllowedEmail;

public interface AllowedEmailDomainJpaRepository extends JpaRepository<AllowedEmail, Long> {

	boolean existsByDomain(String domain);
}
