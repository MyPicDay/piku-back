package store.piku.back.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.diary.entity.Photo;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Integer> {

    List<Photo> findByDiaryId(Long diaryId);
    Optional<Photo> findFirstByDiaryIdAndRepresentIsTrue(Long diaryId);

    List<Photo> findAllByDiaryId(Long diaryId);
}
