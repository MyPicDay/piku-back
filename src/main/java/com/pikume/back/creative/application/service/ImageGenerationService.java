package com.pikume.back.creative.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.creative.application.port.in.GenerateImageUseCase;
import com.pikume.back.creative.application.port.out.AiImageGeneratorPort;
import com.pikume.back.creative.application.port.out.SaveGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;
import com.pikume.back.creative.domain.exception.ImageGenerationException;
import com.pikume.back.diary.adapter.out.storage.MinioPhotoStorageAdapter;
import com.pikume.back.global.util.FileUtil;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.domain.User;
import com.pikume.back.global.exception.BusinessException;
import com.pikume.back.global.error.ErrorCode;

/**
 * AI 이미지 생성 Application Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageGenerationService implements GenerateImageUseCase {

	private final AiImageGeneratorPort aiImageGeneratorPort;
	private final SaveGenerationPort saveGenerationPort;
	private final FileUtil fileUtil;
	private final LoadUserPort loadUserPort;
	private final MinioPhotoStorageAdapter photoStorage;

	@Override
	@Transactional
	public DiaryImageGeneration generateDiaryImage(String content, String userId, RequestMetaInfo requestMetaInfo) {
		log.info("사용자 ID '{}' 일기 이미지 생성 요청", userId);

		User user = loadUserPort.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		String avatarPath = user.getAvatar();

		String characterImageBase64 = fileUtil.getImageAsBase64(avatarPath);
		if (characterImageBase64 == null || characterImageBase64.trim().isEmpty()) {
			log.error("사용자 '{}'의 아바타 이미지 파일을 읽을 수 없습니다. 경로: {}", userId, avatarPath);
			throw new ImageGenerationException("사용자 아바타 이미지 파일을 읽을 수 없습니다. 경로: " + avatarPath);
		}
		log.info("사용자 '{}'의 아바타 이미지 로드 완료. Base64 길이: {}", userId, characterImageBase64.length());

		String prompt = buildActionPrompt(content, detectDesignTextIntent(content));
		log.info("일기 프롬프트 생성 완료: {}", prompt);

		String generatedImageRelativePath = generateCharacterActionImage(prompt, characterImageBase64, userId);
		if (generatedImageRelativePath == null) {
			throw new ImageGenerationException("Gemini API에서 이미지 생성을 실패했거나 파일 저장에 실패했습니다.");
		}

		String aiUrl = photoStorage.getPhotoUrl(generatedImageRelativePath, false);
		DiaryImageGeneration diaryImageGeneration = saveGenerationPort.save(
				new DiaryImageGeneration(userId, prompt, generatedImageRelativePath));
		log.info("생성된 이미지 URL: {}", aiUrl);

		return diaryImageGeneration;
	}

	/**
	 * 생성된 이미지의 URL을 반환 (DiaryImageGeneration에 없는 정보이므로 별도 제공)
	 */
	public String getImageUrl(String filePath) {
		return photoStorage.getPhotoUrl(filePath, false);
	}

	private String generateCharacterActionImage(String prompt, String characterImageBase64, String userId) {
		log.info("Gemini API 호출 (멀티모달: 사용자 ID: {}", userId);

		try {
			String base64ImageData = aiImageGeneratorPort.editImage(characterImageBase64, prompt).block();

			if (base64ImageData != null && !base64ImageData.isEmpty()) {
				String savedFilePathSuffix = photoStorage.saveAIPhoto(base64ImageData, userId, "png");
				log.info("Gemini API 행위 이미지 생성 및 저장 성공: 사용자 ID: {}, 경로: {}", userId, savedFilePathSuffix);
				return savedFilePathSuffix;
			} else {
				log.error("Gemini API에서 이미지 데이터를 받지 못했습니다. userId: {}", userId);
				throw new ImageGenerationException("Gemini API에서 이미지 데이터를 받지 못했습니다.");
			}

		} catch (ImageGenerationException e) {
			throw e;
		} catch (Exception e) {
			log.error("Gemini API 일기 이미지 생성 중 오류 발생: 사용자 ID: {},", userId, e);
			throw new ImageGenerationException("일기 이미지 생성 실패: " + e.getMessage(), e);
		}
	}

	public String buildActionPrompt(String content, boolean hasDesignText) {
		StringBuilder sb = new StringBuilder();

		sb.append("Using the provided character reference image(s), ");
		sb.append("generate ONE image of the SAME character performing this diary action: ");
		sb.append(content).append(". ");

		sb.append("Follow ALL requirements: ");
		sb.append(
				"• Identity & Style Consistency: Match the character's facial features, hairstyle, skin tone, body proportions, clothing palette, and art style from the reference image(s). ");
		sb.append(
				"• Scene: Build a coherent, artistic scene that supports the action with appropriate environment, props, and a natural pose. ");
		sb.append(
				"• Composition & Camera: Use clear framing/angle that showcases the action; cinematic lighting that matches the mood; avoid awkward crops. ");
		sb.append("• Quality: High detail with clean rendering and correct anatomy/hands/fingers. ");

		sb.append("• Text Rendering Rule: ");
		if (hasDesignText) {
			sb.append(
					"If the scene intentionally includes a design element that contains text (e.g., speech bubbles, signs, posters, UI panels, clothing prints), ");
			sb.append("you MAY render SHORT, readable text only INSIDE those elements. ");
		} else {
			sb.append("Render NO text anywhere: no words, letters, numbers, logos, watermarks, or captions. ");
			sb.append("If an object would normally have text, leave it blank or use non-legible abstract marks. ");
		}

		sb.append("• Output: A single finished image only. ");
		sb.append(
				"• Constraints: Respect the reference style/lighting/perspective; keep the background coherent but not distracting.");

		return sb.toString();
	}

	public boolean detectDesignTextIntent(String content) {
		String lower = content.toLowerCase();
		return lower.contains("말풍선") || lower.contains("간판") || lower.contains("표지판")
				|| lower.contains("사인") || lower.contains("포스터") || lower.contains("배너")
				|| lower.contains("현수막") || lower.contains("간판문구")
				|| lower.contains("ui") || lower.contains("패널")
				|| lower.contains("티셔츠 프린트") || lower.contains("텍스트")
				|| lower.contains("speech bubble") || lower.contains("sign")
				|| lower.contains("poster") || lower.contains("banner") || lower.contains("caption");
	}
}
