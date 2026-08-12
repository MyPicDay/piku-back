package com.pikume.back.diary.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Diary generated OpenAPI")
class DiaryOpenApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("생성된 일기 등록 스키마는 필수 diary와 선택 photos·imageInfos를 표현한다")
	void generatedCreateSchemaDocumentsOptionalPhotos() throws Exception {
		JsonNode document = generatedOpenApi();

		JsonNode createOperation = document.path("paths")
				.path("/api/diary")
				.path("post");
		JsonNode requestBody = createOperation.path("requestBody");
		assertThat(requestBody.path("required").asBoolean()).isTrue();

		JsonNode multipartSchema = resolveReference(document, requestBody
				.path("content")
				.path("multipart/form-data")
				.path("schema"));
		assertThat(fieldNames(multipartSchema.path("properties")))
				.contains("diary", "photos");
		assertThat(textValues(multipartSchema.path("required")))
				.contains("diary")
				.doesNotContain("photos");

		JsonNode diarySchema = resolveReference(document, multipartSchema.path("properties").path("diary"));
		assertThat(textValues(diarySchema.path("required")))
				.contains("status", "content", "date")
				.doesNotContain("imageInfos");
		assertThat(schemaTypes(diarySchema.path("properties").path("imageInfos")))
				.containsExactlyInAnyOrder("array", "null");
	}

	@Test
	@DisplayName("생성된 일기 등록 응답은 201과 기존 응답 필드를 표현한다")
	void generatedCreateResponseDocumentsCreatedStatus() throws Exception {
		JsonNode document = generatedOpenApi();

		JsonNode responses = document.path("paths")
				.path("/api/diary")
				.path("post")
				.path("responses");
		assertThat(fieldNames(responses))
				.contains("201")
				.doesNotContain("200");

		JsonNode createdSchema = resolveReference(document, responses.path("201")
				.path("content")
				.path("application/json")
				.path("schema"));
		assertThat(fieldNames(createdSchema.path("properties")))
				.contains("diaryId", "content");
	}

	private JsonNode generatedOpenApi() throws Exception {
		String response = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response);
	}

	private JsonNode resolveReference(JsonNode document, JsonNode schema) {
		if (!schema.hasNonNull("$ref")) {
			return schema;
		}
		String reference = schema.path("$ref").asText();
		String schemaName = reference.substring(reference.lastIndexOf('/') + 1);
		return document.path("components").path("schemas").path(schemaName);
	}

	private Iterable<String> fieldNames(JsonNode node) {
		return () -> node.fieldNames();
	}

	private Iterable<String> textValues(JsonNode node) {
		return () -> StreamSupport.stream(node.spliterator(), false)
				.map(JsonNode::asText)
				.iterator();
	}

	private List<String> schemaTypes(JsonNode schema) {
		JsonNode type = schema.path("type");
		if (type.isArray()) {
			return StreamSupport.stream(type.spliterator(), false)
					.map(JsonNode::asText)
					.toList();
		}
		return type.isTextual() ? List.of(type.asText()) : List.of();
	}
}
