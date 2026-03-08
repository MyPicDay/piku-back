package com.pikume.back.feed.application.port.out;

import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.util.List;

public interface LoadDiaryForFeedPort {

	Diary getDiaryById(Long diaryId);

	List<Long> findRestorableFeedIds(List<Long> ids, String currentUserId, List<String> friendIds);

	List<Long> findFeedIdsByStatusAndUserIds(DiaryVisibility status, List<String> userIds);

	List<Long> findFeedIdsByStatus(DiaryVisibility status, String excludedUserId);

	List<String> getPhotosForDiary(Diary diary, RequestMetaInfo requestMetaInfo);
}
