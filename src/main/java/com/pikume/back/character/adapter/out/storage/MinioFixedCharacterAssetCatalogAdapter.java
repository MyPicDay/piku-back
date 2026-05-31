package com.pikume.back.character.adapter.out.storage;

import com.pikume.back.character.application.port.out.FixedCharacterAssetCatalogPort;
import com.pikume.back.global.storage.StorageProperties;
import com.pikume.back.global.util.CharacterAvatarPathNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MinioFixedCharacterAssetCatalogAdapter implements FixedCharacterAssetCatalogPort {

	private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
			".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp");

	private final S3Client s3Client;
	private final StorageProperties storageProperties;

	@Override
	public List<String> listFixedCharacterObjectKeys() {
		List<String> objectKeys = new ArrayList<>();
		String continuationToken = null;

		do {
			ListObjectsV2Request request = ListObjectsV2Request.builder()
					.bucket(storageProperties.getBucket())
					.prefix(CharacterAvatarPathNormalizer.FIXED_CHARACTER_PUBLIC_PREFIX)
					.continuationToken(continuationToken)
					.build();
			ListObjectsV2Response response = s3Client.listObjectsV2(request);

			response.contents().stream()
					.map(object -> object.key())
					.filter(this::isSupportedFixedCharacterImage)
					.forEach(objectKeys::add);

			continuationToken = Boolean.TRUE.equals(response.isTruncated())
					? response.nextContinuationToken()
					: null;
		} while (continuationToken != null);

		return objectKeys.stream()
				.distinct()
				.sorted()
				.toList();
	}

	private boolean isSupportedFixedCharacterImage(String objectKey) {
		if (objectKey == null || !objectKey.startsWith(CharacterAvatarPathNormalizer.FIXED_CHARACTER_PUBLIC_PREFIX)) {
			return false;
		}
		if (objectKey.equals(CharacterAvatarPathNormalizer.FIXED_CHARACTER_PUBLIC_PREFIX)) {
			return false;
		}
		String lowerCaseKey = objectKey.toLowerCase(Locale.ROOT);
		return SUPPORTED_IMAGE_EXTENSIONS.stream().anyMatch(lowerCaseKey::endsWith);
	}
}
