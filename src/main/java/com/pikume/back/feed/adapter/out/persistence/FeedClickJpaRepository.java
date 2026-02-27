package com.pikume.back.feed.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pikume.back.feed.domain.FeedClick;

import java.util.List;

public interface FeedClickJpaRepository extends JpaRepository<FeedClick, Long> {
	boolean existsByUserIdAndDiaryId(String userId, Long diaryId);

	@Query("SELECT f.diaryId FROM FeedClick f WHERE f.userId = :userId")
	List<Long> findClickedDiaryIdsByUserId(@Param("userId") String userId);
}
