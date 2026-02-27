package com.pikume.back.feed.adapter.out.diary;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.adapter.out.persistence.PhotoJpaRepository;
import com.pikume.back.diary.application.service.DiaryQueryService;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.port.out.LoadDiaryForFeedPort;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DiaryAdapterForFeed implements LoadDiaryForFeedPort {

	private final DiaryJpaRepository diaryJpaRepository;
	private final PhotoJpaRepository photoJpaRepository;
	private final DiaryQueryService diaryQueryService;

	@Override
	public Diary getDiaryById(Long diaryId) {
		return diaryQueryService.getDiaryById(diaryId);
	}

	@Override
	public List<Diary> findAllById(List<Long> ids) {
		return diaryJpaRepository.findByIdInAndDeletedAtIsNull(ids);
	}

	@Override
	public List<Diary> findByStatusAndUserIdIn(DiaryVisibility status, List<String> userIds) {
		return diaryJpaRepository.findByStatusAndUserIdInAndDeletedAtIsNull(status, userIds);
	}

	@Override
	public List<Diary> findByStatusOrderByCreatedAtDesc(DiaryVisibility status) {
		return diaryJpaRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status);
	}

	@Override
	public List<String> getPhotosForDiary(Diary diary, RequestMetaInfo requestMetaInfo) {
		List<Photo> photos = photoJpaRepository.findByDiaryId(diary.getId());
		return diaryQueryService.sortPhotos(photos, requestMetaInfo);
	}
}
