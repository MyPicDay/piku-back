package com.pikume.back.user.application.port.out;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.user.domain.User;

public interface SearchUserPort {

	PageResult<User> searchUsers(String keyword, PageQuery pageQuery);
}
