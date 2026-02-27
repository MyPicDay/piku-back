package com.pikume.back.recommendation.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.recommendation.application.port.out.LoadDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.LoadUserPreferencePort;
import com.pikume.back.recommendation.application.port.out.SaveDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.SaveUserPreferencePort;
import com.pikume.back.recommendation.domain.DiaryMetadata;
import com.pikume.back.recommendation.domain.UserPreference;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecommendationPersistenceAdapter implements
		LoadDiaryMetadataPort, SaveDiaryMetadataPort,
		LoadUserPreferencePort, SaveUserPreferencePort {

	private final DiaryMetadataJpaRepository diaryMetadataJpaRepository;
	private final UserPreferenceJpaRepository userPreferenceJpaRepository;

	// --- DiaryMetadata ---

	@Override
	public Optional<DiaryMetadata> findByDiaryId(Long diaryId) {
		return diaryMetadataJpaRepository.findByDiaryId(diaryId);
	}

	@Override
	public List<DiaryMetadata> findByDiaryIds(List<Long> diaryIds) {
		return diaryMetadataJpaRepository.findByDiaryIds(diaryIds);
	}

	@Override
	public List<DiaryMetadata> findByPrimaryTopic(String topic) {
		return diaryMetadataJpaRepository.findByPrimaryTopic(topic);
	}

	@Override
	public boolean existsByDiaryId(Long diaryId) {
		return diaryMetadataJpaRepository.existsByDiaryId(diaryId);
	}

	@Override
	public DiaryMetadata save(DiaryMetadata metadata) {
		return diaryMetadataJpaRepository.save(metadata);
	}

	// --- UserPreference ---

	@Override
	public Optional<UserPreference> findByUserId(String userId) {
		return userPreferenceJpaRepository.findByUserId(userId);
	}

	@Override
	public boolean existsByUserId(String userId) {
		return userPreferenceJpaRepository.existsByUserId(userId);
	}

	@Override
	public UserPreference save(UserPreference preference) {
		return userPreferenceJpaRepository.save(preference);
	}
}
