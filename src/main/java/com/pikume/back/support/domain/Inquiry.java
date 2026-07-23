package com.pikume.back.support.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.global.entity.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, length = 36)
	private String userId;

	@Column(nullable = false, length = 1000)
	private String content;

	@Column(name = "image_url")
	private String attachmentReference;

	private Inquiry(String userId, String content, String attachmentReference) {
		this.userId = requireSubmitter(userId);
		this.content = requireStorableContent(content);
		this.attachmentReference = attachmentReference;
	}

	public static Inquiry submit(String userId, String content, String attachmentReference) {
		return new Inquiry(userId, content, attachmentReference);
	}

	private static String requireSubmitter(String userId) {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("문의 제출자 식별자는 필수입니다.");
		}
		return userId;
	}

	private static String requireStorableContent(String content) {
		if (content == null) {
			throw new IllegalArgumentException("문의 내용은 필수입니다.");
		}
		if (content.length() > 1000) {
			throw new IllegalArgumentException("문의 내용은 최대 1000자까지 저장할 수 있습니다.");
		}
		return content;
	}
}
