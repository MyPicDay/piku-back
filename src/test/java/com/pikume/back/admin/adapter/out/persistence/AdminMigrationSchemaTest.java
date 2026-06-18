package com.pikume.back.admin.adapter.out.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("관리자 스키마 마이그레이션")
class AdminMigrationSchemaTest {

	private static final String MIGRATION_PATH = "db/migration/V13__add_admin_accounts.sql";
	private static final String MYSQL_TABLE_OPTIONS =
			") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;";

	private String migrationSql;

	@BeforeEach
	void setUp() throws IOException {
		ClassPathResource migration = new ClassPathResource(MIGRATION_PATH);
		migrationSql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("관리자 계정과 등급의 UUID 컬럼은 JPA 매핑과 동일한 VARCHAR(36)이다")
	void usesVarcharForAdminAccountIdentifiers() {
		assertThat(tableDefinition("admins"))
				.contains("id VARCHAR(36) NOT NULL");
		assertThat(tableDefinition("admin_roles"))
				.contains("admin_id VARCHAR(36) NOT NULL");
	}

	@Test
	@DisplayName("모든 관리자 테이블은 동일한 MySQL 테이블 옵션을 사용한다")
	void usesConsistentMySqlTableOptions() {
		assertThat(tableDefinition("admins")).endsWith(MYSQL_TABLE_OPTIONS);
		assertThat(tableDefinition("admin_roles")).endsWith(MYSQL_TABLE_OPTIONS);
	}

	@Test
	@DisplayName("관리자 계정은 세션 무효화를 위한 인증 버전을 저장한다")
	void storesAdminAuthenticationVersion() {
		assertThat(tableDefinition("admins"))
				.contains("authentication_version BIGINT NOT NULL DEFAULT 0");
	}

	@Test
	@DisplayName("관리자 세션은 쿠키와 CSRF 원문 대신 검증용 해시와 인증 단계를 저장한다")
	void storesServerSessionState() {
		assertThat(tableDefinition("admin_sessions"))
				.contains(
						"admin_id varchar(36) DEFAULT NULL",
						"session_token_hash varchar(64) NOT NULL",
						"csrf_token_hash varchar(64) NOT NULL",
						"phase varchar(40) NOT NULL",
						"authentication_version bigint NOT NULL",
						"last_activity_at datetime(6) NOT NULL")
				.doesNotContain(
						"current_refresh_token_hash",
						"last_rotated_at",
						"reuse_detected_at");
	}

	@Test
	@DisplayName("관리자마다 하나의 인증 완료 세션만 저장할 수 있다")
	void enforcesSingleActiveSessionPerAdmin() {
		assertThat(tableDefinition("admin_sessions"))
				.contains(
						"active_admin_id varchar(36) GENERATED ALWAYS AS",
						"CASE WHEN status = 'ACTIVE' AND phase = 'AUTHENTICATED' THEN admin_id ELSE NULL END",
						"UNIQUE KEY uk_admin_sessions_active_admin_id (active_admin_id)");
	}

	@Test
	@DisplayName("관리자 리프레시 토큰 테이블을 생성하지 않는다")
	void doesNotCreateAdminRefreshTokens() {
		assertThat(migrationSql).doesNotContain("CREATE TABLE admin_refresh_tokens");
	}

	private String tableDefinition(String tableName) {
		String createTable = "CREATE TABLE " + tableName + " (";
		int start = migrationSql.indexOf(createTable);
		assertThat(start).as("%s 테이블 정의 시작 위치", tableName).isGreaterThanOrEqualTo(0);

		int end = migrationSql.indexOf(';', start);
		assertThat(end).as("%s 테이블 정의 종료 위치", tableName).isGreaterThan(start);
		return migrationSql.substring(start, end + 1);
	}
}
