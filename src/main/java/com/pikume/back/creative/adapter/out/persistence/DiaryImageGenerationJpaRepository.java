package com.pikume.back.creative.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pikume.back.creative.domain.DiaryImageGeneration;

import java.util.List;
import java.util.Optional;

public interface DiaryImageGenerationJpaRepository extends JpaRepository<DiaryImageGeneration, Long> {

	List<DiaryImageGeneration> findByDiaryIdIsNull();

	Optional<DiaryImageGeneration> findByUserIdAndFilePath(String userId, String filePath);

	boolean existsByIdAndUserId(Long id, String userId);
}
