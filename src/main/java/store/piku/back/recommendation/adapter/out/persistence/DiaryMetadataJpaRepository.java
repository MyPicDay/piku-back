package store.piku.back.recommendation.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.piku.back.recommendation.domain.DiaryMetadata;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryMetadataJpaRepository extends JpaRepository<DiaryMetadata, Long> {

	Optional<DiaryMetadata> findByDiaryId(Long diaryId);

	boolean existsByDiaryId(Long diaryId);

	@Query("SELECT dm FROM DiaryMetadata dm WHERE dm.diaryId IN :diaryIds")
	List<DiaryMetadata> findByDiaryIds(@Param("diaryIds") List<Long> diaryIds);

	@Query("SELECT dm FROM DiaryMetadata dm WHERE dm.primaryTopic = :topic")
	List<DiaryMetadata> findByPrimaryTopic(@Param("topic") String topic);
}
