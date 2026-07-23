package com.pikume.back.global.storage;

import com.pikume.back.global.dto.UploadedFileData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("중립 S3 Object Storage Adapter")
class S3ObjectStorageAdapterTest {

	@Mock
	private S3Client s3Client;

	@Test
	@DisplayName("소비자가 공개로 지정한 객체는 클라이언트 S3 URL로 해석한다")
	void resolvesPublicObjectFromClientBaseUrl() {
		S3ObjectStorageAdapter adapter = adapter();

		String result = adapter.resolveObjectUrl("public/images/avatar.webp", true);

		assertThat(result).isEqualTo(
				"https://assets.example.com/piku/public/images/avatar.webp");
	}

	@Test
	@DisplayName("소비자가 비공개로 지정한 객체는 MinIO presigned URL로 해석한다")
	void resolvesPrivateObjectAsPresignedUrl() {
		S3ObjectStorageAdapter adapter = adapter();

		String result = adapter.resolveObjectUrl("private/images/diary.webp", false);

		assertThat(URI.create(result).getHost()).isEqualTo("assets.example.com");
		assertThat(result).contains("/piku/private/images/diary.webp");
		assertThat(result).contains("X-Amz-Signature=");
	}

	@Test
	@DisplayName("URL 해석 실패는 기존 계약대로 null로 처리한다")
	void returnsNullWhenUrlCannotBeResolved() {
		S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(
				s3Client,
				new StorageProperties(
						"minio",
						"",
						"",
						"ap-northeast-2",
						"access-key",
						"secret-key",
						"piku"));

		assertThat(adapter.resolveObjectUrl("public/images/avatar.webp", true)).isNull();
	}

	@Test
	@DisplayName("중립 파일 값과 Cache-Control로 객체를 저장한다")
	void storesObjectWithTechnicalMetadata() {
		S3ObjectStorageAdapter adapter = adapter();
		UploadedFileData file = new UploadedFileData(
				"avatar.webp",
				"image/webp",
				"image".getBytes(StandardCharsets.UTF_8));

		String result = adapter.storeObject(
				file,
				"public/images/avatar.webp",
				"public, max-age=300");

		assertThat(result).isEqualTo("public/images/avatar.webp");
		then(s3Client).should().putObject(
				argThat((PutObjectRequest request) ->
						request != null
								&& "piku".equals(request.bucket())
								&& "public/images/avatar.webp".equals(request.key())
								&& "image/webp".equals(request.contentType())
								&& "public, max-age=300".equals(request.cacheControl())),
				any(RequestBody.class));
	}

	@Test
	@DisplayName("Object Key로 저장된 bytes를 읽는다")
	void loadsObjectBytes() {
		S3ObjectStorageAdapter adapter = adapter();
		byte[] bytes = "image".getBytes(StandardCharsets.UTF_8);
		given(s3Client.getObjectAsBytes(argThat((GetObjectRequest request) ->
				request != null && "private/images/diary.webp".equals(request.key()))))
				.willReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes));

		assertThat(adapter.loadObject("private/images/diary.webp")).isEqualTo(bytes);
	}

	private S3ObjectStorageAdapter adapter() {
		return new S3ObjectStorageAdapter(
				s3Client,
				new StorageProperties(
						"minio",
						"http://minio:9000",
						"https://assets.example.com",
						"ap-northeast-2",
						"access-key",
						"secret-key",
						"piku"));
	}
}
