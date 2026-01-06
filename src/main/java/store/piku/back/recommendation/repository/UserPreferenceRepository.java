package store.piku.back.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.piku.back.recommendation.entity.UserPreference;

import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

	Optional<UserPreference> findByUserId(String userId);

	boolean existsByUserId(String userId);
}
