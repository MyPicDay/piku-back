package com.pikume.back.diary.adapter.out.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import com.pikume.back.creative.application.port.out.CreativeImageStoragePort;
import com.pikume.back.diary.application.port.out.PhotoStoragePort;
import com.pikume.back.diary.application.port.out.SaveDiaryPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.ResolveImageUrlPort;
import com.pikume.back.global.port.out.StoreObjectPort;
import com.pikume.back.global.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.pikume.back.diary.adapter.out.storage.PhotoConstants.PUBLIC_PREFIX;

@Slf4j
@Component
public class MinioPhotoStorageAdapter implements PhotoStoragePort, ResolveImageUrlPort, CreativeImageStoragePort, StoreObjectPort {

	private final S3Client s3Client;
	private final PhotoUtil photoUtil;
	private final SaveDiaryPort saveDiaryPort;
	private final StorageProperties storageProperties;
	private final FileUtil fileUtil;

	public MinioPhotoStorageAdapter(S3Client s3Client, PhotoUtil photoUtil, SaveDiaryPort saveDiaryPort,
			StorageProperties storageProperties, FileUtil fileUtil) {
		this.s3Client = s3Client;
		this.photoUtil = photoUtil;
		this.saveDiaryPort = saveDiaryPort;
		this.storageProperties = storageProperties;
		this.fileUtil = fileUtil;
	}

	@Override
	public void savePhoto(Diary diary, UploadedFileData photo, String userId, Integer order) throws IOException {
		log.info("사진 S3 저장 시작 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());

		try {
			if (!photo.isEmpty()) {
				String originalFilename = photo.originalFilename();
				String filename = photoUtil.generateFileName(diary.getDate(), originalFilename);
				boolean isPublic = (order != null && order == 0);
				String objectName = isPublic ? PUBLIC_PREFIX + userId + "/" + filename : userId + "/" + filename;

				ensureBucketExists(storageProperties.getBucket());

				PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
						.bucket(storageProperties.getBucket())
						.key(objectName)
						.contentType(photo.contentType())
						.contentLength(photo.size());

				if (isPublic) {
					requestBuilder.cacheControl("public, max-age=31536000, immutable");
				}

				PutObjectRequest putObjectRequest = requestBuilder.build();

				s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(photo.inputStream(), photo.size()));

				Photo savePhoto = new Photo(diary, objectName, order);
				if (isPublic) {
					savePhoto.updateRepresent(true);
				}
				saveDiaryPort.savePhoto(savePhoto);
			} else {
				log.warn("빈 파일 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
			}
		} catch (Exception e) {
			log.warn("Exception occured while saving photo : {}", e.getMessage(), e);
			throw new RuntimeException("S3 파일 저장 중 오류 발생", e);
		}
	}

	@Override
	public String storeObject(UploadedFileData image, String objectKey) {
		try {
			ensureBucketExists(storageProperties.getBucket());

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectKey)
					.contentType(image.contentType())
					.contentLength(image.size())
					.build();

			s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(image.inputStream(), image.size()));

			return objectKey;
		} catch (Exception e) {
			throw new RuntimeException("이미지 업로드 중 오류 발생", e);
		}
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

	public String getMinIOStoragePhotoUrl(String objectName, boolean isPublic) throws Exception {
		MinioClient minioClient = MinioClient.builder()
				.endpoint(storageProperties.getEndpoint())
				.credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey())
				.build();
		if (isPublic) {
			return storageProperties.getEndpoint() + "/" + storageProperties.getBucket() + "/" + objectName;
		}

		String url = minioClient.getPresignedObjectUrl(
				GetPresignedObjectUrlArgs.builder()
						.method(Method.GET)
						.bucket(storageProperties.getBucket())
						.object(objectName)
						.expiry(30, TimeUnit.MINUTES)
						.build());
		return url;
	}

	@Override
	public String getPhotoUrl(String objectName, boolean isPublic) {
		String storageType = storageProperties.getType();
		if ("minio".equalsIgnoreCase(storageType)) {
			try {
				return getMinIOStoragePhotoUrl(objectName, isPublic);
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
	public String saveAIPhoto(String base64Data, String userId, String fileExtension) {
		try {
			if (base64Data == null || base64Data.trim().isEmpty()) {
				throw new IllegalArgumentException("Base64 데이터가 비어있습니다.");
			}

			String cleanExtension = fileUtil.cleanExtension(fileExtension);
			String fileName = fileUtil.generateUniqueFileNameWithExtension(cleanExtension);
			byte[] imageBytes = fileUtil.decodeBase64(base64Data);
			ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

			String objectName = userId + "/" + fileName;

			ensureBucketExists(storageProperties.getBucket());

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectName)
					.contentType(fileUtil.getContentType(cleanExtension))
					.build();

			s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, imageBytes.length));

			log.info("Base64 이미지 저장 완료 - 사용자: {}, 파일: {}, 크기: {} bytes", userId, fileName, imageBytes.length);
			return objectName;

		} catch (IllegalArgumentException e) {
			log.error("Base64 디코딩 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
			throw new RuntimeException("Base64 데이터가 올바르지 않습니다.", e);
		} catch (Exception e) {
			log.error("AI 이미지 저장 중 예상하지 못한 오류 발생: {}", e.getMessage(), e);
			throw new RuntimeException("AI 이미지 저장 중 오류가 발생했습니다.", e);
		}
	}

	@Override
	public String moveToPublic(String sourceKey) {
		String targetKey = PUBLIC_PREFIX + sourceKey;
		boolean fileCopied = false;

		try {
			if (sourceKey.startsWith(PUBLIC_PREFIX)) {
				log.info("이미 public 경로입니다: {}", sourceKey);
				return sourceKey;
			}

			if (objectExists(targetKey)) {
				log.info("public 경로에 이미 파일이 존재합니다. 원본 삭제: {}", sourceKey);
				deleteObject(sourceKey);
				return targetKey;
			}

			if (!objectExists(sourceKey)) {
				log.error("소스 파일이 존재하지 않습니다: {}", sourceKey);
				throw new RuntimeException("소스 파일을 찾을 수 없습니다: " + sourceKey);
			}

			CopyObjectRequest copyRequest = CopyObjectRequest.builder()
					.sourceBucket(storageProperties.getBucket())
					.sourceKey(sourceKey)
					.destinationBucket(storageProperties.getBucket())
					.destinationKey(targetKey)
					.cacheControl("public, max-age=31536000, immutable")
					.build();

			s3Client.copyObject(copyRequest);
			fileCopied = true;

			if (!objectExists(targetKey)) {
				throw new RuntimeException("파일 복사 후 확인 실패: " + targetKey);
			}

			deleteObject(sourceKey);

			log.info("파일 이동 완료: {} → {} (원본 삭제됨)", sourceKey, targetKey);
			return targetKey;

		} catch (S3Exception e) {
			log.error("S3 파일 이동 실패: {} → {}, 오류: {}", sourceKey, targetKey, e.getMessage(), e);

			if (fileCopied && objectExists(targetKey)) {
				try {
					deleteObject(targetKey);
					log.info("롤백: 복사된 파일 삭제 완료: {}", targetKey);
				} catch (Exception rollbackException) {
					log.error("롤백 실패: {}", rollbackException.getMessage());
				}
			}

			throw new RuntimeException("파일 이동 중 오류가 발생했습니다.", e);
		} catch (Exception e) {
			log.error("파일 이동 중 예상하지 못한 오류 발생: {}", e.getMessage(), e);

			if (fileCopied && objectExists(targetKey)) {
				try {
					deleteObject(targetKey);
					log.info("롤백: 복사된 파일 삭제 완료: {}", targetKey);
				} catch (Exception rollbackException) {
					log.error("롤백 실패: {}", rollbackException.getMessage());
				}
			}

			throw new RuntimeException("파일 이동 중 오류가 발생했습니다.", e);
		}
	}

	private boolean objectExists(String key) {
		try {
			HeadObjectRequest request = HeadObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(key)
					.build();

			s3Client.headObject(request);
			return true;
		} catch (NoSuchKeyException e) {
			return false;
		} catch (Exception e) {
			log.warn("객체 존재 확인 중 오류: {}", e.getMessage());
			return false;
		}
	}

	private void deleteObject(String key) {
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
}
