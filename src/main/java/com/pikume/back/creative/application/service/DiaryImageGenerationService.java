package com.pikume.back.creative.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.creative.application.dto.DiaryImageGenerationView;
import com.pikume.back.creative.application.exception.CreativeErrorCode;
import com.pikume.back.creative.application.exception.CreativeException;
import com.pikume.back.creative.application.port.in.AttachGeneratedImageToDiaryUseCase;
import com.pikume.back.creative.application.port.in.QueryDiaryImageGenerationUseCase;
import com.pikume.back.creative.application.port.in.UpdateGeneratedImagePathUseCase;
import com.pikume.back.creative.application.port.out.LoadGenerationForDiaryPort;
import com.pikume.back.creative.application.port.out.RecordGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;

/**
 * 생성 이력 관리 Application Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DiaryImageGenerationService
		implements QueryDiaryImageGenerationUseCase,
		AttachGeneratedImageToDiaryUseCase,
		UpdateGeneratedImagePathUseCase {

	private final LoadGenerationForDiaryPort loadGenerationForDiaryPort;
	private final RecordGenerationPort recordGenerationPort;

	@Override
	public DiaryImageGenerationView queryGenerationForDiary(Long generationId) {
		return toView(loadGenerationForDiaryPort.loadGenerationForDiary(generationId)
				.orElseThrow(() -> new CreativeException(CreativeErrorCode.GENERATION_NOT_FOUND)));
	}

	@Override
	@Transactional
	public void attachGenerationToDiary(Long generationId, Long diaryId) {
		DiaryImageGeneration generation = loadGenerationForDiaryPort.loadGenerationForDiary(generationId)
				.orElseThrow(() -> new CreativeException(CreativeErrorCode.GENERATION_NOT_FOUND));
		generation.attachToDiary(diaryId);
		recordGenerationPort.recordGeneration(generation);
	}

	@Override
	@Transactional
	public void updateGeneratedImagePath(Long generationId, String filePath) {
		DiaryImageGeneration generation = loadGenerationForDiaryPort.loadGenerationForDiary(generationId)
				.orElseThrow(() -> new CreativeException(CreativeErrorCode.GENERATION_NOT_FOUND));
		generation.updateFilePath(filePath);
		recordGenerationPort.recordGeneration(generation);
	}

	@Override
	public boolean isGenerationAvailableForDiary(Long generationId, String userId) {
		return loadGenerationForDiaryPort.isGenerationAvailableForDiary(generationId, userId);
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
