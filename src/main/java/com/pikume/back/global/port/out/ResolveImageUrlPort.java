package com.pikume.back.global.port.out;

/**
 * 기존 이미지 소비자를 위한 단계적 호환 계약이다.
 *
 * @deprecated 새 소비자는 {@link ResolveObjectUrlPort} 또는 소비자 Context 소유 Port를 사용한다.
 */
@Deprecated
public interface ResolveImageUrlPort extends ResolveObjectUrlPort {

	String getPhotoUrl(String objectName, boolean isPublic);

	@Override
	default String resolveObjectUrl(String objectKey, boolean publiclyAccessible) {
		return getPhotoUrl(objectKey, publiclyAccessible);
	}
}
