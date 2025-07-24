package store.piku.back.diary.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.piku.back.diary.dto.DiaryMonthCountDTO;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.enums.Status;
import store.piku.back.user.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    List<Diary> findByUserIdAndDateBetween(String userId, LocalDate start, LocalDate end);

    Optional<Diary> findByUserAndDate(User user, LocalDate date);

    long countByUserId(String userId);


    @Query(value = "SELECT new store.piku.back.diary.dto.DiaryMonthCountDTO(YEAR(d.date), MONTH(d.date), COUNT(d.id)) " +
            "FROM Diary d " +
            "WHERE d.user.id = :userId " +
            "AND d.date >= :monthsAgo " +
            "GROUP BY YEAR(d.date), MONTH(d.date) " +
            "ORDER BY YEAR(d.date) DESC, MONTH(d.date) DESC")
    List<DiaryMonthCountDTO> countDiariesPerMonth(
            @Param("userId") String userId,
            @Param("monthsAgo") LocalDate monthsAgo
    );

    @Query("""
    SELECT d FROM Diary d
    WHERE (d.user.id IN :friendIds AND d.status = 'FRIENDS')
       OR d.status = 'PUBLIC'
    ORDER BY CASE WHEN (d.user.id IN :friendIds AND d.status = 'FRIENDS') THEN 0 ELSE 1 END, d.createdAt DESC
""")
    Page<Diary> findFeedByFriendIdsOrPublic(List<String> friendIds, Pageable safePageable);
}
