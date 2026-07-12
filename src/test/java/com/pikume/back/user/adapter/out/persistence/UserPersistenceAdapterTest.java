package com.pikume.back.user.adapter.out.persistence;

import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.exception.NicknameAlreadyExistsException;
import com.pikume.back.user.domain.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
		given(userJpaRepository.saveAndFlush(user)).willThrow(
				new DataIntegrityViolationException("users.nickname unique constraint"));

		assertThatThrownBy(() -> new UserPersistenceAdapter(userJpaRepository).save(user))
				.isInstanceOf(NicknameAlreadyExistsException.class)
				.hasMessageContaining("duplicate-nickname");
	}

	@Test
	@DisplayName("email 유일 제약 위반을 계정 충돌 의미로 번역한다")
	void translatesEmailConstraintViolation() {
		User user = new User("user@example.com", "password", "nickname");
		DataIntegrityViolationException failure = new DataIntegrityViolationException(
				"users.email unique constraint");
		given(userJpaRepository.saveAndFlush(user)).willThrow(failure);

		assertThatThrownBy(() -> new UserPersistenceAdapter(userJpaRepository).save(user))
				.isInstanceOf(EmailAlreadyExistsException.class);
	}
}
