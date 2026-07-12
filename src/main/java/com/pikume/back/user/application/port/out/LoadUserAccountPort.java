package com.pikume.back.user.application.port.out;

import com.pikume.back.user.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadUserAccountPort {

	Optional<User> findById(String userId);

	Optional<User> findByEmail(String email);

	List<User> findAllByIds(Collection<String> userIds);
}
