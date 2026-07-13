package com.pikume.back.user.auth.application.port.out;

import java.util.List;

public interface LoadAllowedEmailDomainPort {

	boolean isAllowedEmailDomain(String domain);

	List<String> loadAllowedEmailDomains();
}
