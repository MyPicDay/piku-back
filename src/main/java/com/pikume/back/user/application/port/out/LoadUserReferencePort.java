package com.pikume.back.user.application.port.out;

import com.pikume.back.user.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadUserReferencePort {

	Optional<User> loadReference(String userId);

	List<User> loadReferences(Collection<String> userIds);
}
