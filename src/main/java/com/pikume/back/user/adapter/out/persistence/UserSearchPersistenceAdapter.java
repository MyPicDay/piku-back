package com.pikume.back.user.adapter.out.persistence;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.user.application.port.out.SearchUserPort;
import com.pikume.back.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserSearchPersistenceAdapter implements SearchUserPort {

	private final UserJpaRepository jpaRepository;

	@Override
	public PageResult<User> searchUsers(String keyword, PageQuery pageQuery) {
		return SpringPageMapper.toPageResult(
				jpaRepository.searchByName(keyword, SpringPageMapper.toPageable(pageQuery)));
	}
}
