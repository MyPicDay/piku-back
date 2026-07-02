package com.pikume.back.global.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseEntity")
class BaseEntityTest {

	@Test
	@DisplayName("공통 엔티티는 생성·수정 감사 시각만 소유한다")
	void ownsOnlyAuditTimestamps() {
		assertThat(Arrays.stream(BaseEntity.class.getDeclaredFields()).map(Field::getName))
				.containsExactlyInAnyOrder("createdAt", "updatedAt");
		assertThat(Arrays.stream(BaseEntity.class.getDeclaredMethods()).map(Method::getName))
				.doesNotContain("inactive", "restore", "getDeletedAt");
	}
}
