package com.pikume.back.user.adapter.out.persistence;

import com.pikume.back.user.application.port.out.SaveUserPort;
import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.exception.NicknameAlreadyExistsException;
import com.pikume.back.user.domain.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Locale;

/**
 * User Aggregate 저장 어댑터입니다.
 */
@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements SaveUserPort {
	private static final String EMAIL_UNIQUE_CONSTRAINT = "uk6dotkott2kjsp8vw4d0m25fb7";
	private static final String NICKNAME_UNIQUE_CONSTRAINT = "uk2ty1xmrrgtn89xt7kyxx6ta7h";

	private final UserJpaRepository jpaRepository;

	@Override
	public User save(User user) {
		try {
			return jpaRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			String constraintName = findConstraintName(exception);
			if (isNicknameConstraint(constraintName)) {
				throw new NicknameAlreadyExistsException(user.getNickname());
			}
			if (isEmailConstraint(constraintName)) {
				throw new EmailAlreadyExistsException();
			}
			throw exception;
		}
	}

	private String findConstraintName(Throwable throwable) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			if (current instanceof ConstraintViolationException constraintViolation) {
				return normalize(constraintViolation.getConstraintName());
			}
		}
		return "";
	}

	private boolean isNicknameConstraint(String constraintName) {
		return constraintName.contains(NICKNAME_UNIQUE_CONSTRAINT)
				|| constraintName.contains("users(nickname");
	}

	private boolean isEmailConstraint(String constraintName) {
		return constraintName.contains(EMAIL_UNIQUE_CONSTRAINT)
				|| constraintName.contains("users(email");
	}

	private String normalize(String constraintName) {
		if (constraintName == null) {
			return "";
		}
		return constraintName.toLowerCase(Locale.ROOT)
				.replace("`", "")
				.replace("\"", "")
				.replaceAll("\\s+", "");
	}
}
