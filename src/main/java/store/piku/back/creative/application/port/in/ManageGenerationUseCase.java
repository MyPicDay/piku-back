package store.piku.back.creative.application.port.in;

import store.piku.back.creative.domain.DiaryImageGeneration;
import store.piku.back.global.config.CustomUserDetails;

import java.util.List;

/**
 * 생성 이력 관리 Inbound Port
 */
public interface ManageGenerationUseCase {

	DiaryImageGeneration findById(Long id);

	void updateDiaryId(Long historyId, Long diaryId);

	List<DiaryImageGeneration> findUnsavedGenerations();

	DiaryImageGeneration getByUserIdAndFilePath(String userId, String filePath);

	void diaryUpdate(CustomUserDetails customUserDetails, Long diaryId, String path);

	boolean existsByIdAndUserId(Long id, String userId);
}
