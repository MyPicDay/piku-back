package com.pikume.back.social.domain.comment;

import jakarta.persistence.*;
import lombok.*;
import com.pikume.back.social.domain.SocialAuditableEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends SocialAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String content;

	@Column(name = "user_id", nullable = false, length = 36)
	private String userId;

	@Column(name = "diary_id", nullable = false)
	private Long diaryId;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "parent_id")
	private Comment parent;

	@OneToMany(mappedBy = "parent")
	private List<Comment> children = new ArrayList<>();

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public Comment(String content, String userId, Long diaryId) {
		this.content = content;
		this.userId = userId;
		this.diaryId = diaryId;
	}

	public void connectParent(Comment parent) {
		if (parent != null && !this.diaryId.equals(parent.diaryId)) {
			throw new IllegalArgumentException("부모 댓글은 같은 일기에 속해야 합니다.");
		}
		if (parent != null && parent.parent != null) {
			throw new IllegalArgumentException("답글에는 답글을 연결할 수 없습니다.");
		}
		if (this.parent != null) {
			this.parent.getChildren().remove(this);
		}
		this.parent = parent;
		if (parent != null) {
			parent.getChildren().add(this);
		}
	}

	public void updateContent(String content) {
		if (content != null && !content.trim().isEmpty()) {
			this.content = content;
		}
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return this.deletedAt != null;
	}
}
