package com.pikume.back.notification.adapter.in.web;

import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.adapter.in.web.dto.NotificationPageResponse;
import com.pikume.back.notification.adapter.in.web.dto.NotificationResponse;
import com.pikume.back.notification.application.dto.NotificationResult;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationWebMapper {

	public NotificationPageResponse toPageResponse(
			PageResult<NotificationResult> result,
			Pageable pageable) {
		NotificationPageResponse.SortMetadata sort = new NotificationPageResponse.SortMetadata(
				pageable.getSort().isEmpty(),
				pageable.getSort().isSorted(),
				pageable.getSort().isUnsorted());
		NotificationPageResponse.PageableMetadata pageableMetadata =
				new NotificationPageResponse.PageableMetadata(
						result.getNumber(),
						result.getSize(),
						sort,
						(long) result.getNumber() * result.getSize(),
						true,
						false);
		List<NotificationResponse> content = result.getContent().stream()
				.map(this::toResponse)
				.toList();
		return new NotificationPageResponse(
				content,
				pageableMetadata,
				result.last(),
				result.totalPages(),
				result.getTotalElements(),
				result.getSize(),
				result.getNumber(),
				sort,
				result.numberOfElements(),
				result.first(),
				result.empty());
	}

	public NotificationResponse toResponse(NotificationResult result) {
		return new NotificationResponse(
				result.id(),
				result.message(),
				result.nickname(),
				result.avatarUrl(),
				result.type(),
				result.relatedDiaryId(),
				result.thumbnailUrl(),
				result.isRead(),
				result.createdAt(),
				result.diaryDate(),
				result.diaryUserId());
	}
}
