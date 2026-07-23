package com.pikume.back.global.storage;

import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.LoadObjectPort;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.global.port.out.StoreObjectPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3ObjectStorageAdapter implements LoadObjectPort, StoreObjectPort, ResolveObjectUrlPort {

	private final S3Client s3Client;
	private final StorageProperties storageProperties;

	@Override
	public byte[] loadObject(String objectKey) {
		try {
			GetObjectRequest request = GetObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectKey)
					.build();
			ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
			return response.asByteArray();
		} catch (NoSuchKeyException exception) {
			throw missingObject(objectKey, exception);
		} catch (S3Exception exception) {
			if (exception.statusCode() == 404) {
				throw missingObject(objectKey, exception);
			}
			throw new RuntimeException("스토리지 객체를 읽는 중 오류가 발생했습니다.", exception);
		}
	}

	@Override
	public String storeObject(UploadedFileData file, String objectKey, String cacheControl) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("빈 파일은 저장할 수 없습니다.");
		}

		ensureBucketExists();
		PutObjectRequest.Builder request = PutObjectRequest.builder()
				.bucket(storageProperties.getBucket())
				.key(objectKey)
				.contentType(file.contentType())
				.contentLength((long) file.bytes().length);
		if (cacheControl != null && !cacheControl.isBlank()) {
			request.cacheControl(cacheControl);
		}
		s3Client.putObject(request.build(), RequestBody.fromBytes(file.bytes()));
		return objectKey;
	}

	@Override
	public String resolveObjectUrl(String objectKey, boolean publiclyAccessible) {
		if (objectKey == null || objectKey.isBlank()) {
			return null;
		}

		try {
			if ("minio".equalsIgnoreCase(storageProperties.getType())) {
				return publiclyAccessible ? publicObjectUrl(objectKey) : minioPresignedUrl(objectKey);
			}
			return s3PresignedUrl(objectKey);
		} catch (RuntimeException exception) {
			log.warn("event=object_url_resolution_failed outcome=skipped reason={}",
					exception.getClass().getSimpleName());
			return null;
		}
	}

	private void ensureBucketExists() {
		try {
			s3Client.headBucket(HeadBucketRequest.builder()
					.bucket(storageProperties.getBucket())
					.build());
		} catch (S3Exception exception) {
			if (exception.statusCode() != 404) {
				throw exception;
			}
			s3Client.createBucket(CreateBucketRequest.builder()
					.bucket(storageProperties.getBucket())
					.build());
		}
	}

	private String publicObjectUrl(String objectKey) {
		return storageProperties.clientToS3BaseUrl()
				+ "/"
				+ storageProperties.getBucket()
				+ "/"
				+ objectKey;
	}

	private String minioPresignedUrl(String objectKey) {
		S3Presigner.Builder builder = S3Presigner.builder()
				.endpointOverride(URI.create(storageProperties.clientToS3BaseUrl()))
				.region(Region.of(storageProperties.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(
								storageProperties.getAccessKey(),
								storageProperties.getSecretKey())))
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(true)
						.build());
		return presignedUrl(builder, objectKey, Duration.ofMinutes(30));
	}

	private String s3PresignedUrl(String objectKey) {
		return presignedUrl(
				S3Presigner.builder().region(Region.of(storageProperties.getRegion())),
				objectKey,
				Duration.ofHours(12));
	}

	private String presignedUrl(S3Presigner.Builder builder, String objectKey, Duration duration) {
		try (S3Presigner presigner = builder.build()) {
			GetObjectRequest request = GetObjectRequest.builder()
					.bucket(storageProperties.getBucket())
					.key(objectKey)
					.build();
			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
					.signatureDuration(duration)
					.getObjectRequest(request)
					.build();
			return presigner.presignGetObject(presignRequest).url().toString();
		}
	}

	private RuntimeException missingObject(String objectKey, RuntimeException cause) {
		return new RuntimeException("스토리지 객체를 찾을 수 없습니다: " + objectKey, cause);
	}
}
