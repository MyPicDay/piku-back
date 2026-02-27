package com.pikume.back.diary.application.port.in;

import org.springframework.web.multipart.MultipartFile;
import com.pikume.back.diary.adapter.in.web.dto.DiaryDTO;
import com.pikume.back.diary.adapter.in.web.dto.ResponseDiaryDTO;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.io.IOException;
import java.util.List;

public interface CreateDiaryUseCase {
	ResponseDiaryDTO createDiary(DiaryDTO diaryDTO, List<MultipartFile> photos,
			String userId, RequestMetaInfo requestMetaInfo) throws IOException;
}
