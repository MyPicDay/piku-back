package com.pikume.back.user.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.application.port.out.SaveUserPort;
import com.pikume.back.user.application.port.out.UserQueryPort;
import com.pikume.back.user.domain.User;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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
	public List<User> findAllByIds(Collection<String> userIds) {
		return jpaRepository.findAllById(userIds);
	}

	@Override
	public long countActiveMembers() {
		return jpaRepository.countByDeletedAtIsNull();
	}

	@Override
	public long countAllMembers() {
		return jpaRepository.count();
	}

	@Override
	public long countMembersBefore(LocalDateTime cutoffExclusive) {
		return jpaRepository.countByCreatedAtBefore(cutoffExclusive);
	}

	@Override
	public List<LoadUserPort.DailyCount> countSignupMembersByDate(LocalDate startDate, LocalDate endDate) {
		return jpaRepository.countSignupMembersByDate(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
				.stream()
				.map(row -> new LoadUserPort.DailyCount(toLocalDate(row.getMetricDate()), row.getMetricCount()))
				.toList();
	}

	@Override
	public List<LoadUserPort.DailyCount> countAllSignupMembersByDate(LocalDate startDate, LocalDate endDate) {
		return jpaRepository.countAllSignupMembersByDate(
						startDate.atStartOfDay(),
						endDate.plusDays(1).atStartOfDay())
				.stream()
				.map(row -> new LoadUserPort.DailyCount(toLocalDate(row.getMetricDate()), row.getMetricCount()))
				.toList();
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
	public PageResult<User> searchByName(String keyword, PageQuery pageQuery) {
		return SpringPageMapper.toPageResult(jpaRepository.searchByName(keyword, SpringPageMapper.toPageable(pageQuery)));
	}

	private LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		if (value instanceof Date date) {
			return date.toLocalDate();
		}
		if (value instanceof LocalDateTime dateTime) {
			return dateTime.toLocalDate();
		}
		return LocalDate.parse(String.valueOf(value));
	}
}
