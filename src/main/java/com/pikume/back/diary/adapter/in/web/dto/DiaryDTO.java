package com.pikume.back.diary.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiaryDTO {
	@NotNull
	@Schema(description = "공개범위")
	private DiaryVisibility status;

	@NotBlank(message = "일기 내용은 비어 있을 수 없습니다.")
	@Schema(description = "일기 내용")
	@Size(max = 500, message = "일기 내용은 최대 500자까지 입력할 수 있습니다.")
	private String content;

	@NotNull
	@Valid
	@Schema(description = "일기 이미지 정보들")
	private List<DiaryImageInfo> imageInfos;

	@NotNull
	@Schema(description = "일기날짜 ")
	private LocalDate date;
}
