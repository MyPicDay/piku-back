package store.piku.back.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.enums.DiaryPhotoType;
import store.piku.back.diary.enums.Status;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiaryDTO {

    private Status status;  // private , public, friend

    private String content;

    private List<Long> aiPhotos; // AI 사진 (url로 받는다)

    private List<MultipartFile> photos; // photo사진 (파일 받는거야)

    private LocalDate date;

    // NOTE: mainPhotoType은 일기 사진 중 대표 사진의 타입을 나타내고 대표 사진은 0의 인덱스를 가집니다
    private DiaryPhotoType coverPhotoType;

    private int coverPhotoIndex;
}
