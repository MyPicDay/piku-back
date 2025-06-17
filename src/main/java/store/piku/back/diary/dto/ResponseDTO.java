package store.piku.back.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import store.piku.back.diary.entity.Status;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO {
    private Status status;
    private String content;
    private List<String> photos;  // 업로드용
    private Date date;
}