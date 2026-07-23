package com.pikume.back.creative.adapter.out.storage;

import com.pikume.back.creative.application.exception.CreativeException;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.global.port.out.StoreObjectPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreativeImageStorageAdapter")
class CreativeImageStorageAdapterTest {

	@Mock
	private StoreObjectPort storeObjectPort;

	@Mock
	private ResolveObjectUrlPort resolveObjectUrlPort;

	@Test
	@DisplayName("생성 이미지를 Creative Object Key와 private cache 정책으로 저장한다")
	void storesGeneratedImageWithCreativePolicy() {
		given(storeObjectPort.storeObject(
				any(UploadedFileData.class),
				any(String.class),
				eq("private, no-store, max-age=0")))
				.willAnswer(invocation -> invocation.getArgument(1));
		CreativeImageStorageAdapter adapter = adapter();

		String objectKey = adapter.storeGeneratedImage(
				Base64.getEncoder().encodeToString("image".getBytes(StandardCharsets.UTF_8)),
				"user-1",
				"png");

		assertThat(objectKey)
				.matches("private/diary-images/ai/[0-9a-f]{2}/[0-9a-f]{2}/[0-9a-f]{32}\\.png");
		then(storeObjectPort).should().storeObject(
				argThat(file -> file.contentType().equals("image/png")
						&& java.util.Arrays.equals(file.bytes(), "image".getBytes(StandardCharsets.UTF_8))),
				eq(objectKey),
				eq("private, no-store, max-age=0"));
	}

	@Test
	@DisplayName("잘못된 Base64 입력은 기술 원인을 숨긴 Creative 저장 오류로 변환한다")
	void translatesInvalidBase64() {
		assertThatThrownBy(() -> adapter().storeGeneratedImage("not-base64", "user-1", "png"))
				.isInstanceOf(CreativeException.class)
				.hasMessage("생성 이미지를 저장할 수 없습니다.");
	}

	@Test
	@DisplayName("생성 이미지 URL 해석은 중립 Object URL 계약에 위임한다")
	void resolvesGeneratedImageUrl() {
		given(resolveObjectUrlPort.resolveObjectUrl("private/key.png", false))
				.willReturn("https://assets.example.com/private/key.png");

		assertThat(adapter().resolveGeneratedImageUrl("private/key.png", false))
				.isEqualTo("https://assets.example.com/private/key.png");
	}

	private CreativeImageStorageAdapter adapter() {
		return new CreativeImageStorageAdapter(
				storeObjectPort,
				resolveObjectUrlPort,
				new CreativeImageObjectKeyPolicy());
	}
}
