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

import static store.piku.back.diary.constants.PhotoConstants.PUBLIC_PREFIX;

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
                boolean isPublic = (order != null && order == 0);
                objectName = isPublic ? PUBLIC_PREFIX + userId + "/" + filename : userId + "/" + filename;

                ensureBucketExists(storageProperties.getBucket());

                PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                        .bucket(storageProperties.getBucket())
                        .key(objectName)
                        .contentType(photo.getContentType())
                        .contentLength(photo.getSize());
                
                // public 파일인 경우 캐시 헤더 추가
                if (isPublic) {
                    requestBuilder.cacheControl("public, max-age=31536000, immutable");  // 1년간 캐싱
                }
                
                PutObjectRequest putObjectRequest = requestBuilder.build();

                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(photo.getInputStream(), photo.getSize()));

                Photo savePhoto = new Photo(diary, objectName, order);
                if (isPublic) {
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
    public String getMinIOStoragePhotoUrl(String objectName, boolean isPublic) throws Exception {
        // MinIO 클라이언트 생성
        MinioClient minioClient = MinioClient.builder()
                .endpoint(storageProperties.getEndpoint()) // MinIO 주소
                .credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey()) // 접속 키
                .build();
        if (isPublic) {
            return storageProperties.getEndpoint() + "/" + storageProperties.getBucket() + "/" + objectName;
        }

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
    public String getPhotoUrl(String objectName, boolean isPublic) {

        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!isProd) {
            try{
                return getMinIOStoragePhotoUrl(objectName, isPublic);
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

    /**
     * MinIO/S3에서 파일을 public/ 경로로 이동합니다. (복사 후 원본 삭제)
     * 대표 사진을 공개 URL로 제공하기 위해 사용됩니다.
     *
     * @param sourceKey 원본 파일 경로 (예: "user1/photo.png")
     * @return 이동된 파일의 새 경로 (예: "public/user1/photo.png")
     */
    public String moveToPublic(String sourceKey) {
        String targetKey = PUBLIC_PREFIX + sourceKey;
        boolean fileCopied = false;
        
        try {
            // 이미 public/으로 시작하면 이동하지 않음
            if (sourceKey.startsWith(PUBLIC_PREFIX)) {
                log.info("이미 public 경로입니다: {}", sourceKey);
                return sourceKey;
            }
            
            // 타겟이 이미 존재하는지 확인
            if (objectExists(targetKey)) {
                log.info("public 경로에 이미 파일이 존재합니다. 원본 삭제: {}", sourceKey);
                // 타겟이 존재하면 원본만 삭제
                deleteObject(sourceKey);
                return targetKey;
            }
            
            // 소스 파일 존재 확인
            if (!objectExists(sourceKey)) {
                log.error("소스 파일이 존재하지 않습니다: {}", sourceKey);
                throw new RuntimeException("소스 파일을 찾을 수 없습니다: " + sourceKey);
            }
            
            // S3 객체 복사 (캐시 헤더 포함)
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(storageProperties.getBucket())
                    .sourceKey(sourceKey)
                    .destinationBucket(storageProperties.getBucket())
                    .destinationKey(targetKey)
                    .cacheControl("public, max-age=31536000, immutable")  // 1년간 캐싱, 변경 불가
                    .build();
            
            s3Client.copyObject(copyRequest);
            fileCopied = true;
            
            // 복사 확인
            if (!objectExists(targetKey)) {
                throw new RuntimeException("파일 복사 후 확인 실패: " + targetKey);
            }
            
            // 원본 파일 삭제
            deleteObject(sourceKey);
            
            log.info("파일 이동 완료: {} → {} (원본 삭제됨)", sourceKey, targetKey);
            return targetKey;
            
        } catch (S3Exception e) {
            log.error("S3 파일 이동 실패: {} → {}, 오류: {}", sourceKey, targetKey, e.getMessage(), e);
            
            // 복사는 성공했지만 원본 삭제 실패 시 복사본 삭제 (롤백)
            if (fileCopied && objectExists(targetKey)) {
                try {
                    deleteObject(targetKey);
                    log.info("롤백: 복사된 파일 삭제 완료: {}", targetKey);
                } catch (Exception rollbackException) {
                    log.error("롤백 실패: {}", rollbackException.getMessage());
                }
            }
            
            throw new RuntimeException("파일 이동 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("파일 이동 중 예상하지 못한 오류 발생: {}", e.getMessage(), e);
            
            // 복사는 성공했지만 예외 발생 시 복사본 삭제 (롤백)
            if (fileCopied && objectExists(targetKey)) {
                try {
                    deleteObject(targetKey);
                    log.info("롤백: 복사된 파일 삭제 완료: {}", targetKey);
                } catch (Exception rollbackException) {
                    log.error("롤백 실패: {}", rollbackException.getMessage());
                }
            }
            
            throw new RuntimeException("파일 이동 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * S3/MinIO 객체 존재 여부 확인
     */
    private boolean objectExists(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(key)
                    .build();
            
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("객체 존재 확인 중 오류: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * S3/MinIO 객체 삭제
     */
    private void deleteObject(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(key)
                    .build();
            
            s3Client.deleteObject(request);
            log.info("파일 삭제 완료: {}", key);
        } catch (S3Exception e) {
            log.error("S3 파일 삭제 실패: {}, 오류: {}", key, e.getMessage(), e);
            throw e;
        }
    }

}