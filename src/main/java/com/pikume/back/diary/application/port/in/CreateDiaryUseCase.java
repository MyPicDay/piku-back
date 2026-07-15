package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.CreateDiaryCommand;
import com.pikume.back.diary.application.dto.DiaryCreatedResult;
import com.pikume.back.diary.application.dto.DiaryPhotoUpload;

import java.util.List;

public interface CreateDiaryUseCase {
	DiaryCreatedResult createDiary(CreateDiaryCommand diaryCommand, List<DiaryPhotoUpload> photos, String userId);
}
