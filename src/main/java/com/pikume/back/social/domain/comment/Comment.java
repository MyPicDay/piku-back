package com.pikume.back.social.domain.comment;

import jakarta.persistence.*;
import lombok.*;
import com.pikume.back.global.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

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

	public Comment(String content, String userId, Long diaryId) {
		this.content = content;
		this.userId = userId;
		this.diaryId = diaryId;
	}

	public void connectParent(Comment parent) {
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

	public boolean isDeleted() {
		return this.getDeletedAt() != null;
	}
}
