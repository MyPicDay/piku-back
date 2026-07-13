package com.pikume.back.user.adapter.out.persistence;

import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.exception.NicknameAlreadyExistsException;
import com.pikume.back.user.domain.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPersistenceAdapter")
class UserPersistenceAdapterTest {

	@Mock
	private UserJpaRepository userJpaRepository;

	@Test
	@DisplayName("nickname 유일 제약 위반을 도메인 충돌 의미로 번역한다")
	void translatesNicknameConstraintViolation() {
		User user = new User("user@example.com", "password", "duplicate-nickname");
		given(userJpaRepository.saveAndFlush(user)).willThrow(uniqueConstraintFailure(
				"UK2ty1xmrrgtn89xt7kyxx6ta7h"));

		assertThatThrownBy(() -> new UserPersistenceAdapter(userJpaRepository).save(user))
				.isInstanceOf(NicknameAlreadyExistsException.class)
				.hasMessageContaining("duplicate-nickname");
	}

	@Test
	@DisplayName("email 유일 제약 위반을 계정 충돌 의미로 번역한다")
	void translatesEmailConstraintViolation() {
		User user = new User("user@example.com", "password", "nickname");
		DataIntegrityViolationException failure = uniqueConstraintFailure(
				"UK6dotkott2kjsp8vw4d0m25fb7");
		given(userJpaRepository.saveAndFlush(user)).willThrow(failure);

		assertThatThrownBy(() -> new UserPersistenceAdapter(userJpaRepository).save(user))
				.isInstanceOf(EmailAlreadyExistsException.class);
	}

	@Test
	@DisplayName("알 수 없는 유일 제약 위반은 저장 기술 예외를 임의로 번역하지 않는다")
	void preservesUnknownConstraintViolation() {
		User user = new User("user@example.com", "password", "nickname");
		DataIntegrityViolationException failure = uniqueConstraintFailure("uk_users_unknown");
		given(userJpaRepository.saveAndFlush(user)).willThrow(failure);

		assertThatThrownBy(() -> new UserPersistenceAdapter(userJpaRepository).save(user))
				.isSameAs(failure);
	}

	private DataIntegrityViolationException uniqueConstraintFailure(String constraintName) {
		SQLException sqlException = new SQLException("duplicate key", "23505");
		ConstraintViolationException constraintViolation = new ConstraintViolationException(
				"could not execute statement",
				sqlException,
				"insert into users (email, nickname) values (?, ?)",
				constraintName);
		return new DataIntegrityViolationException(
				"could not execute insert into users (email, nickname)",
				constraintViolation);
	}
}
