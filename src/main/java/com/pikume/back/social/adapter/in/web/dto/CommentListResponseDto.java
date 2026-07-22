package com.pikume.back.social.adapter.in.web.dto;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentListResponseDto {
	private Long id;
	private Long diaryId;
	@Schema(nullable = true, description = "익명 또는 삭제 댓글에서는 제공하지 않는 작성자 식별자")
	private String userId;
	@Schema(nullable = true, description = "익명 정책 또는 삭제 상태에 따라 없을 수 있는 표시 이름")
	private String nickname;
	@Schema(nullable = true, description = "익명 정책, 삭제 상태 또는 아바타 미설정 시 없는 URL")
	private String avatar;
	private String content;
	private Long parentId;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	private int replyCount;
	private boolean canReply;
	private boolean canEdit;
	private boolean canDelete;
}
