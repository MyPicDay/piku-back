package store.piku.back.social.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.social.application.port.out.LoadDiaryInfoPort;

import java.util.Optional;

/**
 * Diary Context의 DiaryRepository를 래핑하여
 * Social Context에서 일기 정보를 조회하는 cross-context 어댑터.
 */
@Component
@RequiredArgsConstructor
public class DiaryAdapterForSocial implements LoadDiaryInfoPort {

	private final DiaryRepository diaryRepository;

	@Override
	public boolean existsById(Long diaryId) {
		return diaryRepository.existsById(diaryId);
	}

	@Override
	public Optional<String> findOwnerUserIdByDiaryId(Long diaryId) {
		return diaryRepository.findById(diaryId)
				.map(diary -> diary.getUser().getId());
	}
}
