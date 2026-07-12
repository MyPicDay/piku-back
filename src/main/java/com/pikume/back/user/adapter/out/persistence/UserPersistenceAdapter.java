package com.pikume.back.user.adapter.out.persistence;

import com.pikume.back.user.application.port.out.SaveUserPort;
import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.exception.NicknameAlreadyExistsException;
import com.pikume.back.user.domain.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/**
 * User Aggregate 저장 어댑터입니다.
 */
@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements SaveUserPort {

	private final UserJpaRepository jpaRepository;

	@Override
	public User save(User user) {
		try {
			return jpaRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			if (containsNicknameConstraint(exception)) {
				throw new NicknameAlreadyExistsException(user.getNickname());
			}
			if (containsConstraint(exception, "email")) {
				throw new EmailAlreadyExistsException();
			}
			throw exception;
		}
	}

	private boolean containsNicknameConstraint(Throwable throwable) {
		return containsConstraint(throwable, "nickname");
	}

	private boolean containsConstraint(Throwable throwable, String columnName) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			if (current.getMessage() != null && current.getMessage().toLowerCase().contains(columnName)) {
				return true;
			}
		}
		return false;
	}
}
