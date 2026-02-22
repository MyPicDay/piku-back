# Diary 도메인 모델

## 도메인 개요

Diary 도메인은 **Piku 서비스의 핵심 도메인**으로, 사용자가 날짜별로 텍스트 기반 일기를 작성하고 사진을 첨부하는 기록 기능 전체를 담당합니다.

### 목적

- 사용자의 일상을 날짜 단위로 기록 및 보관할 수 있는 핵심 컨텐츠 단위를 제공한다.
- 일기의 공개 범위(`DiaryVisibility`)를 통해 사용자가 컨텐츠의 노출 대상을 직접 제어할 수 있도록 한다.
- AI 생성 이미지(`Creative` 도메인)와 사용자 업로드 이미지를 함께 관리하는 미디어 첨부 기능을 제공한다.

### 핵심 책임

- 일기(`Diary`) 생성, 삭제(논리적 삭제) 및 월별 일기 조회(캘린더) 관리
- 날짜 중복 방지 및 미래 날짜 작성 금지 검증
- AI 이미지(`DiaryPhotoType.AI_IMAGE`)와 사용자 업로드 이미지(`USER_IMAGE`)의 순서·대표 여부 지정 후 첨부
- 일기 공개 범위가 `FRIENDS`인 경우, 생성 시점에 친구 목록을 조회하여 `FRIEND_DIARY` 알림 발송 트리거
- 일기 본문을 `recommendation` 도메인에 비동기 분석 요청 (실패 시 무시, 메인 플로우에 영향 없음)

### 도메인 경계

- **Aggregate Root**: `Diary`
- `Photo`는 `Diary` Aggregate 내부에 속하며, `Diary` 없이는 독립적으로 존재하지 않는다.
- 사용자 정보는 `LoadUserForDiaryPort`, 사진 저장은 `PhotoStoragePort`, 알림 발송은 `SendDiaryNotificationPort`를 통해 외부 도메인/인프라로 위임한다.
- AI 이미지 원본 데이터는 `Creative` 도메인(`LoadCreativePort`)에서 관리하며, 일기 생성 시 `diaryId`를 갱신하여 연결한다.

### 타 도메인과의 관계

| 도메인             | 관계 설명                                                                                        |
| ------------------ | ------------------------------------------------------------------------------------------------ |
| **User**           | `userId`로 일기 작성자를 식별하고, `LoadUserForDiaryPort`를 통해 사용자 존재 여부를 확인한다.    |
| **Social**         | `FriendUseCase`를 통해 친구 목록을 조회하고, `FRIENDS` 일기 생성 시 알림 대상 목록으로 활용한다. |
| **Notification**   | `SendDiaryNotificationPort`를 통해 친구들에게 `FRIEND_DIARY` 알림을 발송한다.                    |
| **Creative**       | `LoadCreativePort`로 AI 생성 이미지 데이터를 조회하고, 첨부 완료 후 `diaryId`를 갱신한다.        |
| **Recommendation** | `AnalyzeDiaryContentUseCase`를 통해 일기 본문을 분석하여 토픽/키워드 메타데이터를 생성한다.      |
| **Feed**           | `Diary`가 `LoadDiaryForFeedPort`를 통해 피드 후보 목록으로 소비된다.                             |

---

## 일기 상태(DiaryVisibility)

_Enum_

### 상수

- `PUBLIC` : 모든 사용자에게 공개
- `FRIENDS` : 친구에게만 공개
- `PRIVATE` : 본인에게만 공개 (비공개)

---

## 일기 사진 타입(DiaryPhotoType)

_Enum_

### 상수

- `AI_IMAGE` : AI로 생성된 이미지
- `USER_IMAGE` : 사용자가 직접 업로드한 이미지

### 행위

- `static fromString(String type)` : 문자열로부터 일치하는 사진 타입을 반환하며, 알맞은 형태가 없으면 예외를 발생시킨다.

---

## 일기(Diary)

_Entity_

### 속성

- `id` : Long. 일기의 고유 식별자
- `content` : String. 일기 본문 (최대 500자 제한)
- `status` : DiaryVisibility. 일기의 공개 범위 상태
- `date` : LocalDate. 일기가 기록된 대상 날짜
- `userId` : String. 일기를 작성한 사용자의 식별자
- `createdAt` : LocalDateTime. 최초 생성 일시 (`BaseEntity` 공통)
- `updatedAt` : LocalDateTime. 최종 수정 일시 (`BaseEntity` 공통)
- `deletedAt` : LocalDateTime. 삭제 처리 일시 (`BaseEntity` 공통, 소프트 삭제용)

### 행위

- `static create()` : 내용을 기반으로 새로운 일기를 생성한다. (생성자)
- `delete()` : 일기를 삭제 처리한다. 내부적으로 `BaseEntity.inactive()`를 호출하여 `deletedAt`을 현재 시각으로 설정한다.
- `isOwner(String userId)` : 전달받은 사용자 ID가 해당 일기의 작성자인지 여부를 반환한다.
- `inactive()` : `deletedAt`을 현재 시각으로 설정하여 논리적 삭제를 수행한다. (`BaseEntity` 공통)

### 규칙

- 작성된 일기의 공개 범위(`status`)는 `PUBLIC`, `FRIENDS`, `PRIVATE` 중 하나여야 한다.
- 일기 본문(`content`)의 길이는 최대 500자까지 허용된다.
- 일기는 특정 날짜(`date`)에 종속되어 기록된다.
- 시스템 상에서 사용자(`userId`)와 결합되어 식별된다.
- 데이터는 실제 DB에서 삭제되는 대신 `deletedAt` 값을 갖는 논리적 삭제 구조를 따른다.
- 삭제된 일기는 모든 조회 쿼리에서 `deletedAt IS NULL` 조건을 통해 명시적으로 제외된다.
- 일기 삭제는 본인(작성자)만 수행할 수 있으며, 타인의 일기 삭제 시도 시 `DiaryAccessDeniedException`이 발생한다.

---

## 일기 삭제

### 개요

일기 삭제는 물리적 삭제가 아닌 논리적 삭제 방식으로 처리된다.
`BaseEntity`의 `deletedAt` 필드에 삭제 시각이 기록되며, 이후 각 조회 쿼리에 명시적으로 `deletedAt IS NULL` 조건이 포함되어 삭제된 레코드가 제외된다.

### 삭제 흐름

1. 사용자가 `DELETE /api/diary/{diaryId}` API를 호출한다.
2. 해당 일기가 존재하는지 확인한다. (존재하지 않으면 `DiaryNotFoundException`)
3. 요청한 사용자가 일기의 작성자인지 검증한다. (불일치 시 `DiaryAccessDeniedException`)
4. `Diary.delete()`를 호출하여 `deletedAt`을 현재 시각으로 설정한다.
5. 변경된 엔티티를 저장한다.
6. `204 No Content` 응답을 반환한다.

### 조회 시 삭제 레코드 제외 방식

각 Repository 쿼리에 명시적으로 `deletedAt IS NULL` 조건을 추가하여, 삭제된 일기를 조회 결과에서 제외한다.

- **파생 쿼리(Derived Query)** : 메서드명에 `AndDeletedAtIsNull` 조건을 포함한다.
- **JPQL 쿼리** : `WHERE` 절에 `AND d.deletedAt IS NULL` 조건을 추가한다.

### 예외

- `DiaryNotFoundException` : 삭제 대상 일기가 존재하지 않거나 이미 삭제된 경우 발생한다.
- `DiaryAccessDeniedException` : 요청자가 일기 작성자가 아닌 경우 발생한다.

---

## 사진(Photo)

_Entity_

### 속성

- `id` : int. 사진의 고유 식별자
- `url` : String. 사진 이미지가 저장소에 위치한 경로(URL)
- `represent` : Boolean. 해당 일기의 대표(썸네일) 사진 여부
- `photoOrder` : Integer. 일기 내에서 여러 장의 사진이 위치하는 순서
- `diary` : Diary. 이 사진이 첨부된 부모 일기 엔티티

### 행위

- `updateRepresent(Boolean represent)` : 이 사진이 대표 사진인지 여부를 수정한다.

### 규칙

- 하나의 일기(`Diary`)에 여러 장의 사진(`Photo`)이 종속될 수 있다.
- 사진 생성 시 기본적으로 대표 사진 여부(`represent`)는 `false`로 설정된다.
- 일기별 사진 정렬을 위해 `photoOrder`를 기준으로 순서를 보장한다.
