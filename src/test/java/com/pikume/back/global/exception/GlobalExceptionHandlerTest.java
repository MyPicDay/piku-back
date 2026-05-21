package com.pikume.back.global.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
				.setControllerAdvice(new GlobalExceptionHandler(Optional.empty(), new com.pikume.back.global.error.ProblemDetailFactory()))
				.setValidator(validator)
				.build();
	}

	@Test
	@DisplayName("validation 실패는 validation type과 fieldErrors를 반환한다")
	void validationFailure() throws Exception {
		mockMvc.perform(post("/test/validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.fieldErrors.content").exists());
	}

	@Test
	@DisplayName("malformed body는 common malformed-request로 변환된다")
	void malformedBody() throws Exception {
		mockMvc.perform(post("/test/validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/common/malformed-request"))
				.andExpect(jsonPath("$.status").value(400));
	}

	@RestController
	static class TestController {

			@PostMapping("/test/validation")
			void validation(@Valid @RequestBody TestRequest request) {
			}
	}

	static class TestRequest {
		@NotBlank(message = "비어 있을 수 없습니다.")
		private final String content;

		@JsonCreator
		TestRequest(String content) {
			this.content = content;
		}

		public String getContent() {
			return content;
		}
	}
}
