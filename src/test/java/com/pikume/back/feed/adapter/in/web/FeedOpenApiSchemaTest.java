package com.pikume.back.feed.adapter.in.web;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.pikume.back.feed.adapter.in.web.dto.FeedCursorPageResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Feed OpenAPI schema")
class FeedOpenApiSchemaTest {

	@Test
	@DisplayName("피드 Cursor 페이지의 items는 FeedDiaryResponse 배열로 문서화한다")
	void cursorPageItemsReferenceFeedDiaryResponse() {
		Map<String, Schema> schemas = ModelConverters.getInstance().read(FeedCursorPageResponse.class);
		Schema<?> pageSchema = schemas.get("FeedCursorPageResponse");

		assertThat(pageSchema).isNotNull();
		assertThat(pageSchema.getProperties().get("items"))
				.isInstanceOfSatisfying(ArraySchema.class,
						items -> assertThat(items.getItems().get$ref())
								.endsWith("/FeedDiaryResponse"));
	}
}
