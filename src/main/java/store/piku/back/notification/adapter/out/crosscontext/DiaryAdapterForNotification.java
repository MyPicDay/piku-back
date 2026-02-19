package store.piku.back.notification.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.diary.adapter.out.persistence.PhotoJpaRepository;
import store.piku.back.diary.adapter.out.storage.MinioPhotoStorageAdapter;
import store.piku.back.diary.domain.Photo;
import store.piku.back.notification.application.port.out.LoadDiaryForNotificationPort;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DiaryAdapterForNotification implements LoadDiaryForNotificationPort {

	private final PhotoJpaRepository photoJpaRepository;
	private final MinioPhotoStorageAdapter minioPhotoStorageAdapter;

	@Override
	public String getDiaryThumbnailUrl(Long diaryId) {
		Optional<Photo> representPhotoOpt = photoJpaRepository.findFirstByDiaryIdAndRepresentIsTrue(diaryId);
		return representPhotoOpt
				.map(Photo::getUrl)
				.map(url -> minioPhotoStorageAdapter.getPhotoUrl(url, true))
				.orElse(null);
	}
}
