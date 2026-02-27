package com.pikume.back.social.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentDeleteResponseDto {
	private boolean success;
	private String message;
	private Long commentId;
}
