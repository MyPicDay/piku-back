//package store.piku.back.diary.service;
//
//import io.minio.*;
//import io.minio.http.Method;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//import store.piku.back.ai.entity.DiaryImageGeneration;
//import store.piku.back.ai.service.DiaryImageGenerationService;
//import store.piku.back.diary.entity.Diary;
//import store.piku.back.diary.entity.Photo;
//import store.piku.back.diary.repository.PhotoRepository;
//import store.piku.back.global.config.MinioConfig;
//
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class MinioPhotoStorage implements PhotoStorage{
//
//
//    private final PhotoUtil photoUtil;
//    private final PhotoRepository photoRepository;
//    private final DiaryImageGenerationService diaryImageGenerationService;
//
//    private final MinioClient minioClient;
//    private final MinioConfig minioConfig;
//
//    @Override
//    public void savePhoto(Diary diary, MultipartFile photo, String userId, Integer order) throws IOException {
//        log.info("사진 MinIO 저장 시작 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
//        String objectName = null;
//
//        try{
//            if (!photo.isEmpty()) {
//
//                String originalFilename = photo.getOriginalFilename();
//                String filename = photoUtil.generateFileName(diary.getDate(), originalFilename);
//                objectName = userId + "/" + filename;
//
//                ensureBucketExists(minioConfig.getBucket());
//
//                minioClient.putObject(
//                        PutObjectArgs.builder()
//                                .bucket(minioConfig.getBucket())
//                                .object(objectName)
//                                .stream(photo.getInputStream(), photo.getSize(), -1)
//                                .contentType(photo.getContentType())
//                                .build()
//                );
//
//                Photo savePhoto = new Photo(diary, objectName, order);
//                if (order == 0) {
//                    savePhoto.updateRepresent(true); // 첫 번째 사진을 대표 사진으로 설정
//                }
//                photoRepository.save(savePhoto);
//            } else {
//                log.warn("빈 파일 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
//            }
//        }catch (Exception e){
//            log.warn("Exception occured while saving photo : {}", e.getMessage(), e);
//            throw new IOException("MinIO 파일 저장 중 오류 발생", e);
//        }
//    }
//
//    @Override
//    public void saveAiPhoto(Diary diary, Long aiPhoto, String userId, Integer order) {
//        log.info("AI 사진 저장 시작 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
//
//        ensureBucketExists(minioConfig.getBucket());
//
//        if (aiPhoto != null) {
//            DiaryImageGeneration diaryImageGeneration = diaryImageGenerationService.findById(aiPhoto);
//            String filePath = diaryImageGeneration.getFilePath();
//
//            Photo savePhoto = new Photo(diary, filePath, order);
//            if (order == 0) {
//                savePhoto.updateRepresent(true); // 첫 번째 AI 사진을 대표 사진으로 설정
//            }
//            photoRepository.save(savePhoto);
//            diaryImageGenerationService.updateDiaryId(aiPhoto, diary.getId());
//        } else {
//            log.warn("빈 AI 사진 ID 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
//        }
//    }
//
//    private void ensureBucketExists(String bucket) {
//        try {
//            boolean exists = minioClient.bucketExists(
//                    BucketExistsArgs.builder().bucket(bucket).build());
//
//            if (!exists) {
//                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException("MinIO 버킷 확인/생성 중 오류 발생", e);
//        }
//    }
//
//
//    public String getPhotoUrl(String objectName) {
//        try {
//            return minioClient.getPresignedObjectUrl(
//                    GetPresignedObjectUrlArgs.builder()
//                            .method(Method.GET)
//                            .bucket(minioConfig.getBucket())
//                            .object(objectName)
//                            .expiry(12, TimeUnit.HOURS) // 12시간짜리 URL
//                            .build()
//            );
//        } catch (Exception e) {
//            log.warn("Presigned URL 생성 실패: {}", e.getMessage(), e);
//            return null;
//        }
//    }
//}
