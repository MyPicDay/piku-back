package com.pikume.back.user.application.port.in;

import com.pikume.back.user.application.dto.UserSummaryView;

import java.util.Map;
import java.util.Set;

public interface QueryUserSummaryUseCase {

	Map<String, UserSummaryView> queryUserSummaries(Set<String> userIds);
}
