package store.piku.back.diary.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.repository.PhotoRepository;
import store.piku.back.file.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PhotoStorageService {

    private final S3Client s3Client;
    private final PhotoUtil photoUtil;
    private final PhotoRepository photoRepository;
    private final StorageProperties storageProperties;
    private final FileUtil fileUtil;
    private final Environment environment;

    public PhotoStorageService(S3Client s3Client, PhotoUtil photoUtil, PhotoRepository photoRepository,
                               StorageProperties storageProperties, FileUtil fileUtil, Environment environment) {
        this.s3Client = s3Client;
        this.photoUtil = photoUtil;
        this.photoRepository = photoRepository;
        this.storageProperties = storageProperties;
        this.fileUtil = fileUtil;
        this.environment = environment;
    }

    public void savePhoto(Diary diary, MultipartFile photo, String userId, Integer order) throws IOException {
        log.info("사진 S3 저장 시작 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
        String objectName = null;

        try {
            if (!photo.isEmpty()) {
                String originalFilename = photo.getOriginalFilename();
                String filename = photoUtil.generateFileName(diary.getDate(), originalFilename);
                objectName = userId + "/" + filename;

                ensureBucketExists(storageProperties.getBucket());

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(storageProperties.getBucket())
                        .key(objectName)
                        .contentType(photo.getContentType())
                        .contentLength(photo.getSize())
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(photo.getInputStream(), photo.getSize()));

                Photo savePhoto = new Photo(diary, objectName, order);
                if (order == 0) {
                    savePhoto.updateRepresent(true);
                }
                photoRepository.save(savePhoto);
            } else {
                log.warn("빈 파일 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
            }
        } catch (Exception e) {
            log.warn("Exception occured while saving photo : {}", e.getMessage(), e);
            throw new RuntimeException("S3 파일 저장 중 오류 발생", e);
        }
    }

    public String uploadToStorage(MultipartFile image, String userId, String objectKey){
        try {
            ensureBucketExists(storageProperties.getBucket());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(objectKey)
                    .contentType(image.getContentType())
                    .contentLength(image.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));

            return objectKey;
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 중 오류 발생", e);
        }
    }




    private void ensureBucketExists(String bucketName) {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(headBucketRequest);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.warn("버킷이 존재하지 않아 새로 생성합니다: {}", bucketName);
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                s3Client.createBucket(createBucketRequest);
            } else {
                log.error("버킷 확인 중 오류 발생: {} - {}", e.statusCode(), e.awsErrorDetails().errorMessage());
                throw e;
            }
        }
    }

    // 개발 환경용
    public String getMinIOStoragePhotoUrl(String objectName) throws Exception {
        // MinIO 클라이언트 생성
        MinioClient minioClient = MinioClient.builder()
                .endpoint(storageProperties.getEndpoint()) // MinIO 주소
                .credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey()) // 접속 키
                .build();

        // Presigned URL 생성 (예: 30분 동안 유효)
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET) // 다운로드용 presigned URL
                        .bucket(storageProperties.getBucket()) // 버킷 이름
                        .object(objectName) // 오브젝트 이름
                        .expiry(30, TimeUnit.MINUTES) // 유효 시간
                        .build()
        );
        return url;
    }

    /**
     * S3 호환 스토리지의 객체에 대한 미리 서명된 URL을 생성합니다.
     *
     * @param objectName 스토리지 내 객체의 키 (파일 이름)
     * @return 생성된 미리 서명된 URL 문자열, 실패 시 null
     */
    public String getPhotoUrl(String objectName) {

        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!isProd) {
            try{
                return getMinIOStoragePhotoUrl(objectName);
            }catch (Exception e){
                log.error("MinIO에서 미리 서명된 URL 생성 실패: {}", e.getMessage(), e);
                return null;
            }
        }

        // S3Presigner 빌더를 생성하고 기본 설정을 구성합니다.
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(storageProperties.getRegion()))
                // ec2에 직접 할당
                // .credentialsProvider(StaticCredentialsProvider.create(
                //         AwsBasicCredentials.create(
                //                 storageProperties.getAccessKey(),
                //                 storageProperties.getSecretKey()
                //         )))
                ;

        try (S3Presigner presigner = presignerBuilder.build()) {
            // 1. URL을 생성할 객체에 대한 요청을 만듭니다.
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(objectName)
                    .build();

            // 2. 미리 서명된 URL의 유효 기간(12시간)을 포함한 요청을 만듭니다.
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(12))
                    .getObjectRequest(getObjectRequest)
                    .build();

            // 3. 내부 엔드포인트 기준으로 미리 서명된 URL을 생성합니다.
            String internalPresignedUrl = presigner.presignGetObject(presignRequest).url().toString();

            return internalPresignedUrl;

        } catch (Exception e) {
            // URL 생성 중 오류 발생 시 에러 로그를 남기고 null을 반환합니다.
            log.error("미리 서명된 URL 생성에 실패했습니다. Object: {}", objectName, e);
            return null;
        }
    }

    public String saveAIPhoto(String base64Data, String userId, String fileExtension) {
        String objectName = null;
        try {
            if (base64Data == null || base64Data.trim().isEmpty()) {
                throw new IllegalArgumentException("Base64 데이터가 비어있습니다.");
            }

            // 확장자 정리 (점이 있으면 제거)
            String cleanExtension = fileUtil.cleanExtension(fileExtension);

            // 유니크한 파일명 생성
            String fileName = fileUtil.generateUniqueFileNameWithExtension(cleanExtension);

            // Base64 디코딩 후 파일 저장
            byte[] imageBytes = fileUtil.decodeBase64(base64Data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

            objectName = userId + "/" + fileName;

            ensureBucketExists(storageProperties.getBucket());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(objectName)
                    .contentType(fileUtil.getContentType(cleanExtension))
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, imageBytes.length));

            log.info("Base64 이미지 저장 완료 - 사용자: {}, 파일: {}, 크기: {} bytes", userId, fileName, imageBytes.length);
            return objectName;

        } catch (IllegalArgumentException e) {
            log.error("Base64 디코딩 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
            throw new RuntimeException("Base64 데이터가 올바르지 않습니다.", e);
        } catch (Exception e) {
            log.error("AI 이미지 저장 중 예상하지 못한 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("AI 이미지 저장 중 오류가 발생했습니다.", e);
        }
    }



}