package com.pikume.back.diary.adapter.out.storage;

import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PhotoUtil")
class PhotoUtilTest {

	private final PhotoUtil photoUtil = new PhotoUtil();

	@Test
	@DisplayName("legacy userId 기반 object key는 새 visibility scope로 옮길 때 userId 없는 shard key로 변환한다")
	void convertsLegacyUserObjectKeyToAnonymousShardKey() {
		String result = photoUtil.visibilityObjectKeyFor(
				"public/user-1/photo.jpg",
				false,
				DiaryPhotoType.USER_IMAGE);

		assertThat(result).startsWith("private/diary-images/user/");
		assertThat(result).endsWith(".jpg");
		assertThat(result).doesNotContain("user-1");
	}

	@Test
	@DisplayName("새 diary image object key는 visibility prefix만 변경한다")
	void swapsVisibilityPrefixForNewDiaryImageObjectKey() {
		String result = photoUtil.visibilityObjectKeyFor(
				"private/diary-images/ai/ab/cd/image.png",
				true,
				DiaryPhotoType.AI_IMAGE);

		assertThat(result).isEqualTo("public/diary-images/ai/ab/cd/image.png");
	}
}
