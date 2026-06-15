package com.pikume.back.diary.adapter.out.storage;

import com.pikume.back.diary.application.port.out.SaveDiaryPort;
import com.pikume.back.diary.adapter.out.cache.ImageCacheProperties;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.storage.StorageProperties;
import com.pikume.back.global.util.FileUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.ResponseBytes;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

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
	private static final String PUBLIC_CACHE_CONTROL = "public, max-age=300, s-maxage=1200";
	private static final String PRIVATE_CACHE_CONTROL = "private, no-store, max-age=0";

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

		String url = adapter.getPhotoUrl("private/diary-images/ai/ab/cd/generated.png", false);

		assertThat(url).isNotNull();
		assertThat(URI.create(url).getHost()).isEqualTo("localhost");
		assertThat(URI.create(url).getPort()).isEqualTo(19000);
		assertThat(url).contains("/piku/private/diary-images/ai/ab/cd/generated.png");
		assertThat(url).contains("X-Amz-Signature=");
	}

	@Test
	@DisplayName("URL 공개 여부는 호출 인자가 아니라 object key prefix로 판단한다")
	void determinesPublicUrlByObjectKeyPrefix() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");

		String url = adapter.getPhotoUrl("public/diary-images/user/ab/cd/cover.png", false);

		assertThat(url).isEqualTo("https://assets.example.com/piku/public/diary-images/user/ab/cd/cover.png");
	}

	@Test
	@DisplayName("object key로 storage object bytes를 읽는다")
	void loadsObjectBytesByObjectKey() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		byte[] bytes = "fixed-character".getBytes(StandardCharsets.UTF_8);
		given(s3Client.getObjectAsBytes(getObjectWithKey("public/characters/fixed/base_image_1.webp")))
				.willReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes));

		byte[] result = adapter.loadObject("public/characters/fixed/base_image_1.webp");

		assertThat(result).isEqualTo(bytes);
	}

	@Test
	@DisplayName("object read 중 S3 404는 객체 없음으로 처리한다")
	void treatsS3NotFoundAsMissingObjectWhenLoadingObject() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		given(s3Client.getObjectAsBytes(getObjectWithKey("public/characters/fixed/missing.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

		assertThatThrownBy(() -> adapter.loadObject("public/characters/fixed/missing.png"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("스토리지 객체를 찾을 수 없습니다")
				.hasCauseInstanceOf(S3Exception.class);
	}

	@Test
	@DisplayName("public WebP 객체 저장 시 앱 기본 public cache-control을 지정한다")
	void storesPublicWebpObjectWithDefaultCacheControl() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		UploadedFileData webp = new UploadedFileData("photo.webp", "image/webp", "webp".getBytes(StandardCharsets.UTF_8));

		String storedKey = adapter.storeObject(
				webp,
				"public/diary-images/user/ab/cd/photo.webp",
				null);

		assertThat(storedKey).isEqualTo("public/diary-images/user/ab/cd/photo.webp");
		then(s3Client).should().putObject(putObjectWithKeyAndCacheControl(
				"public/diary-images/user/ab/cd/photo.webp",
				"image/webp",
				PUBLIC_CACHE_CONTROL), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	@Test
	@DisplayName("공개 일기 사용자 이미지는 대표 여부와 무관하게 public sharded key로 저장한다")
	void savesPublicDiaryUserPhotoUnderPublicShard() throws IOException {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		Diary diary = new Diary("내용", DiaryVisibility.PUBLIC, LocalDate.now(), "user-1");
		UploadedFileData image = new UploadedFileData("photo.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));
		String objectKey = "public/diary-images/user/ab/cd/photo.png";
		given(photoUtil.generateDiaryUserImageObjectKey(true, "photo.png")).willReturn(objectKey);

		adapter.savePhoto(diary, image, "user-1", 1);

		then(s3Client).should().putObject(
				putObjectWithKeyAndCacheControl(objectKey, "image/png", PUBLIC_CACHE_CONTROL),
				any(software.amazon.awssdk.core.sync.RequestBody.class));
		then(saveDiaryPort).should().savePhoto(argThat(photo -> objectKey.equals(photo.getUrl())
				&& !Boolean.TRUE.equals(photo.getRepresent())
				&& photo.getSourceType() == DiaryPhotoType.USER_IMAGE));
	}

	@Test
	@DisplayName("비공개 일기 대표 이미지는 private sharded key로 저장한다")
	void savesPrivateDiaryRepresentPhotoUnderPrivateShard() throws IOException {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		Diary diary = new Diary("내용", DiaryVisibility.PRIVATE, LocalDate.now(), "user-1");
		UploadedFileData image = new UploadedFileData("photo.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));
		String objectKey = "private/diary-images/user/ab/cd/photo.png";
		given(photoUtil.generateDiaryUserImageObjectKey(false, "photo.png")).willReturn(objectKey);

		adapter.savePhoto(diary, image, "user-1", 0);

		then(s3Client).should().putObject(
				putObjectWithKeyAndCacheControl(objectKey, "image/png", PRIVATE_CACHE_CONTROL),
				any(software.amazon.awssdk.core.sync.RequestBody.class));
		then(saveDiaryPort).should().savePhoto(argThat(photo -> objectKey.equals(photo.getUrl())
				&& Boolean.TRUE.equals(photo.getRepresent())
				&& photo.getSourceType() == DiaryPhotoType.USER_IMAGE));
	}

	@Test
	@DisplayName("AI 임시 이미지는 private sharded key로 저장한다")
	void savesAiTemporaryPhotoUnderPrivateShard() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		String objectKey = "private/diary-images/ai/ab/cd/generated.png";
		given(fileUtil.cleanExtension("png")).willReturn("png");
		given(fileUtil.decodeBase64("base64")).willReturn("image".getBytes(StandardCharsets.UTF_8));
		given(fileUtil.getContentType("png")).willReturn("image/png");
		given(photoUtil.generateDiaryAiImageObjectKey("png")).willReturn(objectKey);

		String result = adapter.saveAIPhoto("base64", "user-1", "png");

		assertThat(result).isEqualTo(objectKey);
		then(s3Client).should().putObject(
				putObjectWithKeyAndCacheControl(objectKey, "image/png", PRIVATE_CACHE_CONTROL),
				any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	@Test
	@DisplayName("대표 AI 이미지를 public 경로로 복사한 뒤 원본을 삭제한다")
	void movesSourceObjectToPublicPath() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
				.contentType("image/png")
				.build();

		given(photoUtil.publicObjectKeyFor("private/ai.png")).willReturn("public/ai.png");
		given(s3Client.headObject(headObjectWithKey("public/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build())
				.willReturn(headObjectResponse);
		given(s3Client.headObject(headObjectWithKey("private/ai.png")))
				.willReturn(headObjectResponse);

		String movedKey = adapter.moveToPublic("private/ai.png");

		assertThat(movedKey).isEqualTo("public/ai.png");
		then(s3Client).should().copyObject(copyObjectFromToWithCacheControl(
				"private/ai.png",
				"public/ai.png",
				PUBLIC_CACHE_CONTROL,
				"image/png"));
		then(s3Client).should().deleteObject(deleteObjectWithKey("private/ai.png"));
	}

	@Test
	@DisplayName("private scope로 복사할 때 no-store cache-control을 지정한다")
	void copiesSourceObjectToPrivateScopeWithNoStoreCacheControl() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");
		HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
				.contentType("image/jpeg")
				.build();

		given(photoUtil.visibilityObjectKeyFor("public/diary-images/user/ab/cd/photo.jpg", false, DiaryPhotoType.USER_IMAGE))
				.willReturn("private/diary-images/user/ab/cd/photo.jpg");
		given(s3Client.headObject(headObjectWithKey("private/diary-images/user/ab/cd/photo.jpg")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build())
				.willReturn(headObjectResponse);
		given(s3Client.headObject(headObjectWithKey("public/diary-images/user/ab/cd/photo.jpg")))
				.willReturn(headObjectResponse);

		String copiedKey = adapter.copyToVisibilityScope(
				"public/diary-images/user/ab/cd/photo.jpg",
				DiaryVisibility.PRIVATE,
				DiaryPhotoType.USER_IMAGE);

		assertThat(copiedKey).isEqualTo("private/diary-images/user/ab/cd/photo.jpg");
		then(s3Client).should().copyObject(copyObjectFromToWithCacheControl(
				"public/diary-images/user/ab/cd/photo.jpg",
				"private/diary-images/user/ab/cd/photo.jpg",
				PRIVATE_CACHE_CONTROL,
				"image/jpeg"));
	}

	@Test
	@DisplayName("HeadObject 403은 객체 없음으로 처리하지 않고 이동 실패로 전파한다")
	void propagatesForbiddenHeadObjectInsteadOfTreatingItAsMissing() {
		MinioPhotoStorageAdapter adapter = adapterWith(
				"http://minio:9000",
				"https://assets.example.com");

		given(photoUtil.publicObjectKeyFor("private/ai.png")).willReturn("public/ai.png");
		given(s3Client.headObject(headObjectWithKey("public/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());
		given(s3Client.headObject(headObjectWithKey("private/ai.png")))
				.willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());

		assertThatThrownBy(() -> adapter.moveToPublic("private/ai.png"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("파일 복제 중 오류가 발생했습니다.")
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

		given(photoUtil.publicObjectKeyFor("private/ai.png")).willReturn("public/ai.png");
		given(s3Client.headObject(headObjectWithKey("public/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());
		given(s3Client.headObject(headObjectWithKey("private/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

		assertThatThrownBy(() -> adapter.moveToPublic("private/ai.png"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("파일 복제 중 오류가 발생했습니다.")
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

		given(photoUtil.publicObjectKeyFor("private/ai.png")).willReturn("public/ai.png");
		given(s3Client.headObject(headObjectWithKey("public/ai.png")))
				.willThrow(S3Exception.builder().statusCode(404).message("Not Found").build())
				.willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build())
				.willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());
		given(s3Client.headObject(headObjectWithKey("private/ai.png")))
				.willReturn(headObjectResponse);

		assertThatThrownBy(() -> adapter.moveToPublic("private/ai.png"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("파일 복제 중 오류가 발생했습니다.")
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
		return new MinioPhotoStorageAdapter(s3Client, photoUtil, saveDiaryPort, properties, new ImageCacheProperties(), fileUtil);
	}

	private HeadObjectRequest headObjectWithKey(String key) {
		return argThat((HeadObjectRequest request) -> request != null && key.equals(request.key()));
	}

	private CopyObjectRequest copyObjectFromToWithCacheControl(
			String sourceKey,
			String destinationKey,
			String cacheControl,
			String contentType) {
		return argThat((CopyObjectRequest request) -> request != null
				&& sourceKey.equals(request.sourceKey())
				&& destinationKey.equals(request.destinationKey())
				&& cacheControl.equals(request.cacheControl())
				&& contentType.equals(request.contentType())
				&& request.metadataDirective() == software.amazon.awssdk.services.s3.model.MetadataDirective.REPLACE);
	}

	private GetObjectRequest getObjectWithKey(String key) {
		return argThat((GetObjectRequest request) -> request != null && key.equals(request.key()));
	}

	private DeleteObjectRequest deleteObjectWithKey(String key) {
		return argThat((DeleteObjectRequest request) -> request != null && key.equals(request.key()));
	}

	private PutObjectRequest putObjectWithKeyAndCacheControl(String key, String contentType, String cacheControl) {
		return argThat((PutObjectRequest request) -> request != null
				&& key.equals(request.key())
				&& contentType.equals(request.contentType())
				&& cacheControl.equals(request.cacheControl()));
	}
}
