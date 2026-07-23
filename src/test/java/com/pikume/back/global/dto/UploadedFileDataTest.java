package com.pikume.back.global.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UploadedFileData")
class UploadedFileDataTest {

	@Test
	@DisplayName("입력 바이트 배열을 방어적으로 복사한다")
	void copiesInputBytes() {
		byte[] source = "image".getBytes(StandardCharsets.UTF_8);
		UploadedFileData file = new UploadedFileData("image.png", "image/png", source);

		source[0] = 'X';

		assertThat(file.bytes()).containsExactly("image".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	@DisplayName("반환 바이트 배열을 변경해도 파일 값은 변하지 않는다")
	void copiesReturnedBytes() {
		UploadedFileData file = new UploadedFileData(
				"image.png",
				"image/png",
				"image".getBytes(StandardCharsets.UTF_8));

		byte[] returned = file.bytes();
		returned[0] = 'X';

		assertThat(file.bytes()).containsExactly("image".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	@DisplayName("바이트가 없으면 빈 파일이며 입력 스트림과 크기도 비어 있다")
	void representsMissingBytesAsEmptyFile() throws Exception {
		UploadedFileData file = new UploadedFileData(null, null, null);

		assertThat(file.originalFilename()).isNull();
		assertThat(file.contentType()).isNull();
		assertThat(file.isEmpty()).isTrue();
		assertThat(file.size()).isZero();
		assertThat(file.inputStream().readAllBytes()).isEmpty();
	}
}
