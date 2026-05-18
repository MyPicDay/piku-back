package com.pikume.back.character.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.character.application.exception.FixedCharacterImageNotFoundException;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.character.application.port.in.ManageCharacterUseCase;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.ProblemDetailFallbackExceptionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterController")
class CharacterControllerTest {

	@Mock
	private GetCharacterUseCase getCharacterUseCase;

	@Mock
	private ManageCharacterUseCase manageCharacterUseCase;

	private CharacterController characterController;
	private MockMvc mockMvc;
	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@BeforeEach
	void setUp() {
		characterController = new CharacterController(
				getCharacterUseCase,
				manageCharacterUseCase,
				problemDetailFactory);
		mockMvc = MockMvcBuilders.standaloneSetup(characterController)
				.setHandlerExceptionResolvers(new ProblemDetailFallbackExceptionResolver(
						new ObjectMapper(),
						problemDetailFactory,
						java.util.Optional.empty()))
				.build();
	}

	@Test
	@DisplayName("GET /api/characters/fixed/{fileName}는 이미지가 없으면 404 Problem Details를 반환한다")
	void getFixedCharacterImageReturnsProblemDetailWhenImageDoesNotExist() {
		given(manageCharacterUseCase.getFixedCharacterImage("missing.png"))
				.willThrow(new FixedCharacterImageNotFoundException("missing.png"));

		ResponseEntity<?> response = characterController.getFixedCharacterImage("missing.png");

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/resource-not-found");
		assertThat(problemDetail.getStatus()).isEqualTo(404);
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/characters/fixed/missing.png");
	}

	@Test
	@DisplayName("GET /api/characters/fixed/{fileName}는 읽기 실패면 500 Problem Details를 반환한다")
	void getFixedCharacterImageReturnsInternalServerErrorWhenReadFails() throws Exception {
		given(manageCharacterUseCase.getFixedCharacterImage("broken.png"))
				.willThrow(new RuntimeException("storage failure"));

		mockMvc.perform(get("/api/characters/fixed/broken.png"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/common/internal-server-error"))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.instance").value("/api/characters/fixed/broken.png"));
	}
}
