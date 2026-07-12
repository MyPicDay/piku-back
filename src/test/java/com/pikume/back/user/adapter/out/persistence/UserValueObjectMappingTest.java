package com.pikume.back.user.adapter.out.persistence;

import com.pikume.back.user.domain.User;
import com.pikume.back.global.pagination.PageQuery;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("User value object persistence mapping")
class UserValueObjectMappingTest {

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("값 객체 상태를 기존 users 문자열 컬럼에 저장하고 복원한다")
	void persistsValueObjectsUsingExistingColumns() {
		User saved = userJpaRepository.saveAndFlush(new User(
				"user@example.com",
				"password-hash",
				"pikume",
				"public/characters/fixed/base.webp"));
		entityManager.clear();

		User restored = userJpaRepository.findById(saved.getId()).orElseThrow();
		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT email, nickname, avatar FROM users WHERE id = ?",
				saved.getId());

		assertThat(restored.getEmail()).isEqualTo("user@example.com");
		assertThat(restored.getNickname()).isEqualTo("pikume");
		assertThat(restored.getAvatar()).isEqualTo("public/characters/fixed/base.webp");
		assertThat(row.get("email")).isEqualTo("user@example.com");
		assertThat(row.get("nickname")).isEqualTo("pikume");
		assertThat(row.get("avatar")).isEqualTo("public/characters/fixed/base.webp");
	}

	@Test
	@DisplayName("기존 문자열 Repository 계약으로 값 객체 컬럼을 조회한다")
	void queriesValueObjectColumnsUsingStringContracts() {
		userJpaRepository.saveAndFlush(new User(
				"user@example.com",
				"password-hash",
				"pikume-user",
				"public/characters/fixed/base.webp"));
		entityManager.clear();

		UserPersistenceAdapter adapter = new UserPersistenceAdapter(userJpaRepository);

		assertThat(adapter.findByEmail("user@example.com")).isPresent();
		assertThat(adapter.existsByEmail("user@example.com")).isTrue();
		assertThat(adapter.existsByNickname("pikume-user")).isTrue();
		assertThat(adapter.searchByName("%pikume%", PageQuery.of(0, 20)).getContent())
				.singleElement()
				.satisfies(user -> assertThat(user.getNickname()).isEqualTo("pikume-user"));
	}
}
