package com.pikume.back.creative.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.creative.application.port.out.LoadGenerationPort;
import com.pikume.back.creative.application.port.out.SaveGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;

import java.util.List;
import java.util.Optional;

/**
 * 생성 이력 Persistence Adapter
 */
@Component
@RequiredArgsConstructor
public class GenerationPersistenceAdapter implements LoadGenerationPort, SaveGenerationPort {

	private final DiaryImageGenerationJpaRepository repository;

	@Override
	public Optional<DiaryImageGeneration> findById(Long id) {
		return repository.findById(id);
	}

	@Override
	public List<DiaryImageGeneration> findByDiaryIdIsNull() {
		return repository.findByDiaryIdIsNull();
	}

	@Override
	public Optional<DiaryImageGeneration> findByUserIdAndFilePath(String userId, String filePath) {
		return repository.findByUserIdAndFilePath(userId, filePath);
	}

	@Override
	public boolean existsByIdAndUserId(Long id, String userId) {
		return repository.existsByIdAndUserId(id, userId);
	}

	@Override
	public DiaryImageGeneration save(DiaryImageGeneration generation) {
		return repository.save(generation);
	}
}
