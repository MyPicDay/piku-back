package store.piku.back.diary.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CalendarDiaryResponseDTO {
    private Long diaryId;
    private String coverPhotoUrl;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}
