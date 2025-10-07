# 일기 대표 이미지 Public 경로 자동 저장 구현

## 📌 개요

일기의 대표 이미지(`order = 0`)를 `public/` 경로에 자동으로 저장하여 공개 URL로 접근 가능하게 하고, 브라우저 캐싱을 통한 성능 최적화를 구현한 작업입니다.

## 🎯 구현 내용

### 핵심 기능
- ✅ **사용자 업로드 사진**: 대표 사진 업로드 시 자동으로 `public/` 경로에 저장
- ✅ **AI 생성 사진**: 대표 사진으로 사용 시 기존 경로에서 `public/` 경로로 자동 이동
- ✅ **DB 동기화**: `photos` 테이블과 `diary_image_generation` 테이블의 경로 자동 업데이트
- ✅ **트랜잭션 안전성**: 파일 저장/이동 실패 시 자동 롤백으로 데이터 정합성 보장
- ✅ **캐싱 최적화**: MinIO에 Cache-Control 메타데이터 설정, API는 no-cache로 즉시 반영

---

## 🔄 신규 데이터 처리 플로우

### 1️⃣ 사용자 업로드 사진이 대표 사진인 경우

**API 요청:**
```json
POST /api/diary
{
  "content": "오늘의 일기",
  "date": "2025-10-07",
  "imageInfos": [
    {
      "type": "USER_IMAGE",
      "photoIndex": 0,
      "order": 0  // ← 대표 사진
    }
  ]
}
```

**처리 플로우:**
```
1. DiaryController.createDiary()
   ↓
2. DiaryService.createDiary()
   ↓ photos[0], order=0
3. PhotoStorageService.savePhoto()
   ↓
4. order == 0 확인
   ├─ true: objectName = "public/user1/20251007_abc123.png"
   │        cacheControl = "public, max-age=31536000, immutable"
   └─ false: objectName = "user1/20251007_abc123.png"
   ↓
5. MinIO에 업로드 (Cache-Control 메타데이터 포함)
   ↓
6. Photo 엔티티 저장
   - url: "public/user1/20251007_abc123.png"
   - represent: true
```

**결과:**
- ✅ MinIO: `public/user1/20251007_abc123.png` (Cache-Control 메타데이터 포함)
- ✅ DB photos: `url = "public/user1/20251007_abc123.png"`, `represent = true`

---

### 2️⃣ AI 생성 사진이 대표 사진인 경우

**API 요청:**
```json
POST /api/diary
{
  "content": "오늘의 일기",
  "date": "2025-10-07",
  "imageInfos": [
    {
      "type": "AI_IMAGE",
      "aiPhotoId": 123,
      "order": 0  // ← 대표 사진
    }
  ]
}
```

**처리 플로우:**
```
1. DiaryController.createDiary()
   ↓
2. DiaryService.createDiary()
   ↓ aiPhotoId=123, order=0
3. DiaryService.saveAiPhoto()
   ↓
4. DiaryImageGeneration 조회
   - filePath: "user1/20251007_xyz789.png" (기존 경로)
   ↓
5. order == 0 확인
   ├─ true: PhotoStorageService.moveToPublic() 호출
   └─ false: 그대로 사용
   ↓
6. PhotoStorageService.moveToPublic()
   ├─ 파일 복사: user1/20251007_xyz789.png
   │              → public/user1/20251007_xyz789.png
   │              (Cache-Control 메타데이터 포함)
   ├─ 복사 확인
   ├─ 원본 파일 삭제
   └─ 반환: "public/user1/20251007_xyz789.png"
   ↓
7. DiaryImageGeneration.updateFilePath()
   - filePath: "public/user1/20251007_xyz789.png" (업데이트)
   ↓
8. Photo 엔티티 저장
   - url: "public/user1/20251007_xyz789.png"
   - represent: true
```

**결과:**
- ✅ MinIO: `public/user1/20251007_xyz789.png` (이동됨, Cache-Control 메타데이터 포함)
- ✅ DB photos: `url = "public/user1/20251007_xyz789.png"`, `represent = true`
- ✅ DB diary_image_generation: `file_path = "public/user1/20251007_xyz789.png"` (동기화)

---

## 🔒 트랜잭션 안전성

### AI 사진 이동 시 (moveToPublic)

```
1. 파일 복사
   ├─ 성공 → 2번으로
   └─ 실패 → 예외 발생, DB 업데이트 안 함 ✓

2. 복사 확인
   ├─ 확인됨 → 3번으로
   └─ 확인 실패 → 예외 발생, DB 업데이트 안 함 ✓

3. 원본 파일 삭제
   ├─ 성공 → 4번으로
   └─ 실패 → 복사본 삭제 (롤백) ✓

4. 반환 (DiaryService에서 DB 저장)
```

**보장 사항:**
- ✅ 파일이 이동되지 않으면 DB도 변경 안 됨
- ✅ 파일 이동 실패 시 복사본 자동 삭제
- ✅ 파일과 DB의 상태 항상 일치

---

## 📂 저장 경로 규칙

### 대표 사진 (order = 0, represent = true)
```
MinIO: public/user1/20251007_abc123.png
DB:    public/user1/20251007_abc123.png
```
- ✅ 공개 URL로 접근 가능
- ✅ 브라우저/CDN 캐싱 1년

### 일반 사진 (order > 0, represent = false)
```
MinIO: user1/20251007_def456.png
DB:    user1/20251007_def456.png
```
- ✅ Presigned URL로만 접근
- ✅ 캐싱 없음

---

## 🚀 브라우저 캐싱 전략

### 1️⃣ 이미지 파일 (MinIO에서 직접 서빙)

**MinIO 응답 헤더:**
```http
GET /piku/public/user1/20251007_abc123.png

HTTP/1.1 200 OK
Cache-Control: public, max-age=31536000, immutable
Content-Type: image/png
```

**설정 위치:**
- `PhotoStorageService.savePhoto()` - 사용자 업로드 시 직접 public 경로에 저장
- `PhotoStorageService.moveToPublic()` - AI 사진 이동 시 캐시 헤더 포함

**캐싱 정책:**
- `public`: CDN 캐싱 허용
- `max-age=31536000`: 1년간 캐싱
- `immutable`: 절대 변경되지 않음 (브라우저가 재검증 안 함)

**효과:**
- ✅ 네트워크 요청 제거 (1년간)
- ✅ 페이지 로딩 속도 향상
- ✅ MinIO 부하 감소

---

### 2️⃣ API 응답 (DiaryController)

#### 월별 일기 목록 API
```java
GET /api/diary/user/{userId}/monthly?year=2025&month=10

HTTP/1.1 200 OK
Cache-Control: no-cache, must-revalidate
```

**설정:**
```java
return ResponseEntity.ok()
    .cacheControl(CacheControl.noCache().mustRevalidate())
    .body(diaries);
```

**이유:** 새 일기 작성 시 즉시 반영 필요

---

#### 이미지 프록시 API (필요한 경우)
```java
GET /api/diary/images/{userId}/{filename}

HTTP/1.1 200 OK
Cache-Control: public, max-age=86400, immutable
```

**설정:**
```java
return ResponseEntity.ok()
    .cacheControl(CacheControl
        .maxAge(1, TimeUnit.DAYS)
        .cachePublic()
        .immutable())
    .body(resource);
```

---

## 📊 전체 아키텍처

```
┌─────────────────┐
│   브라우저       │
└────────┬────────┘
         │
         │ GET /api/diary/user/123/monthly
         │ Cache-Control: no-cache
         ↓
┌─────────────────┐
│ DiaryController │ ← 항상 서버에서 최신 데이터 응답
└────────┬────────┘
         │
         ↓
    ┌────────┐
    │   DB   │ ← URL: public/user1/photo.png
    └────────┘

브라우저가 이미지 로드:
         │
         │ GET http://localhost:9000/piku/public/user1/photo.png
         │ Cache-Control: public, max-age=31536000, immutable
         ↓
┌─────────────────┐
│     MinIO       │ ← 메타데이터에서 Cache-Control 헤더 반환
└─────────────────┘

브라우저 캐시:
  ✅ 이미지는 1년간 로컬 캐시에서 로드
  ✅ 네트워크 요청 0회
```

---

## 📊 캐싱 동작 및 효과

### 캐싱 동작 방식
- **첫 방문**: API 요청 + 이미지 다운로드
- **재방문**: API 요청 (서버 재검증) + 이미지 캐시 사용 (네트워크 요청 없음)

### 구현 효과
- ✅ **네트워크 트래픽 감소**: 이미지가 1년간 브라우저 캐시에 저장
- ✅ **서버 부하 감소**: MinIO로의 반복적인 이미지 요청 제거
- ✅ **데이터 최신성 보장**: API는 항상 서버에서 재검증하여 새 일기 즉시 반영
- ✅ **CDN 연동 준비**: public 헤더로 CDN 캐싱 가능

---

## 📁 파일 구조

```
piku-back/
├── src/main/java/store/piku/back/
│   ├── diary/
│   │   ├── controller/
│   │   │   └── DiaryController.java          # API 캐싱 설정
│   │   ├── service/
│   │   │   ├── DiaryService.java             # AI 사진 처리
│   │   │   └── PhotoStorageService.java      # 파일 저장/이동, 캐시 헤더
│   │   ├── entity/
│   │   │   └── Photo.java
│   │   └── constants/
│   │       └── PhotoConstants.java           # PUBLIC_PREFIX = "public/"
│   └── ai/
│       └── entity/
│           └── DiaryImageGeneration.java     # filePath 동기화
└── PUBLIC_PHOTO_MIGRATION_README.md         # 이 문서
```

---

## 🧪 테스트 시나리오

### 1. 사용자 업로드 사진 (대표)
```bash
# 1. 일기 작성
POST /api/diary
{
  "imageInfos": [
    { "type": "USER_IMAGE", "photoIndex": 0, "order": 0 }
  ]
}

# 2. MinIO 확인
ls minio-data/piku/public/user1/
# → 20251007_abc123.png 존재

# 3. 캐시 헤더 확인
curl -I http://localhost:9000/piku/public/user1/20251007_abc123.png
# Cache-Control: public, max-age=31536000, immutable

# 4. DB 확인
SELECT url, represent FROM photos WHERE diary_id=?;
# → url: public/user1/20251007_abc123.png, represent: true
```

### 2. AI 생성 사진 (대표)
```bash
# 1. 일기 작성
POST /api/diary
{
  "imageInfos": [
    { "type": "AI_IMAGE", "aiPhotoId": 123, "order": 0 }
  ]
}

# 2. MinIO 확인
ls minio-data/piku/public/user1/
# → 20251007_xyz789.png 존재 (이동됨)

ls minio-data/piku/user1/
# → 원본 삭제됨 ✓

# 3. DB 확인
SELECT file_path FROM diary_image_generation WHERE id=123;
# → public/user1/20251007_xyz789.png (동기화됨)

SELECT url, represent FROM photos WHERE diary_id=?;
# → url: public/user1/20251007_xyz789.png, represent: true
```

### 3. 브라우저 캐싱 테스트
```bash
# 1. 첫 방문 (DevTools Network 탭)
GET /api/diary/user/user1/monthly
# Status: 200 OK (서버에서 응답)
# Cache-Control: no-cache, must-revalidate

GET /piku/public/user1/20251007_abc123.png
# Status: 200 OK (다운로드)
# Cache-Control: public, max-age=31536000, immutable
# Size: 250 KB

# 2. 재방문 (새로고침)
GET /api/diary/user/user1/monthly
# Status: 200 OK (서버 재검증)

GET /piku/public/user1/20251007_abc123.png
# Status: 200 OK (from disk cache)
# Size: (disk cache) ← 네트워크 요청 없음!
```

---

## ⚠️ 주의사항

### 1. 파일명은 절대 변경하지 않음
- 파일명에 타임스탬프/UUID 포함 → 유니크함 보장
- `immutable` 캐싱은 파일이 **절대 변경되지 않을 때만** 사용 가능

### 2. 대표 사진 변경 시
- 기존 대표 사진: `public/` 경로 유지 (삭제 안 함)
- 새 대표 사진: 새로운 파일로 `public/` 경로에 저장
- 오래된 파일은 별도 정리 작업 필요

### 3. 캐시 무효화
- 파일명이 바뀌므로 자동으로 캐시 무효화
- URL이 바뀜: `.../photo_old.png` → `.../photo_new.png`

---

## 🔧 환경별 설정

### 개발 환경 (MinIO)
```yaml
# application-dev.yml
storage:
  endpoint: http://localhost:9000
  bucket: piku
  access-key: minioadmin
  secret-key: minioadmin
```

### 프로덕션 환경 (AWS S3)
```yaml
# application-prod.yml
storage:
  bucket: piku-prod
  region: ap-northeast-2
  # access-key/secret-key: EC2 IAM Role 사용
```

---

## 📚 참고 자료

- [HTTP Caching - MDN](https://developer.mozilla.org/en-US/docs/Web/HTTP/Caching)
- [Cache-Control Header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control)
- [AWS S3 Object Metadata](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingMetadata.html)

---

## 🎯 구현 완료

대표 이미지가 자동으로 `public/` 경로에 저장되며, 브라우저 캐싱을 통한 성능 최적화가 적용되었습니다.

### 주요 성과
- ✅ **자동화**: 사용자 업로드/AI 사진 모두 자동으로 public 경로 처리
- ✅ **데이터 정합성**: 파일과 DB 상태 항상 일치 (트랜잭션 롤백)
- ✅ **캐싱 최적화**: 이미지는 1년간 브라우저 캐싱, API는 즉시 반영
- ✅ **CDN 준비**: public 헤더로 CDN 연동 가능

