package com.pikume.back.recommendation.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.recommendation.application.port.out.LoadDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.LoadUserPreferencePort;
import com.pikume.back.recommendation.application.port.out.RecordDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.RecordUserPreferencePort;
import com.pikume.back.recommendation.domain.DiaryMetadata;
import com.pikume.back.recommendation.domain.UserPreference;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecommendationPersistenceAdapter implements
		LoadDiaryMetadataPort, RecordDiaryMetadataPort,
		LoadUserPreferencePort, RecordUserPreferencePort {

	private final DiaryMetadataJpaRepository diaryMetadataJpaRepository;
	private final UserPreferenceJpaRepository userPreferenceJpaRepository;

	// --- DiaryMetadata ---

	@Override
	public Optional<DiaryMetadata> loadByDiaryId(Long diaryId) {
		return diaryMetadataJpaRepository.findByDiaryId(diaryId);
	}

	@Override
	public List<DiaryMetadata> loadByDiaryIds(List<Long> diaryIds) {
		return diaryMetadataJpaRepository.findByDiaryIds(diaryIds);
	}

	@Override
	public DiaryMetadata recordDiaryMetadata(DiaryMetadata metadata) {
		return diaryMetadataJpaRepository.save(metadata);
	}

	// --- UserPreference ---

	@Override
	public Optional<UserPreference> loadByUserId(String userId) {
		return userPreferenceJpaRepository.findByUserId(userId);
	}

	@Override
	public UserPreference recordUserPreference(UserPreference preference) {
		return userPreferenceJpaRepository.save(preference);
	}
}
