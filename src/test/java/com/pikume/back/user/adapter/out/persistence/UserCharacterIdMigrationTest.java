package com.pikume.back.user.adapter.out.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@Tag("mysql-migration")
@DisplayName("V15 사용자 캐릭터 식별자 마이그레이션")
class UserCharacterIdMigrationTest {

	@Container
	private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("pikume")
			.withUsername("pikume")
			.withPassword("pikume");

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void migrateToVersion14() {
		dataSource = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
		jdbcTemplate = new JdbcTemplate(dataSource);
		Flyway flyway = Flyway.configure()
				.dataSource(dataSource)
				.cleanDisabled(false)
				.target(MigrationVersion.fromVersion("14"))
				.load();
		flyway.clean();
		flyway.migrate();
	}

	@Test
	@DisplayName("정확히 하나의 고정 캐릭터와 일치하면 식별자를 이전하고 필수 컬럼만 남긴다")
	void migratesExactFixedCharacterMatch() {
		long characterId = insertCharacter("public/characters/fixed/base_image_1.webp", "FIXED");
		insertUser("user-1", "public/characters/fixed/base_image_1.webp");

		migrateToLatest();

		Long migratedCharacterId = jdbcTemplate.queryForObject(
				"SELECT character_id FROM users WHERE id = 'user-1'", Long.class);
		assertThat(migratedCharacterId).isEqualTo(characterId);
		assertThat(columnCount("users", "avatar")).isZero();
		assertThat(nullableValue("users", "character_id")).isEqualTo("NO");
		assertThat(indexCount("users", "idx_users_character_id")).isEqualTo(1);
		assertThat(foreignKeyCount("users", "character_id")).isZero();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(InvalidFixture.class)
	@DisplayName("정확한 고정 캐릭터 단일 일치가 아니면 스키마 변경 전에 실패한다")
	void rejectsInvalidExistingData(InvalidFixture fixture) {
		fixture.seed(this);

		assertThatThrownBy(this::migrateToLatest)
				.isInstanceOf(FlywayException.class);
		assertThat(columnCount("users", "avatar")).isEqualTo(1);
		assertThat(columnCount("users", "character_id")).isZero();
	}

	private void migrateToLatest() {
		Flyway.configure().dataSource(dataSource).load().migrate();
	}

	private long insertCharacter(String imageReference, String type) {
		jdbcTemplate.update(
				"INSERT INTO characters (image_url, type) VALUES (?, ?)", imageReference, type);
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	private void insertUser(String userId, String avatar) {
		jdbcTemplate.update(
				"INSERT INTO users (id, email, nickname, avatar) VALUES (?, ?, ?, ?)",
				userId, userId + "@example.com", userId, avatar);
	}

	private int columnCount(String tableName, String columnName) {
		return jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.COLUMNS
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = ?
				  AND COLUMN_NAME = ?
				""", Integer.class, tableName, columnName);
	}

	private String nullableValue(String tableName, String columnName) {
		return jdbcTemplate.queryForObject("""
				SELECT IS_NULLABLE
				FROM information_schema.COLUMNS
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = ?
				  AND COLUMN_NAME = ?
				""", String.class, tableName, columnName);
	}

	private int indexCount(String tableName, String indexName) {
		return jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT INDEX_NAME)
				FROM information_schema.STATISTICS
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = ?
				  AND INDEX_NAME = ?
				""", Integer.class, tableName, indexName);
	}

	private int foreignKeyCount(String tableName, String columnName) {
		return jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.KEY_COLUMN_USAGE
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = ?
				  AND COLUMN_NAME = ?
				  AND REFERENCED_TABLE_NAME IS NOT NULL
				""", Integer.class, tableName, columnName);
	}

	private enum InvalidFixture {
		NULL_AVATAR {
			@Override
			void seed(UserCharacterIdMigrationTest test) {
				test.insertUser("user-null", null);
			}
		},
		BLANK_AVATAR {
			@Override
			void seed(UserCharacterIdMigrationTest test) {
				test.insertUser("user-blank", "   ");
			}
		},
		UNMATCHED_AVATAR {
			@Override
			void seed(UserCharacterIdMigrationTest test) {
				test.insertUser("user-unmatched", "missing.webp");
			}
		},
		CASE_MISMATCH {
			@Override
			void seed(UserCharacterIdMigrationTest test) {
				test.insertCharacter("Avatar.webp", "FIXED");
				test.insertUser("user-case", "avatar.webp");
			}
		},
		DUPLICATE_FIXED_MATCH {
			@Override
			void seed(UserCharacterIdMigrationTest test) {
				test.insertCharacter("duplicate.webp", "FIXED");
				test.insertCharacter("duplicate.webp", "FIXED");
				test.insertUser("user-duplicate", "duplicate.webp");
			}
		},
		AI_GENERATED_ONLY_MATCH {
			@Override
			void seed(UserCharacterIdMigrationTest test) {
				test.insertCharacter("generated.webp", "AI_GENERATED");
				test.insertUser("user-ai", "generated.webp");
			}
		};

		abstract void seed(UserCharacterIdMigrationTest test);
	}
}
