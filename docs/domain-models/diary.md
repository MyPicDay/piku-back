# Diary 도메인 모델

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-15

## 도메인 개요

Diary 도메인은 **감정 일기 기록과 시각적 회고**를 담당한다. 사용자가 하루의 감정과 상황을 일기 내용과 생성 이미지로 기록하고, 날짜에 따라 축적된 기록을 다시 발견하도록 한다.

### 목적

- 사용자의 일상을 날짜 단위로 기록 및 보관할 수 있는 핵심 컨텐츠 단위를 제공한다.
- 생성 이미지를 달력에 표시하고, 일기 수와 사진 모아보기를 통해 축적된 기록을 회고할 수 있게 한다.
- 일기의 공개 범위(`DiaryVisibility`)를 통해 사용자가 컨텐츠의 노출 대상을 직접 제어할 수 있도록 한다.
- AI 생성 이미지와 사용자 업로드 이미지를 함께 관리하는 미디어 첨부 기능을 제공한다.

현재 모델은 감정을 별도의 값이나 통계로 저장하지 않는다. 감정은 사용자가 작성한 일기 내용과 AI 생성 이미지로만 표현된다.

### 핵심 책임

- 일기(`Diary`) 생성, 삭제(논리적 삭제) 및 월별 일기 조회(캘린더) 관리
- 일기 수, 월별 기록과 사진 모아보기를 위한 기록 조회 정보 제공
- 날짜 중복 방지 및 미래 날짜 작성 금지 검증
- AI 이미지(`DiaryPhotoType.AI_IMAGE`)와 사용자 업로드 이미지(`USER_IMAGE`)의 순서·대표 여부 지정 후 첨부
- 익명 일기의 내용 공개와 작성자 비식별 정책 관리

### 도메인 경계

- **Aggregate Root**: `Diary`
- `Photo`는 `Diary` Aggregate 내부에 속하며, `Diary` 없이는 독립적으로 존재하지 않는다.
- 현재 일기 작성에는 생성된 AI 이미지가 필수다. Diary는 기록에 사용할 이미지의 연결과 표시를 소유하지만 이미지 생성 과정과 생성 이력은 소유하지 않는다.
- 달력, 일기 수와 사진 모아보기는 Diary 기록의 조회 표현이다. 프로필 화면에서 사용하더라도 해당 정보의 의미와 공개 범위는 Diary가 소유한다.
- 작성자는 `userId`로 참조하며 Diary Aggregate는 사용자 계정의 상태와 프로필을 소유하지 않는다.
- 익명 일기의 외부 조회 표현은 실제 작성자 정보를 노출하지 않으며, 작성자 본인 여부만 별도 권한 정보로 제공한다.
- 사진 저장, 알림 전달과 본문 분석 기술은 Diary 모델의 책임이 아니다.

---

## 일기 상태(DiaryVisibility)

_Enum_

### 상수

- `PUBLIC` : 모든 사용자에게 공개
- `FRIENDS` : 친구에게만 공개
- `PRIVATE` : 본인에게만 공개 (비공개)
- `ANONYMOUS` : 내용은 모두에게 공개하되 작성자 정보는 공개하지 않음

### 공개 범위 정책

`DiaryVisibility`는 단순 표시 값이 아니라, 조회 가능 범위와 피드 노출 가능 여부를 함께 결정하는 정책 값이다.

#### FRIENDS 정책

- `FRIENDS` 일기의 전체 내용과 전체 이미지 조회 권한은 작성자 본인과 현재 친구 관계인 사용자만 가진다.
- 비친구는 `FRIENDS` 일기의 상세 정보와 상호작용 정보에 접근할 수 없다.
- 비친구에게는 `FRIENDS` 일기의 `diaryId`, 상세 본문, 댓글/좋아요 접근, 캘린더 노출, 통계 반영이 허용되지 않는다.
- 따라서 `FRIENDS`는 비친구에게 부분 응답을 주는 공개 범위가 아니라, 존재를 숨기는 제한 공개 범위로 해석한다.

#### PRIVATE 정책

- `PRIVATE` 일기의 **전체 내용과 전체 이미지 조회 권한은 작성자 본인만** 가진다.
- 작성자가 아닌 사용자는 `PRIVATE` 일기의 본문, 이미지, 대표 사진, 상세 정보에 접근할 수 없다.
- 작성자가 아닌 사용자에게는 `PRIVATE` 일기의 **존재 자체가 노출되면 안 된다.**
- 따라서 비소유자는 `PRIVATE` 일기의 `diaryId`, 대표 사진, 본문, 카운트, 월별 존재 여부를 알 수 없어야 한다.
- `PRIVATE` 일기는 **어떠한 경우에도 피드 추천 후보에 포함되어서는 안 된다.**
- 위 규칙은 일기 정보를 사용하는 모든 조회 표현과 추천 결과에 동일하게 적용된다.

#### ANONYMOUS 정책

- `ANONYMOUS` 일기의 본문과 이미지는 작성자와의 관계 또는 로그인 여부와 무관하게 조회할 수 있다.
- 외부 조회 표현의 실제 작성자 `userId`, 닉네임과 아바타는 요청자가 작성자 본인이어도 노출하지 않는다.
- 작성자 본인은 서버가 계산한 소유자 여부를 통해 자신의 일기임을 확인하고 수정·삭제 같은 작성자 권한을 행사한다.
- 작성자 프로필로 이동하거나 실제 친구 관계를 추정할 수 있는 정보는 제공하지 않는다.
- 익명 일기는 피드의 공개 후보에 포함할 수 있지만, 특정 사용자의 기록 소유를 드러내는 프로필·월별 목록·사진 갤러리·프로필 통계에는 작성자 본인이 조회할 때만 포함한다.
- 익명 일기의 댓글 내용과 댓글 작성자 정보는 Social 도메인이 익명 댓글 정책에 따라 보호한다.

#### 정책 해석 메모

- 비소유자에게는 `PRIVATE`가 "내용만 숨겨진 일기"가 아니라 **없는 것처럼 보여야 하는 일기**다.
- `ANONYMOUS`는 내용이 공개되어도 작성자의 신원과 사용자별 기록 소유 관계는 공개되지 않는 별도 공개 범위다.
- `DiaryVisibility`를 사용하는 외부 기능은 공개 범위의 의미를 자체적으로 바꾸지 않는다.

---

## 일기 사진 타입(DiaryPhotoType)

_Enum_

### 상수

- `AI_IMAGE` : AI로 생성된 이미지
- `USER_IMAGE` : 사용자가 직접 업로드한 이미지

### 행위

- `static fromString(String type)` : 문자열로부터 일치하는 사진 타입을 반환하며, 알맞은 형태가 없으면 예외를 발생시킨다.

---

## 사진 최적화 상태(PhotoOptimizationStatus)

_Enum_

### 상수

- `PENDING` : WebP 변환 대상이며 아직 처리되지 않은 상태
- `PROCESSING` : 백그라운드 최적화 작업이 처리권을 획득한 상태
- `SUCCEEDED` : WebP 최적화가 완료되어 `optimizedUrl`을 사용할 수 있는 상태
- `FAILED` : 최대 시도 횟수에 도달하여 더 이상 자동 재시도하지 않는 상태
- `SKIPPED` : 확장자가 없거나 지원하지 않는 형식이라 최적화 대상에서 제외된 상태

### 규칙

- `PhotoOptimizationStatus`는 `Photo`의 WebP 최적화 생명주기를 표현한다.
- 상태 전이는 도메인 생성 규칙과 백그라운드 최적화 작업에 의해 변경된다.
- 조회 응답은 상태 값 자체가 아니라 `Photo.getDisplayUrl()` 결과를 통해 최적화된 이미지 경로를 우선 사용한다.

---

## 일기(Diary)

_Entity_

### 속성

- `id` : Long. 일기의 고유 식별자
- `content` : String. 일기 본문 (최대 500자 제한)
- `status` : DiaryVisibility. 일기의 공개 범위 상태
- `date` : LocalDate. 일기가 기록된 대상 날짜
- `userId` : String. 일기를 작성한 사용자의 식별자
- `createdAt` : LocalDateTime. 최초 생성 일시
- `updatedAt` : LocalDateTime. 최종 수정 일시
- `deletedAt` : LocalDateTime. `Diary`가 소유하는 삭제 처리 일시

### 행위

- `static create()` : 내용을 기반으로 새로운 일기를 생성한다. (생성자)
- `delete()` : 일기를 논리적으로 삭제하고 `deletedAt`을 현재 시각으로 설정한다.
- `isDeleted()` : 일기가 삭제 처리되었는지 여부를 반환한다.
- `isOwner(String userId)` : 전달받은 사용자 ID가 해당 일기의 작성자인지 여부를 반환한다.

### 규칙

- 작성된 일기의 공개 범위(`status`)는 `PUBLIC`, `FRIENDS`, `PRIVATE`, `ANONYMOUS` 중 하나여야 한다.
- 일기 본문(`content`)의 길이는 최대 500자까지 허용된다.
- 일기는 특정 날짜(`date`)에 종속되어 기록된다.
- 시스템 상에서 사용자(`userId`)와 결합되어 식별된다.
- 일기는 물리적으로 제거하는 대신 `deletedAt` 값을 갖는 논리적 삭제 구조를 따른다.
- 현재 일기는 본문과 생성 이미지를 기록하며 감정 값이나 감정 통계를 별도로 보유하지 않는다.
- 삭제된 일기는 모든 조회와 집계에서 제외된다.
- 일기 삭제는 작성자만 수행할 수 있다.
- `PRIVATE` 일기는 작성자 본인 외에는 존재가 드러나면 안 되며, 이는 대표 이미지·캘린더 커버·통계 정보에도 동일하게 적용된다.
- `ANONYMOUS` 일기는 내용과 이미지를 공개하되 실제 작성자 정보와 다른 사용자의 사용자별 기록 화면에서는 소유 관계를 숨긴다.

---

## 일기 삭제

### 개요

일기 삭제는 물리적 삭제가 아닌 논리적 삭제 방식으로 처리된다. `Diary`가 소유한 `deletedAt`에 삭제 시각을 기록한다.

### 규칙

- 작성자만 자신의 일기를 삭제할 수 있다.
- 삭제된 일기는 다시 삭제할 수 있는 활성 일기로 취급하지 않는다.
- 삭제된 일기는 상세, 달력, 사진 모아보기, 피드 후보와 모든 통계에서 제외한다.
- 삭제는 일기의 사진과 공개 범위가 더 이상 노출되지 않게 한다.

---

## 사진(Photo)

_Entity_

### 속성

- `id` : int. 사진의 고유 식별자
- `url` : String. 원본 사진 이미지가 저장소에 위치한 object key
- `optimizedUrl` : String. WebP 최적화가 완료된 사진의 object key
- `optimizationStatus` : PhotoOptimizationStatus. 사진의 WebP 최적화 상태
- `optimizedAt` : LocalDateTime. WebP 최적화 완료 시각
- `optimizationAttemptCount` : Integer. WebP 최적화 실패 시도 횟수
- `optimizationLastAttemptAt` : LocalDateTime. 마지막 WebP 최적화 시도 시각
- `represent` : Boolean. 해당 일기의 대표(썸네일) 사진 여부
- `photoOrder` : Integer. 일기 내에서 여러 장의 사진이 위치하는 순서
- `sourceType` : DiaryPhotoType. AI 생성 이미지와 사용자 업로드 이미지를 구분하는 출처 타입
- `diary` : Diary. 이 사진이 첨부된 부모 일기 엔티티

### 행위

- `updateRepresent(Boolean represent)` : 이 사진이 대표 사진인지 여부를 수정한다.
- `getDisplayUrl()` : `optimizedUrl`이 있으면 최적화된 object key를 반환하고, 없으면 원본 `url`을 반환한다.
- `markOptimizationSucceeded(String optimizedUrl)` : WebP 최적화 완료 object key를 저장하고 상태를 `SUCCEEDED`로 변경한다.

### 규칙

- 하나의 일기(`Diary`)에 여러 장의 사진(`Photo`)이 종속될 수 있다.
- 사진 생성 시 기본적으로 대표 사진 여부(`represent`)는 `false`로 설정된다.
- 사진 생성 시 기본적으로 `optimizationAttemptCount`는 `0`으로 설정된다.
- 일기별 사진 정렬을 위해 `photoOrder`를 기준으로 순서를 보장한다.
- 사진 생성 시 원본 `url`의 확장자를 기준으로 최적화 상태를 초기화한다.
- `jpg`, `jpeg`, `png`, `bmp` 확장자는 `PENDING`으로 초기화되어 WebP 최적화 스케줄링 대상이 된다.
- `webp` 확장자는 이미 최적화된 것으로 보고 `optimizedUrl = url`, `optimizationStatus = SUCCEEDED`, `optimizedAt = 현재 시각`으로 초기화한다.
- 확장자가 없거나 지원하지 않는 확장자는 `SKIPPED`로 초기화한다.
- 일기 상세, 캘린더 커버, 피드 후보 등 사진 경로를 노출하는 조회 흐름은 가능한 경우 `optimizedUrl`을 우선 사용해야 한다.
