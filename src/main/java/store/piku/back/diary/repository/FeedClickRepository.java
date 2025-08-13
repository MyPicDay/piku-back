package store.piku.back.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.piku.back.diary.entity.FeedClick;

import java.util.List;

public interface FeedClickRepository extends JpaRepository<FeedClick, Long> {
    boolean existsByUserIdAndDiaryId(String userId, Long diaryId);

    @Query("SELECT f.diaryId FROM FeedClick f WHERE f.userId = :userId")
    List<Long> findClickedDiaryIdsByUserId(@Param("userId") String userId);

}
