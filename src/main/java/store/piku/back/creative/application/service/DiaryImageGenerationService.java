package store.piku.back.creative.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.creative.application.port.in.ManageGenerationUseCase;
import store.piku.back.creative.application.port.out.LoadGenerationPort;
import store.piku.back.creative.application.port.out.SaveGenerationPort;
import store.piku.back.creative.domain.DiaryImageGeneration;
import store.piku.back.global.config.CustomUserDetails;

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
	public DiaryImageGeneration findById(Long id) {
		return loadGenerationPort.findById(id)
				.orElseThrow(() -> new RuntimeException("DiaryImageGeneration not found with id: " + id));
	}

	@Override
	@Transactional
	public void updateDiaryId(Long historyId, Long diaryId) {
		DiaryImageGeneration generation = findById(historyId);
		generation.saveDiaryId(diaryId);
		saveGenerationPort.save(generation);
	}

	@Override
	public List<DiaryImageGeneration> findUnsavedGenerations() {
		return loadGenerationPort.findByDiaryIdIsNull();
	}

	@Override
	public DiaryImageGeneration getByUserIdAndFilePath(String userId, String filePath) {
		return loadGenerationPort.findByUserIdAndFilePath(userId, filePath)
				.orElseThrow(() -> new RuntimeException(
						"DiaryImageGeneration not found for userId: " + userId + " and filePath: " + filePath));
	}

	@Override
	@Transactional
	public void diaryUpdate(CustomUserDetails customUserDetails, Long diaryId, String path) {
		String userId = customUserDetails.getId();
		DiaryImageGeneration generation = getByUserIdAndFilePath(userId, path);
		updateDiaryId(generation.getId(), diaryId);
	}

	@Override
	public boolean existsByIdAndUserId(Long id, String userId) {
		return loadGenerationPort.existsByIdAndUserId(id, userId);
	}
}
