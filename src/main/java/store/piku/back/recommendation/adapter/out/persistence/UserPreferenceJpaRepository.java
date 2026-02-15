package store.piku.back.recommendation.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.piku.back.recommendation.domain.UserPreference;

import java.util.Optional;

@Repository
public interface UserPreferenceJpaRepository extends JpaRepository<UserPreference, Long> {

	Optional<UserPreference> findByUserId(String userId);

	boolean existsByUserId(String userId);
}
