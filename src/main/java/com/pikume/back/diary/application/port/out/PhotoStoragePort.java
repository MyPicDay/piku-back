package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.dto.UploadedFileData;

import java.io.IOException;

public interface PhotoStoragePort {
	void savePhoto(Diary diary, UploadedFileData photo, String userId, Integer order) throws IOException;

	String getPhotoUrl(String objectName, boolean isPublic);

	String moveToPublic(String sourceKey);

	String copyToVisibilityScope(String sourceKey, DiaryVisibility visibility, DiaryPhotoType sourceType);

	void deleteObject(String objectKey);

	String saveAIPhoto(String base64Data, String userId, String fileExtension);
}
