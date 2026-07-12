package com.pikume.back.user.adapter.out.persistence;

import com.pikume.back.user.application.port.out.CheckUserUniquenessPort;
import com.pikume.back.user.application.port.out.LoadUserAccountPort;
import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.vo.Email;
import com.pikume.back.user.domain.vo.Nickname;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserAccountPersistenceAdapter implements LoadUserAccountPort, CheckUserUniquenessPort {

	private final UserJpaRepository jpaRepository;

	@Override
	public Optional<User> findById(String userId) {
		return jpaRepository.findById(userId);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return jpaRepository.findByEmail(new Email(email));
	}

	@Override
	public List<User> findAllByIds(Collection<String> userIds) {
		return jpaRepository.findAllById(userIds);
	}

	@Override
	public boolean existsByNickname(String nickname) {
		return jpaRepository.existsByNickname(new Nickname(nickname));
	}

	@Override
	public boolean existsByEmail(String email) {
		return jpaRepository.existsByEmail(new Email(email));
	}
}
