package com.pikume.back.creative.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.creative.application.dto.DiaryIllustrationRequest;
import com.pikume.back.creative.application.dto.AiGenerationQuotaConsumption;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.dto.GeneratedIllustrationPayload;
import com.pikume.back.creative.application.exception.AiGenerationQuotaExceededException;
import com.pikume.back.creative.application.policy.DiaryIllustrationPromptPolicy;
import com.pikume.back.creative.application.port.in.GenerateImageUseCase;
import com.pikume.back.creative.application.port.in.ManageAiGenerationQuotaUseCase;
import com.pikume.back.creative.application.port.in.RecordAiPhotoStatisticsUseCase;
import com.pikume.back.creative.application.port.out.CreativeImageStoragePort;
import com.pikume.back.creative.application.port.out.GenerateDiaryIllustrationPort;
import com.pikume.back.creative.application.port.out.LoadCharacterReferencePort;
import com.pikume.back.creative.application.port.out.SaveGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;
import com.pikume.back.creative.domain.exception.ImageGenerationException;

/**
 * AI 이미지 생성 Application Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageGenerationService implements GenerateImageUseCase {

	private final GenerateDiaryIllustrationPort generateDiaryIllustrationPort;
	private final SaveGenerationPort saveGenerationPort;
	private final LoadCharacterReferencePort loadCharacterReferencePort;
	private final CreativeImageStoragePort creativeImageStoragePort;
	private final DiaryIllustrationPromptPolicy diaryIllustrationPromptPolicy;
	private final ManageAiGenerationQuotaUseCase manageAiGenerationQuotaUseCase;
	private final RecordAiPhotoStatisticsUseCase recordAiPhotoStatisticsUseCase;

	@Override
	@Transactional
	public GeneratedImageResult generateDiaryImage(String content, String userId) {
		log.info("사용자 ID '{}' 일기 이미지 생성 요청", userId);
		recordAiPhotoStatisticsUseCase.recordRequest(userId);

		AiGenerationQuotaConsumption consumption = manageAiGenerationQuotaUseCase.tryConsumeForGeneration(userId);
		if (!consumption.consumed()) {
			throw new AiGenerationQuotaExceededException(consumption.dailyLimit());
		}

		GeneratedImageResult result;
		try {
			String characterImageBase64 = loadCharacterReferencePort.findByUserId(userId)
					.map(reference -> reference.imageBase64())
					.orElseThrow(() -> new ImageGenerationException("참조 캐릭터 이미지를 불러올 수 없습니다."));

			String prompt = diaryIllustrationPromptPolicy.createPrompt(content);
			log.info("일기 프롬프트 생성 완료: {}", prompt);

			GeneratedIllustrationPayload illustration = generateDiaryIllustrationPort.generate(
					new DiaryIllustrationRequest(prompt, characterImageBase64));
			String generatedImageRelativePath = creativeImageStoragePort.saveAIPhoto(
					illustration.imageBase64(),
					userId,
					illustration.fileExtension());

			String aiUrl = creativeImageStoragePort.getPhotoUrl(generatedImageRelativePath, false);
			DiaryImageGeneration diaryImageGeneration = saveGenerationPort.save(
					new DiaryImageGeneration(userId, prompt, generatedImageRelativePath));
			log.info("생성된 이미지 URL: {}", aiUrl);
			result = new GeneratedImageResult(diaryImageGeneration.getId(), aiUrl, generatedImageRelativePath);
		} catch (RuntimeException e) {
			releaseConsumptionSafely(userId, e);
			recordAiPhotoStatisticsUseCase.recordFailure(userId);
			throw e;
		}

		recordAiPhotoStatisticsUseCase.recordSuccess(userId);
		return result;
	}

	private void releaseConsumptionSafely(String userId, RuntimeException cause) {
		try {
			manageAiGenerationQuotaUseCase.releaseGenerationConsumption(userId);
		} catch (RuntimeException releaseFailure) {
			log.warn("event=ai_generation_quota_release_failed userId={} originalReason={} releaseReason={}",
					userId,
					cause.getMessage(),
					releaseFailure.getMessage());
		}
	}
}
