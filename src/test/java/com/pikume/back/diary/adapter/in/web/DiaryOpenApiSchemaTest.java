package com.pikume.back.diary.adapter.in.web;

import com.pikume.back.diary.adapter.in.web.dto.DiaryDTO;
import com.pikume.back.security.principal.UserPrincipal;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestPart;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Diary OpenAPI schema")
class DiaryOpenApiSchemaTest {

	@Test
	@DisplayName("일기 등록은 필수 diary와 선택 photos 파트를 문서화한다")
	void createDiaryDocumentsRequiredAndOptionalMultipartParts() throws NoSuchMethodException {
		Method method = DiaryController.class.getDeclaredMethod(
				"createDiary",
				DiaryDTO.class,
				List.class,
				UserPrincipal.class);
		java.lang.reflect.Parameter diaryParameter = method.getParameters()[0];
		java.lang.reflect.Parameter photosParameter = method.getParameters()[1];

		assertThat(diaryParameter.getAnnotation(RequestPart.class).required()).isTrue();
		assertThat(diaryParameter.getAnnotation(Parameter.class).required()).isTrue();
		assertThat(photosParameter.getAnnotation(RequestPart.class).required()).isFalse();
		assertThat(photosParameter.getAnnotation(Parameter.class)).satisfies(parameter -> {
			assertThat(parameter.required()).isFalse();
			assertThat(parameter.description()).contains("선택", "생략", "빈 파일", "거부");
		});
		assertThat(method.getAnnotation(Operation.class).description()).contains("사진 없이");
	}

	@Test
	@DisplayName("DiaryDTO imageInfos는 선택 가능한 nullable 목록으로 문서화한다")
	void imageInfosIsOptionalAndNullable() {
		Map<String, Schema> schemas = ModelConverters.getInstance().readAll(DiaryDTO.class);
		Schema<?> diarySchema = schemas.get("DiaryDTO");
		Schema<?> imageInfos = (Schema<?>) diarySchema.getProperties().get("imageInfos");

		assertThat(diarySchema.getRequired())
				.contains("status", "content", "date")
				.doesNotContain("imageInfos");
		assertThat(imageInfos.getNullable()).isTrue();
		assertThat(imageInfos.getDescription()).contains("생략", "빈 목록", "사진 없이");
	}
}
