package com.pikume.back.character.adapter.out.storage;

import com.pikume.back.global.storage.StorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinioFixedCharacterAssetCatalogAdapter")
class MinioFixedCharacterAssetCatalogAdapterTest {

	@Mock
	private S3Client s3Client;

	@Test
	@DisplayName("public/characters/fixed prefix의 이미지 object key만 정렬해서 반환한다")
	void listsOnlyFixedCharacterImageObjectKeys() {
		MinioFixedCharacterAssetCatalogAdapter adapter = new MinioFixedCharacterAssetCatalogAdapter(
				s3Client,
				storageProperties());
			given(s3Client.listObjectsV2(fixedCharacterListRequest()))
					.willReturn(ListObjectsV2Response.builder()
							.contents(
									S3Object.builder().key("public/characters/fixed/base_image_2.webp").build(),
									S3Object.builder().key("public/characters/fixed/group/base_image_3.webp").build(),
									S3Object.builder().key("public/characters/fixed/").build(),
									S3Object.builder().key("public/characters/fixed/readme.txt").build(),
									S3Object.builder().key("public/characters/fixed/base_image_1.webp").build())
							.isTruncated(false)
							.build());

			List<String> result = adapter.listFixedCharacterObjectKeys();

			assertThat(result).containsExactly(
					"public/characters/fixed/base_image_1.webp",
					"public/characters/fixed/base_image_2.webp",
					"public/characters/fixed/group/base_image_3.webp");
		}

	private ListObjectsV2Request fixedCharacterListRequest() {
		return argThat((ListObjectsV2Request request) -> request != null
				&& "piku".equals(request.bucket())
				&& "public/characters/fixed/".equals(request.prefix()));
	}

	private StorageProperties storageProperties() {
		return new StorageProperties(
				"minio",
				"http://minio:9000",
				"https://assets.example.com",
				"ap-northeast-2",
				"test-access-key",
				"test-secret-key",
				"piku");
	}
}
