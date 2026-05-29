package com.pikume.back.diary.adapter.out.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
	private final String type;
	private final String serverToS3Url;
	private final String clientToS3Url;
	private final String region;
	private final String accessKey;
	private final String secretKey;
	private final String bucket;

	public String serverToS3BaseUrl() {
		return normalizeBaseUrl(serverToS3Url, "storage.server-to-s3-url");
	}

	public String clientToS3BaseUrl() {
		if (hasText(clientToS3Url)) {
			return normalizeBaseUrl(clientToS3Url, "storage.client-to-s3-url");
		}
		return serverToS3BaseUrl();
	}

	private String normalizeBaseUrl(String value, String propertyName) {
		if (!hasText(value)) {
			throw new IllegalStateException(propertyName + " must not be blank");
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
