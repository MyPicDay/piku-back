package com.pikume.back.global.port.out;

/**
 * 저장된 객체를 표시할 수 있는 URL로 해석하는 중립 기술 계약이다.
 */
public interface ResolveObjectUrlPort {

	String resolveObjectUrl(String objectKey, boolean publiclyAccessible);
}
