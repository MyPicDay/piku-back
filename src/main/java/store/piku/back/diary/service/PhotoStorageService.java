package store.piku.back.diary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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
import java.net.URI;
import java.time.Duration;

@Slf4j
@Service
public class PhotoStorageService {

    private final S3Client s3Client;
    private final PhotoUtil photoUtil;
    private final PhotoRepository photoRepository;
    private final StorageProperties storageProperties;
    private final FileUtil fileUtil;

    public PhotoStorageService(S3Client s3Client, PhotoUtil photoUtil, PhotoRepository photoRepository,
                               StorageProperties storageProperties, FileUtil fileUtil) {
        this.s3Client = s3Client;
        this.photoUtil = photoUtil;
        this.photoRepository = photoRepository;
        this.storageProperties = storageProperties;
        this.fileUtil = fileUtil;
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
            throw new IOException("S3 파일 저장 중 오류 발생", e);
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

        /**
     * S3 호환 스토리지의 객체에 대한 미리 서명된 URL을 생성합니다.
     *
     * @param objectName 스토리지 내 객체의 키 (파일 이름)
     * @return 생성된 미리 서명된 URL 문자열, 실패 시 null
     */
    public String getPhotoUrl(String objectName) {
        // S3Presigner 빌더를 생성하고 기본 설정을 구성합니다.
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                storageProperties.getAccessKey(),
                                storageProperties.getSecretKey()
                        )));

        // MinIO와 같은 S3 호환 스토리지를 위한 엔드포인트가 설정된 경우 적용합니다.
        final String endpoint = storageProperties.getEndpoint();
        if (endpoint != null && !endpoint.isEmpty()) {
            presignerBuilder.endpointOverride(URI.create(endpoint));
        }

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

            // 4. 외부 접근용 publicUrl이 설정된 경우, URL의 호스트 부분을 교체합니다.
            final String publicUrl = storageProperties.getPublicUrl();
            if (publicUrl != null && !publicUrl.isEmpty()) {
                // 예: http://minio-service:9000/... -> https://cdn.example.com/...
                return internalPresignedUrl.replace(endpoint, publicUrl);
            }

            return internalPresignedUrl;

        } catch (Exception e) {
            // URL 생성 중 오류 발생 시 에러 로그를 남기고 null을 반환합니다.
            log.error("미리 서명된 URL 생성에 실패했습니다. Object: {}", objectName, e);
            return null;
        }
    }




    // public String getPhotoUrl(String objectName) {
    //     log.info("storageProperties endpoint: {}", storageProperties.getEndpoint());
    //     log.info("MinIO Endpoint: {}", storageProperties.getEndpoint());


    //     try {
    //         // 1. S3 객체 요청 생성: 특정 버킷에서 주어진 키(objectName)를 가진 객체를 가져오기 위한 요청을 만듭니다.
    //         GetObjectRequest getObjectRequest = GetObjectRequest.builder()
    //                 .bucket(storageProperties.getBucket())
    //                 .key(objectName)
    //                 .build();

    //         // 2. 서명 만료 시간 설정: 생성될 미리 서명된 URL의 유효 기간을 12시간으로 설정합니다.
    //         Duration expiration = Duration.ofHours(12);

    //         // 3. S3Presigner 빌더 초기화: S3 객체에 대한 미리 서명된 URL을 생성하는 데 사용될 S3Presigner를 구성합니다.
    //         S3Presigner.Builder presignerBuilder = S3Presigner.builder()
    //                 .credentialsProvider(StaticCredentialsProvider.create(
    //                         AwsBasicCredentials.create(
    //                                 storageProperties.getAccessKey(),
    //                                 storageProperties.getSecretKey()
    //                         )))
    //                 .region(Region.of(storageProperties.getRegion()));

    //         // 4. 엔드포인트 오버라이드 (MinIO 지원): MinIO와 같은 S3 호환 스토리지 사용을 위해 엔드포인트를 동적으로 설정합니다.
    //         if (storageProperties.getEndpoint() != null && !storageProperties.getEndpoint().isEmpty()) {
    //             presignerBuilder.endpointOverride(URI.create(storageProperties.getEndpoint())); // ex) http://localhost:9000
    //             log.info("MinIO Endpoint: {}", storageProperties.getEndpoint());
    //         }

    //         // 5. S3Presigner 생성 및 URL 생성: 구성된 빌더를 사용하여 S3Presigner 인스턴스를 생성하고, try-with-resources 구문으로 자동 리소스 관리를 보장합니다.
    //         try (S3Presigner presigner = presignerBuilder.build()) {
    //             // 6. 미리 서명된 URL 요청 생성: 만료 시간과 S3 객체 요청을 포함하여 최종 요청을 구성합니다.
    //             GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
    //                     .signatureDuration(expiration)
    //                     .getObjectRequest(getObjectRequest)
    //                     .build();

    //             // 7. 미리 서명된 URL 생성 실행: presignGetObject 메서드를 호출하여 실제 URL을 생성합니다.
    //             String presignedUrl = presigner.presignGetObject(presignRequest).url().toString();

    //             // 8. Public URL로 변환: 외부에서 접근 가능한 Public URL이 설정된 경우, 내부 엔드포인트 주소를 Public URL로 교체합니다.
    //             if (storageProperties.getPublicUrl() != null && !storageProperties.getPublicUrl().isEmpty()) {
    //                 return presignedUrl.replace(storageProperties.getEndpoint(), storageProperties.getPublicUrl());
    //             }

    //             // 9. 생성된 URL 반환: Public URL 설정이 없으면 생성된 presignedUrl을 그대로 반환합니다.
    //             return presignedUrl;
    //         }

    //     } catch (Exception e) {
    //         // 10. 예외 처리: URL 생성 과정에서 오류가 발생하면 로그를 남기고 null을 반환합니다.
    //         log.warn("Presigned URL 생성 실패: {}", e.getMessage(), e);
    //         return null;
    //     }
    // }

    /**
     * 파일 다운로드를 위한 미리 서명된 URL을 생성합니다.
     * 이 코드는 S3와 MinIO 모두에서 동일하게 동작합니다.
     * @param objectName 파일 키 (경로 포함 파일명)
     * @return 생성된 URL
     */
//    public URL generateDownloadUrl(String objectName) {
//        try {
//            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                    .bucket(storageProperties.getBucket())
//                    .key(objectName)
//                    .build();
//
//
//            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
//                    .signatureDuration(Duration.ofHours(12))
//                    .getObjectRequest(getObjectRequest)
//                    .build();
//
//            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
//            return presignedRequest.url();
//
//        } catch (Exception e) {
//            // 실제 프로덕션 코드에서는 더 구체적인 예외 처리를 권장합니다.
//            throw new RuntimeException("URL 생성 중 오류가 발생했습니다.", e);
//        }
//    }




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