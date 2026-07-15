package com.pikume.back.diary.adapter.out.storage;

import com.pikume.back.diary.adapter.out.cache.ImageCacheProperties;
import com.pikume.back.diary.application.dto.DiaryPhotoUpload;
import com.pikume.back.diary.application.port.out.LoadDiaryPhotoObjectPort;
import com.pikume.back.diary.application.port.out.RelocateDiaryPhotoPort;
import com.pikume.back.diary.application.port.out.ResolveDiaryPhotoUrlPort;
import com.pikume.back.diary.application.port.out.StoreDiaryPhotoPort;
import com.pikume.back.diary.application.port.out.StoreOptimizedDiaryPhotoPort;
import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.storage.StorageProperties;
import com.pikume.back.global.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import static com.pikume.back.diary.adapter.out.storage.PhotoObjectKeyConstants.PUBLIC_PREFIX;

@Slf4j
@Component
public class MinioPhotoStorageAdapter implements StoreDiaryPhotoPort, RelocateDiaryPhotoPort,
		ResolveDiaryPhotoUrlPort, LoadDiaryPhotoObjectPort, StoreOptimizedDiaryPhotoPort {

	private final S3Client s3Client;
	private final PhotoUtil photoUtil;
	private final StorageProperties storageProperties;
	private final ImageCacheProperties imageCacheProperties;
	private final FileUtil fileUtil;

	public MinioPhotoStorageAdapter(S3Client s3Client, PhotoUtil photoUtil,
			StorageProperties storageProperties, ImageCacheProperties imageCacheProperties, FileUtil fileUtil) {
		this.s3Client = s3Client;
		this.photoUtil = photoUtil;
		this.storageProperties = storageProperties;
		this.imageCacheProperties = imageCacheProperties;
		this.fileUtil = fileUtil;
	}

	@Override
	public String store(DiaryPhotoUpload photo, DiaryVisibility visibility) {
		try {
			byte[] bytes = photo.bytes();
			if (bytes.length == 0) {
				throw new IllegalArgumentException("빈 사진은 저장할 수 없습니다.");
			}
			String objectKey = photoUtil.generateDiaryUserImageObjectKey(
					visibility.isPublicStorageScope(),
					photo.originalFilename());
			ensureBucketExists(storageProperties.getBucket());
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectKey)
					.contentType(photo.contentType())
					.cacheControl(cacheControlFor(objectKey))
					.contentLength((long) bytes.length)
					.build();
			s3Client.putObject(request, RequestBody.fromBytes(bytes));
			return objectKey;
		} catch (RuntimeException exception) {
			throw new RuntimeException("일기 사진 저장 중 오류가 발생했습니다.", exception);
		}
	}

	public String storeObject(String contentType, byte[] bytes, String objectKey, String cacheControl) {
		try {
			if (bytes == null || bytes.length == 0) {
				throw new IllegalArgumentException("빈 이미지는 저장할 수 없습니다.");
			}
			ensureBucketExists(storageProperties.getBucket());

			PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectKey)
					.contentType(contentType)
					.contentLength((long) bytes.length);

			String effectiveCacheControl = hasText(cacheControl) ? cacheControl : cacheControlFor(objectKey);
			if (hasText(effectiveCacheControl)) {
				requestBuilder.cacheControl(effectiveCacheControl);
			}

			PutObjectRequest putObjectRequest = requestBuilder.build();

			s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

			return objectKey;
		} catch (Exception e) {
			throw new RuntimeException("이미지 업로드 중 오류 발생", e);
		}
	}

	public byte[] loadObject(String objectKey) {
		try {
			GetObjectRequest request = GetObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectKey)
					.build();

			ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
			return objectBytes.asByteArray();
		} catch (NoSuchKeyException e) {
			throw new RuntimeException("스토리지 객체를 찾을 수 없습니다: " + objectKey, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				throw new RuntimeException("스토리지 객체를 찾을 수 없습니다: " + objectKey, e);
			}
			log.warn("event=storage_object_load_failed outcome=failed key={} status={} reason={}",
					objectKey,
					e.statusCode(),
					e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
			throw new RuntimeException("스토리지 객체를 읽는 중 오류가 발생했습니다.", e);
		}
	}

	@Override
	public byte[] load(String objectKey) {
		return loadObject(objectKey);
	}

	@Override
	public void store(String objectKey, String contentType, byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("빈 최적화 이미지는 저장할 수 없습니다.");
		}
		ensureBucketExists(storageProperties.getBucket());
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(storageProperties.getBucket())
				.key(objectKey)
				.contentType(contentType)
				.cacheControl(cacheControlFor(objectKey))
				.contentLength((long) bytes.length)
				.build();
		s3Client.putObject(request, RequestBody.fromBytes(bytes));
	}

	private void ensureBucketExists(String bucketName) {
		try {
			HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
					.bucket(bucketName)
					.build();
			s3Client.headBucket(headBucketRequest);
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				log.warn("버킷이 존재하지 않아 새로 생성합니다: {}", bucketName);
				CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
						.bucket(bucketName)
						.build();
				s3Client.createBucket(createBucketRequest);
			} else {
				log.error("버킷 확인 중 오류 발생: {} - {}", e.statusCode(), e.awsErrorDetails().errorMessage());
				throw e;
			}
		}
	}

	public String getMinIOStoragePhotoUrl(String objectName, boolean isPublic) {
		String clientToS3BaseUrl = storageProperties.clientToS3BaseUrl();
		if (isPublic) {
			return clientToS3BaseUrl + "/" + storageProperties.getBucket() + "/" + objectName;
		}

		S3Presigner.Builder presignerBuilder = S3Presigner.builder()
				.endpointOverride(URI.create(clientToS3BaseUrl))
				.region(Region.of(storageProperties.getRegion()))
				.credentialsProvider(
						StaticCredentialsProvider.create(
								AwsBasicCredentials.create(
										storageProperties.getAccessKey(),
										storageProperties.getSecretKey())))
				.serviceConfiguration(
						S3Configuration.builder()
								.pathStyleAccessEnabled(true)
								.build());

		try (S3Presigner presigner = presignerBuilder.build()) {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectName)
					.build();

			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
					.signatureDuration(Duration.ofMinutes(30))
					.getObjectRequest(getObjectRequest)
					.build();

			return presigner.presignGetObject(presignRequest).url().toString();
		}
	}

	public String getPhotoUrl(String objectName, boolean isPublic) {
		if (objectName == null || objectName.isBlank()) {
			return null;
		}
		boolean publicObject = isPublicObjectKey(objectName);
		String storageType = storageProperties.getType();
		if ("minio".equalsIgnoreCase(storageType)) {
			try {
				return getMinIOStoragePhotoUrl(objectName, publicObject);
			} catch (Exception e) {
				log.error("MinIO에서 미리 서명된 URL 생성 실패: {}", e.getMessage(), e);
				return null;
			}
		}

		S3Presigner.Builder presignerBuilder = S3Presigner.builder()
				.region(Region.of(storageProperties.getRegion()));

		try (S3Presigner presigner = presignerBuilder.build()) {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectName)
					.build();

			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
					.signatureDuration(Duration.ofHours(12))
					.getObjectRequest(getObjectRequest)
					.build();

			return presigner.presignGetObject(presignRequest).url().toString();
		} catch (Exception e) {
			log.error("미리 서명된 URL 생성에 실패했습니다. Object: {}", objectName, e);
			return null;
		}
	}

	@Override
	public String resolve(String objectKey) {
		return getPhotoUrl(objectKey, isPublicObjectKey(objectKey));
	}

	public String saveGeneratedImage(String base64Data, String userId, String fileExtension) {
		try {
			if (base64Data == null || base64Data.trim().isEmpty()) {
				throw new IllegalArgumentException("Base64 데이터가 비어있습니다.");
			}

			String cleanExtension = fileUtil.cleanExtension(fileExtension);
			byte[] imageBytes = fileUtil.decodeBase64(base64Data);
			ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

			String objectName = photoUtil.generateDiaryAiImageObjectKey(cleanExtension);

			ensureBucketExists(storageProperties.getBucket());

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectName)
					.contentType(fileUtil.getContentType(cleanExtension))
					.cacheControl(cacheControlFor(objectName))
					.build();

			s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, imageBytes.length));

			log.info("Base64 이미지 저장 완료 - 사용자: {}, objectKey: {}, 크기: {} bytes", userId, objectName, imageBytes.length);
			return objectName;

		} catch (IllegalArgumentException e) {
			log.error("Base64 디코딩 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
			throw new RuntimeException("Base64 데이터가 올바르지 않습니다.", e);
		} catch (Exception e) {
			log.error("AI 이미지 저장 중 예상하지 못한 오류 발생: {}", e.getMessage(), e);
			throw new RuntimeException("AI 이미지 저장 중 오류가 발생했습니다.", e);
		}
	}

	public String moveToPublic(String sourceKey) {
		String targetKey = photoUtil.publicObjectKeyFor(sourceKey);
		return copyObject(sourceKey, targetKey, true);
	}

	@Override
	public String copyGeneratedImageToPublic(String sourceObjectKey) {
		return copyObject(sourceObjectKey, photoUtil.publicObjectKeyFor(sourceObjectKey), false);
	}

	@Override
	public String copyToVisibilityScope(String sourceKey, DiaryVisibility visibility, DiaryPhotoType sourceType) {
		String targetKey = photoUtil.visibilityObjectKeyFor(sourceKey, isPublicDiary(visibility), sourceType);
		return copyObject(sourceKey, targetKey, false);
	}

	private String copyObject(String sourceKey, String targetKey, boolean deleteSource) {
		boolean fileCopied = false;

		try {
			if (sourceKey.equals(targetKey)) {
				log.info("이미 대상 경로입니다: {}", sourceKey);
				return sourceKey;
			}

			if (objectHead(targetKey).isPresent()) {
				log.info("대상 경로에 이미 파일이 존재합니다. sourceKey: {}, targetKey: {}", sourceKey, targetKey);
				if (deleteSource) {
					deleteObject(sourceKey);
				}
				return targetKey;
			}

			HeadObjectResponse sourceHead = objectHead(sourceKey)
					.orElseThrow(() -> {
						log.error("소스 파일이 존재하지 않습니다: {}", sourceKey);
						return new RuntimeException("소스 파일을 찾을 수 없습니다: " + sourceKey);
					});

			CopyObjectRequest.Builder copyRequestBuilder = CopyObjectRequest.builder()
					.sourceBucket(storageProperties.getBucket())
					.sourceKey(sourceKey)
					.destinationBucket(storageProperties.getBucket())
					.destinationKey(targetKey)
					.metadataDirective(MetadataDirective.REPLACE)
					.cacheControl(cacheControlFor(targetKey));

			if (hasText(sourceHead.contentType())) {
				copyRequestBuilder.contentType(sourceHead.contentType());
			}
			if (sourceHead.metadata() != null && !sourceHead.metadata().isEmpty()) {
				copyRequestBuilder.metadata(sourceHead.metadata());
			}

			s3Client.copyObject(copyRequestBuilder.build());
			fileCopied = true;

			if (!objectExists(targetKey)) {
				throw new RuntimeException("파일 복사 후 확인 실패: " + targetKey);
			}

			if (deleteSource) {
				deleteObject(sourceKey);
			}

			log.info("파일 복제 완료: {} → {} (원본 삭제 여부: {})", sourceKey, targetKey, deleteSource);
			return targetKey;

		} catch (S3Exception e) {
			log.error("S3 파일 복제 실패: {} → {}, 오류: {}", sourceKey, targetKey, e.getMessage(), e);

			if (fileCopied) {
				try {
					if (!objectExists(targetKey)) {
						throw new RuntimeException("복사된 파일을 확인할 수 없습니다: " + targetKey);
					}
					deleteObject(targetKey);
					log.info("롤백: 복사된 파일 삭제 완료: {}", targetKey);
				} catch (Exception rollbackException) {
					log.error("롤백 실패: {}", rollbackException.getMessage());
				}
			}

			throw new RuntimeException("파일 복제 중 오류가 발생했습니다.", e);
		} catch (Exception e) {
			log.error("파일 복제 중 예상하지 못한 오류 발생: {}", e.getMessage(), e);

			if (fileCopied) {
				try {
					if (!objectExists(targetKey)) {
						throw new RuntimeException("복사된 파일을 확인할 수 없습니다: " + targetKey);
					}
					deleteObject(targetKey);
					log.info("롤백: 복사된 파일 삭제 완료: {}", targetKey);
				} catch (Exception rollbackException) {
					log.error("롤백 실패: {}", rollbackException.getMessage());
				}
			}

			throw new RuntimeException("파일 복제 중 오류가 발생했습니다.", e);
		}
	}

	private boolean isPublicDiary(DiaryVisibility visibility) {
		return visibility == DiaryVisibility.PUBLIC || visibility == DiaryVisibility.ANONYMOUS;
	}

	private boolean isPublicObjectKey(String objectName) {
		return objectName.startsWith(PUBLIC_PREFIX);
	}

	private boolean objectExists(String key) {
		return objectHead(key).isPresent();
	}

	private Optional<HeadObjectResponse> objectHead(String key) {
		try {
			HeadObjectRequest request = HeadObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(key)
					.build();

			return Optional.of(s3Client.headObject(request));
		} catch (NoSuchKeyException e) {
			return Optional.empty();
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				return Optional.empty();
			}
			log.warn("event=storage_object_exists_failed outcome=failed key={} status={} reason={}",
					key,
					e.statusCode(),
					e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
			throw e;
		}
	}

	private String cacheControlFor(String objectKey) {
		return imageCacheProperties.cacheControlForObjectKey(objectKey);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public void deleteObject(String key) {
		try {
			DeleteObjectRequest request = DeleteObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(key)
					.build();

			s3Client.deleteObject(request);
			log.info("파일 삭제 완료: {}", key);
		} catch (S3Exception e) {
			log.error("S3 파일 삭제 실패: {}, 오류: {}", key, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public void delete(String objectKey) {
		deleteObject(objectKey);
	}
}
