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
	public List<Long> findRestorableFeedIds(List<Long> ids, String currentUserId, List<String> friendIds) {
		if (ids.isEmpty()) {
			return List.of();
		}

		List<Long> restorableIds = friendIds == null || friendIds.isEmpty()
				? diaryJpaRepository.findRestorablePublicFeedIds(ids, currentUserId)
				: diaryJpaRepository.findRestorableFeedIds(ids, currentUserId, friendIds);
		java.util.Set<Long> restorableIdSet = new java.util.HashSet<>(restorableIds);

		return ids.stream()
				.filter(restorableIdSet::contains)
				.toList();
	}

	@Override
	public List<Long> findFeedIdsByStatusAndUserIds(DiaryVisibility status, List<String> userIds) {
		if (userIds.isEmpty()) {
			return List.of();
		}
		return diaryJpaRepository.findFeedIdsByStatusAndUserIdIn(status, userIds);
	}

	@Override
	public List<Long> findFeedIdsByStatus(DiaryVisibility status, String excludedUserId) {
		if (excludedUserId == null || excludedUserId.isBlank()) {
			return diaryJpaRepository.findFeedIdsByStatus(status);
		}
		return diaryJpaRepository.findFeedIdsByStatusAndUserIdNot(status, excludedUserId);
	}

	@Override
	public List<String> getPhotosForDiary(Diary diary, RequestMetaInfo requestMetaInfo) {
		List<Photo> photos = photoJpaRepository.findByDiaryId(diary.getId());
		return diaryQueryService.sortPhotos(photos, requestMetaInfo);
	}
}
