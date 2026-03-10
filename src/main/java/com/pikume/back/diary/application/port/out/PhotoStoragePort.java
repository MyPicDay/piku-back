package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.Diary;
import com.pikume.back.global.dto.UploadedFileData;

import java.io.IOException;

public interface PhotoStoragePort {
	void savePhoto(Diary diary, UploadedFileData photo, String userId, Integer order) throws IOException;

	String getPhotoUrl(String objectName, boolean isPublic);

	String moveToPublic(String sourceKey);

	String saveAIPhoto(String base64Data, String userId, String fileExtension);
}
