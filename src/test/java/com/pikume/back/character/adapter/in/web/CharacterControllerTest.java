package com.pikume.back.character.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.character.application.dto.CharacterResult;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.ProblemDetailFallbackExceptionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

	private MockMvc mockMvc;
	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@BeforeEach
	void setUp() {
		CharacterController characterController = new CharacterController(getCharacterUseCase);
		mockMvc = MockMvcBuilders.standaloneSetup(characterController)
				.setHandlerExceptionResolvers(new ProblemDetailFallbackExceptionResolver(
						new ObjectMapper(),
						problemDetailFactory,
						java.util.Optional.empty()))
				.build();
	}

	@Test
	@DisplayName("GET /api/characters/fixed는 MinIO public URL을 displayImageUrl로 반환한다")
	void getFixedCharactersReturnsStoragePublicUrl() throws Exception {
		given(getCharacterUseCase.getFixedCharacters())
				.willReturn(List.of(new CharacterResult(
						1L,
						null,
						"https://assets.example.com/piku/public/characters/fixed/base_image_1.png",
						CharacterCreationType.FIXED)));

		mockMvc.perform(get("/api/characters/fixed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].displayImageUrl")
						.value("https://assets.example.com/piku/public/characters/fixed/base_image_1.png"))
				.andExpect(jsonPath("$[0].type").value("FIXED"));
	}

	@Test
	@DisplayName("GET /api/characters/fixed/{fileName} 이미지 bytes endpoint는 제공하지 않는다")
	void fixedCharacterImageEndpointIsRemoved() {
		boolean hasFixedImageMapping = Arrays.stream(CharacterController.class.getDeclaredMethods())
				.map(method -> method.getAnnotation(GetMapping.class))
				.filter(Objects::nonNull)
				.flatMap(mapping -> Arrays.stream(mapping.value()))
				.anyMatch(value -> value.startsWith("/fixed/"));

		assertThat(hasFixedImageMapping).isFalse();
	}
}
