package com.pikume.back.support.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Support exception handler integration")
class SupportExceptionHandlerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("문의 필수 multipart 누락은 Support 오류 계약을 우선 적용한다")
	void missingRequiredPartUsesSupportProblemDetails() throws Exception {
		mockMvc.perform(multipart("/api/inquiry"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type")
						.value("https://api.pikume.com/problems/support/invalid-inquiry"))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("필수 문의 요청 값이 없습니다: content"))
				.andExpect(jsonPath("$.instance").value("/api/inquiry"));
	}
}
