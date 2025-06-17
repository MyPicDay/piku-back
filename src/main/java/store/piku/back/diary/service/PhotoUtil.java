package store.piku.back.diary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class PhotoUtil {

    // 1. 파일명 생성
    public String generateFileName(Date diaryDate, String originalFilename) {
        String date = diaryDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info("파일명 생성 - 현재 등록하는 일기날짜: {}", diaryDate);

        String uuid = UUID.randomUUID().toString().substring(0, 8);


        log.info("파일명 생성완료  - 생성명:{} ",  date + "_" + uuid + "_" + originalFilename );
        return date + "_" + uuid + "_" + originalFilename;
    }

    // 2. 로컬 저장
    public String saveToLocal(MultipartFile photos, String userId, String filename) throws IOException {
        String uploadDir = System.getProperty("user.dir") + "/uploads/" + userId;
        new File(uploadDir).mkdirs();
        File destination = new File(uploadDir, filename);

        log.info("파일 저장 시작 - 저장 경로: {}", destination);
        photos.transferTo(destination);

        log.info("파일 저장 완료 - 저장 경로: {}", destination);
        return "/uploads/" + userId + "/" + filename;
    }
}


