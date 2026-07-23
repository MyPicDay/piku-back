package com.pikume.back.character.adapter.in.web;

import com.pikume.back.character.application.dto.CharacterResult;
import com.pikume.back.character.application.exception.CharacterErrorCode;
import com.pikume.back.character.application.exception.CharacterException;
import com.pikume.back.character.application.port.in.QueryFixedCharacterCatalogUseCase;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterController")
class CharacterControllerTest {

	@Mock
	private QueryFixedCharacterCatalogUseCase queryFixedCharacterCatalogUseCase;

	@Mock
	private ResolveObjectUrlPort resolveObjectUrlPort;

	private MockMvc mockMvc;
	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@BeforeEach
	void setUp() {
		CharacterWebMapper characterWebMapper = new CharacterWebMapper(resolveObjectUrlPort);
		CharacterController characterController = new CharacterController(
				queryFixedCharacterCatalogUseCase,
				characterWebMapper);
		mockMvc = MockMvcBuilders.standaloneSetup(characterController)
				.setControllerAdvice(new CharacterExceptionHandler(problemDetailFactory))
				.build();
	}

	@Test
	@DisplayName("GET /api/characters/fixed는 MinIO public URL을 displayImageUrl로 반환한다")
	void getFixedCharactersReturnsStoragePublicUrl() throws Exception {
		given(queryFixedCharacterCatalogUseCase.queryFixedCharacters())
				.willReturn(List.of(new CharacterResult(
						1L,
						null,
						"public/characters/fixed/base_image_1.webp",
						CharacterCreationType.FIXED)));
		given(resolveObjectUrlPort.resolveObjectUrl(
				"public/characters/fixed/base_image_1.webp",
				true))
				.willReturn("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");

		mockMvc.perform(get("/api/characters/fixed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].displayImageUrl")
						.value("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp"))
				.andExpect(jsonPath("$[0].type").value("FIXED"));
	}

	@Test
	@DisplayName("카탈로그 조회 실패는 내부 원인을 노출하지 않는 RFC 9457 응답으로 변환한다")
	void getFixedCharactersReturnsProblemDetails() throws Exception {
		given(queryFixedCharacterCatalogUseCase.queryFixedCharacters())
				.willThrow(new CharacterException(
						CharacterErrorCode.CATALOG_UNAVAILABLE,
						new IllegalStateException("secret storage detail")));

		mockMvc.perform(get("/api/characters/fixed"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type")
						.value("https://api.pikume.com/problems/character/catalog-unavailable"))
				.andExpect(jsonPath("$.title").value("Internal Server Error"))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.detail").value("캐릭터 목록을 불러올 수 없습니다."))
				.andExpect(jsonPath("$.instance").value("/api/characters/fixed"))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("secret storage detail"))));
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
