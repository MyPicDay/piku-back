package com.pikume.back.admin.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdminId")
class AdminIdTest {

	@Test
	@DisplayName("UUID v7 형식의 관리자 내부 식별자를 생성한다")
	void createsUuidV7() {
		UUID uuid = UUID.fromString(AdminId.newId());

		assertThat(uuid.version()).isEqualTo(7);
		assertThat(uuid.variant()).isEqualTo(2);
	}
}
