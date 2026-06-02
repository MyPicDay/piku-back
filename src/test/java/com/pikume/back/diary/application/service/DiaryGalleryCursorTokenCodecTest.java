package com.pikume.back.diary.application.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.pikume.back.diary.application.dto.DiaryGalleryCursor;
import com.pikume.back.diary.application.exception.InvalidDiaryGalleryCursorException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DiaryGalleryCursorTokenCodec")
class DiaryGalleryCursorTokenCodecTest {

	private final DiaryGalleryCursorTokenCodec codec = new DiaryGalleryCursorTokenCodec(
			JsonMapper.builder().findAndAddModules().build());

	@Test
	@DisplayName("갤러리 cursor를 opaque token으로 인코딩하고 다시 디코딩한다")
	void encodesAndDecodesCursor() {
		DiaryGalleryCursor cursor = new DiaryGalleryCursor(LocalDate.of(2026, 5, 31), 42L);

		String token = codec.encode(cursor);
		DiaryGalleryCursor decoded = codec.decode(token);

		assertThat(token).isNotBlank();
		assertThat(decoded).isEqualTo(cursor);
	}

	@Test
	@DisplayName("빈 cursor token은 첫 페이지로 해석한다")
	void decodesBlankCursorAsFirstPage() {
		assertThat(codec.decode(null)).isNull();
		assertThat(codec.decode(" ")).isNull();
	}

	@Test
	@DisplayName("형식이 잘못된 cursor token은 갤러리 cursor 예외를 던진다")
	void rejectsMalformedCursor() {
		assertThatThrownBy(() -> codec.decode("not-base64"))
				.isInstanceOf(InvalidDiaryGalleryCursorException.class);
	}

	@Test
	@DisplayName("정렬 키가 부족한 cursor token은 갤러리 cursor 예외를 던진다")
	void rejectsCursorMissingSortKey() {
		String token = codec.encode(new DiaryGalleryCursor(LocalDate.of(2026, 5, 31), 0L));

		assertThatThrownBy(() -> codec.decode(token))
				.isInstanceOf(InvalidDiaryGalleryCursorException.class);
	}
}
