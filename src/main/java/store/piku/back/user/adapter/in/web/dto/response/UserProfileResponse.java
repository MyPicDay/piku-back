package store.piku.back.user.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import store.piku.back.user.application.dto.UserProfileResult;

import java.util.List;

@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(
		@Schema(description = "사용자 ID") String id,
		@Schema(description = "닉네임") String nickname,
		@Schema(description = "아바타 이미지 URL") String avatar,
		@Schema(description = "친구 수") int friendCount,
		@Schema(description = "일기 총 개수") long diaryCount,
		@Schema(description = "친구 관계 상태") String friendStatus,
		@Schema(description = "본인 프로필 여부") boolean isOwner,
		@Schema(description = "월별 일기 개수 리스트") List<MonthlyDiaryCountResponse> monthlyDiaryCount) {
	public static UserProfileResponse from(UserProfileResult result) {
		List<MonthlyDiaryCountResponse> monthlyCounts = result.monthlyDiaryCount().stream()
				.map(m -> new MonthlyDiaryCountResponse(m.year(), m.month(), m.count()))
				.toList();
		return new UserProfileResponse(
				result.id(),
				result.nickname(),
				result.avatar(),
				result.friendCount(),
				result.diaryCount(),
				result.friendStatus(),
				result.isOwner(),
				monthlyCounts);
	}

	public record MonthlyDiaryCountResponse(int year, int month, long count) {
	}
}
