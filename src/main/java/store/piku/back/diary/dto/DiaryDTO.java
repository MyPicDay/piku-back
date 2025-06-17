package store.piku.back.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.entity.Status;
import store.piku.back.user.entity.User;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiaryDTO {

    private Status status;  // private , public, friend

    private String content;

//    private List<String> aiPhotos; // AI 사진 (url로 받는다)

    private List<MultipartFile> photos; // photo사진 (파일 받는거야)


    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date; // 일기 쓰고자 하는 날짜

}
