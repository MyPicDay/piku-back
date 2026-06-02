package com.pikume.back.diary.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiaryGalleryCursor;
import com.pikume.back.diary.application.exception.InvalidDiaryGalleryCursorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class DiaryGalleryCursorTokenCodec {

	private final ObjectMapper objectMapper;

	public String encode(DiaryGalleryCursor cursor) {
		try {
			String json = objectMapper.writeValueAsString(cursor);
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(json.getBytes(StandardCharsets.UTF_8));
		} catch (JsonProcessingException e) {
			throw new InvalidDiaryGalleryCursorException();
		}
	}

	public DiaryGalleryCursor decode(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}

		try {
			byte[] decoded = Base64.getUrlDecoder().decode(token);
			DiaryGalleryCursor cursor = objectMapper.readValue(decoded, DiaryGalleryCursor.class);
			validate(cursor);
			return cursor;
		} catch (IllegalArgumentException | IOException e) {
			throw new InvalidDiaryGalleryCursorException();
		}
	}

	private void validate(DiaryGalleryCursor cursor) {
		if (cursor == null || cursor.date() == null || cursor.diaryId() == null || cursor.diaryId() <= 0) {
			throw new InvalidDiaryGalleryCursorException();
		}
	}
}
