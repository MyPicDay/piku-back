package com.pikume.back.admin.application.port.out;

import java.util.Optional;

public interface AdminSessionCachePort {

	Optional<AdminSessionCacheEntry> findByTokenHash(String sessionTokenHash);

	void put(AdminSessionCacheEntry entry);

	void evict(String sessionTokenHash);
}
