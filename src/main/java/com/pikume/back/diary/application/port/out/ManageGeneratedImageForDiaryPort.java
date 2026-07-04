package com.pikume.back.diary.application.port.out;

public interface ManageGeneratedImageForDiaryPort {

	String loadGeneratedImagePath(Long generationId);

	boolean isGeneratedImageOwnedByUser(Long generationId, String userId);

	void updateGeneratedImagePath(Long generationId, String filePath);

	void attachGeneratedImageToDiary(Long generationId, Long diaryId);
}
