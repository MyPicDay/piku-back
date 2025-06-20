package store.piku.back.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import store.piku.back.diary.enums.Status;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO {
    private Long diaryId;
    private Status status;
    private String content;
    private List<String> photos;  // 업로드용
    private LocalDate date;
    private String nickname; // 작성자 닉네임 추가
}