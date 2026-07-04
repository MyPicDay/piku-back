package com.pikume.back.creative.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.creative.application.dto.DiaryImageGenerationView;
import com.pikume.back.creative.application.port.in.ManageGenerationUseCase;
import com.pikume.back.creative.application.port.out.LoadGenerationPort;
import com.pikume.back.creative.application.port.out.RecordGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;

/**
 * 생성 이력 관리 Application Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DiaryImageGenerationService implements ManageGenerationUseCase {

	private final LoadGenerationPort loadGenerationPort;
	private final RecordGenerationPort recordGenerationPort;

	@Override
	public DiaryImageGenerationView loadGenerationForDiary(Long generationId) {
		return toView(loadGenerationPort.loadGenerationForDiaryIntegration(generationId)
				.orElseThrow(() -> new RuntimeException(
						"DiaryImageGeneration not found with id: " + generationId)));
	}

	@Override
	@Transactional
	public void attachGenerationToDiary(Long generationId, Long diaryId) {
		DiaryImageGeneration generation = loadGenerationPort.loadGenerationForDiaryIntegration(generationId)
				.orElseThrow(() -> new RuntimeException(
						"DiaryImageGeneration not found with id: " + generationId));
		generation.saveDiaryId(diaryId);
		recordGenerationPort.recordGeneration(generation);
	}

	@Override
	@Transactional
	public void updateGeneratedImagePath(Long generationId, String filePath) {
		DiaryImageGeneration generation = loadGenerationPort.loadGenerationForDiaryIntegration(generationId)
				.orElseThrow(() -> new RuntimeException("DiaryImageGeneration not found with id: " + generationId));
		generation.updateFilePath(filePath);
		recordGenerationPort.recordGeneration(generation);
	}

	@Override
	public boolean isGenerationOwnedByUser(Long generationId, String userId) {
		return loadGenerationPort.isGenerationOwnedByUser(generationId, userId);
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
