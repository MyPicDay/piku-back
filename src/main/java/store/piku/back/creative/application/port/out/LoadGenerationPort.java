package store.piku.back.creative.application.port.out;

import store.piku.back.creative.domain.DiaryImageGeneration;

import java.util.List;
import java.util.Optional;

/**
 * 생성 이력 조회 Outbound Port
 */
public interface LoadGenerationPort {

	Optional<DiaryImageGeneration> findById(Long id);

	List<DiaryImageGeneration> findByDiaryIdIsNull();

	Optional<DiaryImageGeneration> findByUserIdAndFilePath(String userId, String filePath);

	boolean existsByIdAndUserId(Long id, String userId);
}
