package store.piku.back.feed.adapter.out.diary;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.diary.adapter.out.persistence.DiaryJpaRepository;
import store.piku.back.diary.adapter.out.persistence.PhotoJpaRepository;
import store.piku.back.diary.application.service.DiaryQueryService;
import store.piku.back.diary.domain.Diary;
import store.piku.back.diary.domain.Photo;
import store.piku.back.diary.domain.vo.DiaryVisibility;
import store.piku.back.feed.application.port.out.LoadDiaryForFeedPort;
import store.piku.back.global.dto.RequestMetaInfo;

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
		return diaryJpaRepository.findAllById(ids);
	}

	@Override
	public List<Diary> findByStatusAndUserIdIn(DiaryVisibility status, List<String> userIds) {
		return diaryJpaRepository.findByStatusAndUserIdIn(status, userIds);
	}

	@Override
	public List<Diary> findByStatusOrderByCreatedAtDesc(DiaryVisibility status) {
		return diaryJpaRepository.findByStatusOrderByCreatedAtDesc(status);
	}

	@Override
	public List<String> getPhotosForDiary(Diary diary, RequestMetaInfo requestMetaInfo) {
		List<Photo> photos = photoJpaRepository.findByDiaryId(diary.getId());
		return diaryQueryService.sortPhotos(photos, requestMetaInfo);
	}
}
