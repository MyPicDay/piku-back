package com.pikume.back.notification.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "NotificationPageResponse")
public record NotificationPageResponse(
		List<NotificationResponse> content,
		PageableMetadata pageable,
		boolean last,
		int totalPages,
		long totalElements,
		int size,
		int number,
		SortMetadata sort,
		int numberOfElements,
		boolean first,
		boolean empty
) {

	public record PageableMetadata(
			int pageNumber,
			int pageSize,
			SortMetadata sort,
			long offset,
			boolean paged,
			boolean unpaged
	) {
	}

	public record SortMetadata(
			boolean empty,
			boolean sorted,
			boolean unsorted
	) {
	}
}
