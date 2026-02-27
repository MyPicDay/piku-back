package com.pikume.back.feed.application.port.out;

import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.util.List;

public interface LoadDiaryForFeedPort {

	Diary getDiaryById(Long diaryId);

	List<Diary> findAllById(List<Long> ids);

	List<Diary> findByStatusAndUserIdIn(DiaryVisibility status, List<String> userIds);

	List<Diary> findByStatusOrderByCreatedAtDesc(DiaryVisibility status);

	List<String> getPhotosForDiary(Diary diary, RequestMetaInfo requestMetaInfo);
}
