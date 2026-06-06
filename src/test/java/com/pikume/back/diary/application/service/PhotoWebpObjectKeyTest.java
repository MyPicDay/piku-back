package com.pikume.back.diary.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PhotoWebpObjectKey")
class PhotoWebpObjectKeyTest {

	@Test
	@DisplayName("private 원본 key의 확장자만 webp로 바꾼다")
	void replacesPrivateObjectExtensionWithWebp() {
		assertThat(PhotoWebpObjectKey.fromOriginal("user-1/photo.png"))
				.contains("user-1/photo.webp");
	}

	@Test
	@DisplayName("public 원본 key도 prefix와 baseName을 유지한다")
	void preservesPublicPrefixAndBaseName() {
		assertThat(PhotoWebpObjectKey.fromOriginal("public/user-1/photo.jpeg"))
				.contains("public/user-1/photo.webp");
	}

	@Test
	@DisplayName("대문자 확장자도 webp 확장자로 치환한다")
	void handlesUppercaseExtension() {
		assertThat(PhotoWebpObjectKey.fromOriginal("user-1/photo.PNG"))
				.contains("user-1/photo.webp");
	}

	@Test
	@DisplayName("이미 WebP인 원본은 새 WebP key를 만들지 않는다")
	void returnsEmptyForWebpOriginal() {
		assertThat(PhotoWebpObjectKey.fromOriginal("user-1/photo.webp")).isEmpty();
	}

	@Test
	@DisplayName("확장자가 없으면 WebP key를 만들지 않는다")
	void returnsEmptyForObjectWithoutExtension() {
		assertThat(PhotoWebpObjectKey.fromOriginal("user-1/photo")).isEmpty();
	}
}
