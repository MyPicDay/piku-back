package com.pikume.back.user.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pikume.back.user.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User JPA Repository (Spring Data JPA 인터페이스)
 */
public interface UserJpaRepository extends JpaRepository<User, String> {

	interface DailyCountProjection {
		Object getMetricDate();

		Long getMetricCount();
	}

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	long countByDeletedAtIsNull();

	@Query(value = """
			SELECT CAST(created_at AS DATE) AS metricDate, COUNT(*) AS metricCount
			FROM users
			WHERE created_at >= :startDateTime
			  AND created_at < :endExclusiveDateTime
			  AND deleted_at IS NULL
			GROUP BY CAST(created_at AS DATE)
			""", nativeQuery = true)
	List<DailyCountProjection> countSignupMembersByDate(
			@Param("startDateTime") LocalDateTime startDateTime,
			@Param("endExclusiveDateTime") LocalDateTime endExclusiveDateTime);

	@Query("SELECT u FROM User u WHERE u.nickname LIKE :keyword")
	Page<User> searchByName(@Param("keyword") String keyword, Pageable pageable);

	boolean existsByNickname(String nickname);
}
