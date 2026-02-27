package com.pikume.back.social.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.social.application.port.out.LoadDiaryInfoPort;

import java.util.Optional;

/**
 * Diary Context의 DiaryRepository를 래핑하여
 * Social Context에서 일기 정보를 조회하는 cross-context 어댑터.
 */
@Component
@RequiredArgsConstructor
public class DiaryAdapterForSocial implements LoadDiaryInfoPort {

	private final DiaryJpaRepository diaryJpaRepository;

	@Override
	public boolean existsById(Long diaryId) {
		return diaryJpaRepository.existsById(diaryId);
	}

	@Override
	public Optional<String> findOwnerUserIdByDiaryId(Long diaryId) {
		return diaryJpaRepository.findById(diaryId)
				.map(diary -> diary.getUserId());
	}
}
