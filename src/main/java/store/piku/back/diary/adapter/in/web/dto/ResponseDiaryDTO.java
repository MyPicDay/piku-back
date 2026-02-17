package store.piku.back.diary.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDiaryDTO {
	private Long diaryId;
	private String content;
}
