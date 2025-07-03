package store.piku.back.diary.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.enums.Status;
import store.piku.back.user.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    List<Diary> findByUserIdAndDateBetween(String userId, LocalDate start, LocalDate end);

    Page<Diary> findByStatus(Status status, Pageable pageable);

    Optional<Diary> findByUserAndDate(User user, LocalDate date);
}
