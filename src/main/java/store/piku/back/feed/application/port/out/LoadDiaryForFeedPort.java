package store.piku.back.feed.application.port.out;

import store.piku.back.diary.domain.Diary;
import store.piku.back.diary.domain.vo.DiaryVisibility;
import store.piku.back.global.dto.RequestMetaInfo;

import java.util.List;

public interface LoadDiaryForFeedPort {

	Diary getDiaryById(Long diaryId);

	List<Diary> findAllById(List<Long> ids);

	List<Diary> findByStatusAndUserIdIn(DiaryVisibility status, List<String> userIds);

	List<Diary> findByStatusOrderByCreatedAtDesc(DiaryVisibility status);

	List<String> getPhotosForDiary(Diary diary, RequestMetaInfo requestMetaInfo);
}
