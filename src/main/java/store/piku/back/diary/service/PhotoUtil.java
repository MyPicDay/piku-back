package store.piku.back.diary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.file.FileConstants;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class PhotoUtil {

    // 1. 파일명 생성
    public String generateFileName(LocalDate diaryDate, String originalFilename) {
        String date = diaryDate
                .atStartOfDay(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info("파일명 생성 - 현재 등록하는 일기날짜: {}", diaryDate);

        String uuid = UUID.randomUUID().toString().substring(0, 8);


        String fileName = date + "_" + uuid;
        log.info("파일명 생성완료  - 생성명:{} ",  fileName);
        return fileName;
    }

    public String getDefaultPath(){
        return System.getProperty("user.dir");
    }

    // 2. 로컬 저장
    public String saveToLocal(MultipartFile photos, String userId, String filename) throws IOException {
        String uploadPath = FileConstants.UPLOADS_BASE_DIR_NAME + "/" + userId;
        String uploadDir = getDefaultPath() + "/" + uploadPath;

        new File(uploadDir).mkdirs();
        File destination = new File(uploadDir, filename);

        log.info("파일 저장 시작 - 저장 경로: {}", destination);
        photos.transferTo(destination);

        log.info("파일 저장 완료 - 저장 경로: {}", destination);
        return userId + "/" + filename;
    }
}


