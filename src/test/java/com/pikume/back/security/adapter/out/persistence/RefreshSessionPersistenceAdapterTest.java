package com.pikume.back.security.adapter.out.persistence;

import com.pikume.back.user.auth.application.port.out.RefreshSessionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("RefreshSession persistence")
class RefreshSessionPersistenceAdapterTest {

	@Autowired private RefreshTokenJpaRepository repository;
	@Autowired private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("기존 refresh_tokens 테이블과 refresh_key를 유지해 저장·조회·삭제한다")
	void preservesExistingTableAndKey() {
		RefreshTokenPersistenceAdapter adapter = new RefreshTokenPersistenceAdapter(repository);
		var session = new RefreshSessionPort.RefreshSession("user-1-device-1", "refresh", "user-1");

		adapter.save(session);
		repository.flush();

		assertThat(adapter.findByRefreshToken("refresh")).contains(session);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT refresh_key FROM refresh_tokens WHERE refresh_token = ?", String.class, "refresh"))
				.isEqualTo("user-1-device-1");
		adapter.deleteByKey("user-1-device-1");
		repository.flush();
		assertThat(repository.findById("user-1-device-1")).isEmpty();
	}
}
