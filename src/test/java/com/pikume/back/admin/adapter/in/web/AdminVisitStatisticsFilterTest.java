package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.port.in.RecordAdminStatisticsEventUseCase;
import com.pikume.back.admin.domain.AdminStatisticsEventType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminVisitStatisticsFilter")
class AdminVisitStatisticsFilterTest {

	@Mock
	private RecordAdminStatisticsEventUseCase recordUseCase;
	@Mock
	private FilterChain filterChain;

	@Test
	@DisplayName("익명 방문자의 vid 헤더를 해시해 방문 이벤트를 기록한다")
	void recordsHashedVidHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/feed");
		request.addHeader("vid", "visitor-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		new AdminVisitStatisticsFilter(recordUseCase).doFilter(request, response, filterChain);

		then(recordUseCase).should().record(
				AdminStatisticsEventType.VISIT,
				null,
				hash("visitor:visitor-123"));
		then(filterChain).should().doFilter(request, response);
	}

	private String hash(String value) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
	}
}
