package com.pikume.back.diary.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateDiaryRequest {
	@NotNull
	@Schema(description = "공개범위")
	private DiaryVisibility status;

	@NotBlank(message = "일기 내용은 비어 있을 수 없습니다.")
	@Size(max = 500, message = "일기 내용은 최대 500자까지 입력할 수 있습니다.")
	@Schema(description = "일기 내용")
	private String content;
}
