package com.pikume.back.diary.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pikume.back.diary.domain.Photo;

import java.util.List;
import java.util.Optional;

public interface PhotoJpaRepository extends JpaRepository<Photo, Integer> {
	List<Photo> findByDiaryId(Long diaryId);

	Optional<Photo> findFirstByDiaryIdAndRepresentIsTrue(Long diaryId);
}
