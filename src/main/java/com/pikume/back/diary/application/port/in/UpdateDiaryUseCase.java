package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.DiaryUpdatedResult;
import com.pikume.back.diary.application.dto.UpdateDiaryCommand;

public interface UpdateDiaryUseCase {
	DiaryUpdatedResult updateDiary(Long diaryId, UpdateDiaryCommand command, String userId);
}
