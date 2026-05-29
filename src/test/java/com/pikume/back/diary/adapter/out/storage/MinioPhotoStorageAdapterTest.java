package com.pikume.back.diary.adapter.out.storage;

import com.pikume.back.diary.application.port.out.SaveDiaryPort;
import com.pikume.back.global.util.FileUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinioPhotoStorageAdapter")
class MinioPhotoStorageAdapterTest {

	private static final String REGION = "ap-northeast-2";
	private static final String ACCESS_KEY = "test-access-key";
	private static final String SECRET_KEY = "test-secret-key";
	private static final String BUCKET = "piku";

	@Mock
	private S3Client s3Client;

	@Mock
	private PhotoUtil photoUtil;

	@Mock
	private SaveDiaryPort saveDiaryPort;

	@Mock
	private FileUtil fileUtil;

	@Test
	@DisplayName("public 객체 URL은 서버 S3 API 경로가 아니라 클라이언트 S3 이미지 조회 경로를 사용한다")
	void returnsPublicObjectUrlFromClientToS3Url() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");

		String url = adapter.getPhotoUrl("public/user-1/cover.png", true);

		assertThat(url).isEqualTo("https://assets.example.com/piku/public/user-1/cover.png");
	}

	@Test
	@DisplayName("클라이언트 S3 이미지 조회 경로 끝의 slash를 정규화한다")
	void normalizesClientToS3UrlTrailingSlash() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com/");

		String url = adapter.getPhotoUrl("public/user-1/cover.png", true);

		assertThat(url).isEqualTo("https://assets.example.com/piku/public/user-1/cover.png");
	}

	@Test
	@DisplayName("클라이언트 S3 이미지 조회 경로가 없으면 서버 S3 API 경로로 fallback한다")
	void fallsBackToServerToS3UrlWhenClientToS3UrlIsBlank() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://localhost:9000",
				"");

		String url = adapter.getPhotoUrl("public/user-1/cover.png", true);

		assertThat(url).isEqualTo("http://localhost:9000/piku/public/user-1/cover.png");
	}

	@Test
	@DisplayName("private presigned URL도 클라이언트 S3 이미지 조회 경로 host로 생성한다")
	void returnsPrivatePresignedUrlFromClientToS3Url() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"http://localhost:19000");

		String url = adapter.getPhotoUrl("user-1/generated.png", false);

		assertThat(url).isNotNull();
		assertThat(URI.create(url).getHost()).isEqualTo("localhost");
		assertThat(URI.create(url).getPort()).isEqualTo(19000);
		assertThat(url).contains("/piku/user-1/generated.png");
		assertThat(url).contains("X-Amz-Signature=");
	}

	@Test
	@DisplayName("대표 AI 이미지를 public 경로로 복사한 뒤 원본을 삭제한다")
	void movesSourceObjectToPublicPath() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		HeadObjectResponse headObjectResponse = HeadObjectResponse.builder().build();

		given(s3Client.headObject(headObjectWithKey("public/private/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build())
				.willReturn(headObjectResponse);
		given(s3Client.headObject(headObjectWithKey("private/ai.png")))
				.willReturn(headObjectResponse);

		String movedKey = adapter.moveToPublic("private/ai.png");

		assertThat(movedKey).isEqualTo("public/private/ai.png");
		then(s3Client).should().copyObject(copyObjectFromTo("private/ai.png", "public/private/ai.png"));
		then(s3Client).should().deleteObject(deleteObjectWithKey("private/ai.png"));
	}

	@Test
	@DisplayName("HeadObject 403은 객체 없음으로 처리하지 않고 이동 실패로 전파한다")
	void propagatesForbiddenHeadObjectInsteadOfTreatingItAsMissing() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");

		given(s3Client.headObject(headObjectWithKey("public/private/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());
		given(s3Client.headObject(headObjectWithKey("private/ai.png")))
				.willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());

		assertThatThrownBy(() -> adapter.moveToPublic("private/ai.png"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("파일 이동 중 오류가 발생했습니다.")
				.hasCauseInstanceOf(S3Exception.class);

		then(s3Client).should(never()).copyObject(any(CopyObjectRequest.class));
		then(s3Client).should(never()).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("source 객체가 실제로 없으면 복사하지 않는다")
	void doesNotCopyWhenSourceObjectIsMissing() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");

		given(s3Client.headObject(headObjectWithKey("public/private/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());
		given(s3Client.headObject(headObjectWithKey("private/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

		assertThatThrownBy(() -> adapter.moveToPublic("private/ai.png"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("파일 이동 중 오류가 발생했습니다.")
				.hasRootCauseMessage("소스 파일을 찾을 수 없습니다: private/ai.png");

		then(s3Client).should(never()).copyObject(any(CopyObjectRequest.class));
		then(s3Client).should(never()).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("복사 후 확인이 S3 오류로 실패해도 롤백 검사 예외가 이동 실패 예외를 덮지 않는다")
	void preservesMoveFailureWhenRollbackExistenceCheckFails() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		HeadObjectResponse headObjectResponse = HeadObjectResponse.builder().build();

		given(s3Client.headObject(headObjectWithKey("public/private/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build())
				.willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build())
				.willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());
		given(s3Client.headObject(headObjectWithKey("private/ai.png")))
				.willReturn(headObjectResponse);

		assertThatThrownBy(() -> adapter.moveToPublic("private/ai.png"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("파일 이동 중 오류가 발생했습니다.")
				.hasCauseInstanceOf(S3Exception.class);
	}

	private MinioPhotoStorageAdapter adapterWith(String serverToS3Url, String clientToS3Url) {
		StorageProperties properties = new StorageProperties(
				"minio",
				serverToS3Url,
				clientToS3Url,
				REGION,
				ACCESS_KEY,
				SECRET_KEY,
				BUCKET);
		return new MinioPhotoStorageAdapter(s3Client, photoUtil, saveDiaryPort, properties, fileUtil);
	}

	private HeadObjectRequest headObjectWithKey(String key) {
		return argThat((HeadObjectRequest request) -> request != null && key.equals(request.key()));
	}

	private CopyObjectRequest copyObjectFromTo(String sourceKey, String destinationKey) {
		return argThat((CopyObjectRequest request) -> request != null
				&& sourceKey.equals(request.sourceKey())
				&& destinationKey.equals(request.destinationKey()));
	}

	private DeleteObjectRequest deleteObjectWithKey(String key) {
		return argThat((DeleteObjectRequest request) -> request != null && key.equals(request.key()));
	}
}
