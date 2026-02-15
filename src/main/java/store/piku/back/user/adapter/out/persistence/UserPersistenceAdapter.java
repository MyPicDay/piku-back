package store.piku.back.user.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import store.piku.back.user.application.port.out.LoadUserPort;
import store.piku.back.user.application.port.out.SaveUserPort;
import store.piku.back.user.application.port.out.UserQueryPort;
import store.piku.back.user.domain.User;

import java.util.Optional;

/**
 * User 영속성 어댑터
 * LoadUserPort, SaveUserPort, UserQueryPort를 구현하는 JPA 어댑터입니다.
 */
@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort, UserQueryPort {

	private final UserJpaRepository jpaRepository;

	@Override
	public Optional<User> findById(String userId) {
		return jpaRepository.findById(userId);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return jpaRepository.findByEmail(email);
	}

	@Override
	public User save(User user) {
		return jpaRepository.save(user);
	}

	@Override
	public boolean existsByNickname(String nickname) {
		return jpaRepository.existsByNickname(nickname);
	}

	@Override
	public boolean existsByEmail(String email) {
		return jpaRepository.existsByEmail(email);
	}

	@Override
	public Page<User> searchByName(String keyword, Pageable pageable) {
		return jpaRepository.searchByName(keyword, pageable);
	}
}
