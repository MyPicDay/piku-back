package store.piku.back.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.diary.entity.Photo;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Integer> {

    List<Photo> findByDiaryId(Integer diaryId);
}
