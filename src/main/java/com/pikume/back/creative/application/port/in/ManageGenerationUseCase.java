package com.pikume.back.creative.application.port.in;

import com.pikume.back.creative.application.dto.DiaryImageGenerationView;
import com.pikume.back.global.config.CustomUserDetails;

import java.util.List;

/**
 * 생성 이력 관리 Inbound Port
 */
public interface ManageGenerationUseCase {

	DiaryImageGenerationView findById(Long id);

	void updateDiaryId(Long historyId, Long diaryId);

	List<DiaryImageGenerationView> findUnsavedGenerations();

	DiaryImageGenerationView getByUserIdAndFilePath(String userId, String filePath);

	void diaryUpdate(CustomUserDetails customUserDetails, Long diaryId, String path);

	void updateFilePath(Long generationId, String filePath);

	boolean existsByIdAndUserId(Long id, String userId);
}
