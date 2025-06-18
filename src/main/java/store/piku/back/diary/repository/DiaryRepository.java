package store.piku.back.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.diary.entity.Diary;

import java.time.LocalDate;
import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    List<Diary> findByUserIdAndDateBetween(String userId, LocalDate start, LocalDate end);
}
