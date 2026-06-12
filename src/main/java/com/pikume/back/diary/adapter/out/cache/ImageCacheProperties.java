package com.pikume.back.diary.adapter.out.cache;

import com.pikume.back.diary.adapter.out.storage.PhotoObjectKeyConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "image.cache")
public class ImageCacheProperties {

	private static final String PUBLIC_PREFIX = PhotoObjectKeyConstants.PUBLIC_PREFIX;
	private static final String PRIVATE_PREFIX = PhotoObjectKeyConstants.PRIVATE_PREFIX;

	private String publicCacheControl = "public, max-age=300, s-maxage=1200";
	private String privateCacheControl = "private, no-store, max-age=0";

	public String cacheControlForObjectKey(String objectKey) {
		if (objectKey == null || objectKey.isBlank()) {
			return null;
		}
		if (objectKey.startsWith(PUBLIC_PREFIX)) {
			return publicCacheControl;
		}
		if (objectKey.startsWith(PRIVATE_PREFIX)) {
			return privateCacheControl;
		}
		return null;
	}
}
