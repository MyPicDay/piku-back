package store.piku.back.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.diary.entity.Diary;

public interface DiaryRepository extends JpaRepository<Diary, Integer> {

}
