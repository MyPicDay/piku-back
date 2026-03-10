package com.pikume.back.creative.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.creative.application.dto.DiaryImageGenerationView;
import com.pikume.back.creative.application.port.in.ManageGenerationUseCase;
import com.pikume.back.creative.application.port.out.LoadGenerationPort;
import com.pikume.back.creative.application.port.out.SaveGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;
import com.pikume.back.global.config.CustomUserDetails;

import java.util.List;

/**
 * 생성 이력 관리 Application Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DiaryImageGenerationService implements ManageGenerationUseCase {

	private final LoadGenerationPort loadGenerationPort;
	private final SaveGenerationPort saveGenerationPort;

	@Override
	public DiaryImageGenerationView findById(Long id) {
		return toView(loadGenerationPort.findById(id)
				.orElseThrow(() -> new RuntimeException("DiaryImageGeneration not found with id: " + id)));
	}

	@Override
	@Transactional
	public void updateDiaryId(Long historyId, Long diaryId) {
		DiaryImageGeneration generation = loadGenerationPort.findById(historyId)
				.orElseThrow(() -> new RuntimeException("DiaryImageGeneration not found with id: " + historyId));
		generation.saveDiaryId(diaryId);
		saveGenerationPort.save(generation);
	}

	@Override
	public List<DiaryImageGenerationView> findUnsavedGenerations() {
		return loadGenerationPort.findByDiaryIdIsNull().stream()
				.map(this::toView)
				.toList();
	}

	@Override
	public DiaryImageGenerationView getByUserIdAndFilePath(String userId, String filePath) {
		return toView(loadGenerationPort.findByUserIdAndFilePath(userId, filePath)
				.orElseThrow(() -> new RuntimeException(
						"DiaryImageGeneration not found for userId: " + userId + " and filePath: " + filePath)));
	}

	@Override
	@Transactional
	public void diaryUpdate(CustomUserDetails customUserDetails, Long diaryId, String path) {
		String userId = customUserDetails.getId();
		DiaryImageGenerationView generation = getByUserIdAndFilePath(userId, path);
		updateDiaryId(generation.id(), diaryId);
	}

	@Override
	@Transactional
	public void updateFilePath(Long generationId, String filePath) {
		DiaryImageGeneration generation = loadGenerationPort.findById(generationId)
				.orElseThrow(() -> new RuntimeException("DiaryImageGeneration not found with id: " + generationId));
		generation.updateFilePath(filePath);
		saveGenerationPort.save(generation);
	}

	@Override
	public boolean existsByIdAndUserId(Long id, String userId) {
		return loadGenerationPort.existsByIdAndUserId(id, userId);
	}

	private DiaryImageGenerationView toView(DiaryImageGeneration generation) {
		return new DiaryImageGenerationView(
				generation.getId(),
				generation.getUserId(),
				generation.getPrompt(),
				generation.getFilePath(),
				generation.getDiaryId());
	}
}
