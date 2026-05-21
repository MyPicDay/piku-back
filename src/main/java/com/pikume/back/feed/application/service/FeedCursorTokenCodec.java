package com.pikume.back.feed.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.exception.InvalidFeedCursorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class FeedCursorTokenCodec {

	private final ObjectMapper objectMapper;

	public String encode(FeedCursor cursor) {
		try {
			String json = objectMapper.writeValueAsString(cursor);
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(json.getBytes(StandardCharsets.UTF_8));
		} catch (JsonProcessingException e) {
			throw new InvalidFeedCursorException();
		}
	}

	public FeedCursor decode(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}

		try {
			byte[] decoded = Base64.getUrlDecoder().decode(token);
			return objectMapper.readValue(decoded, FeedCursor.class);
		} catch (IllegalArgumentException | IOException e) {
			throw new InvalidFeedCursorException();
		}
	}
}
