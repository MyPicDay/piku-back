package com.pikume.back.user.auth.application.port.out;

import java.util.List;

public interface LoadAllowedEmailDomainPort {

	boolean existsByDomain(String domain);

	List<String> loadAllDomains();
}
