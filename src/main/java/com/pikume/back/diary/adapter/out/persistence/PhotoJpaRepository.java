package com.pikume.back.diary.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pikume.back.diary.domain.Photo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PhotoJpaRepository extends JpaRepository<Photo, Integer> {

	interface DiaryThumbnailProjection {
		Long getDiaryId();

		String getUrl();
	}

	List<Photo> findByDiaryId(Long diaryId);

	Optional<Photo> findFirstByDiaryIdAndRepresentIsTrue(Long diaryId);

	@Query("SELECT p.diary.id AS diaryId, p.url AS url " +
			"FROM Photo p " +
			"WHERE p.diary.id IN :diaryIds AND p.represent = true")
	List<DiaryThumbnailProjection> findRepresentPhotoUrlsByDiaryIds(@Param("diaryIds") Collection<Long> diaryIds);
}
