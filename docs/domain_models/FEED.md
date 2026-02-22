# Feed 도메인 모델

## 도메인 개요

Feed 도메인은 **여러 사용자의 공개 일기를 개인화된 형태로 탐색하게 하고, 사용자의 열람 행위 이력을 수집·관리**하는 도메인입니다. 수집된 클릭 이력은 향후 추천 시스템의 핵심 데이터로 활용됩니다.

### 목적

- `PUBLIC` 또는 `FRIENDS` 공개 범위 일기를 추천 점수 기반으로 개인화하여 탐색할 수 있도록 피드를 제공한다.
- 일기 열람 행위(`FeedClick`)를 기록함으로써 사용자 행위 기반 선호도 데이터를 수집한다.
- 피드 결과를 캐시하고, 클릭 발생 시 캐시를 무효화하여 최신 선호도가 반영된 피드를 제공한다.

### 핵심 책임

- 피드 클릭 이력(`FeedClick`) 생성 및 중복 클릭 방지
- 클릭 발생 시 일기 토픽 조회 후 사용자 선호도(`recordInteraction`) 자동 업데이트
- 클릭 발생 시 추천 피드 캐시 무효화(`invalidateCache`)
- 친구 일기 후보와 공개 일기 후보를 구분하여 `FeedCompositionService`로 위임, 친구 일기 약 30% 고정 슬롯 + 추천 점수 정렬
- 피드 목록 응답 DTO에 좋아요 수, 댓글 수, 현재 사용자 좋아요 여부, 작성자 닉네임·아바타·친구 상태 포함
- 일기 상세 조회 시 접근 권한(소유자 / `PUBLIC` / `FRIENDS` + 친구 여부) 검증

### 도메인 경계

- **Aggregate Root**: `FeedClick`
- `FeedClick`은 독립적인 로그 성격의 Aggregate이며, `Diary`나 `User`의 상태에 직접 영향을 주지 않는다.
- 피드 후보 수집(`FeedCandidateCollector`), 피드 구성(`FeedCompositionService`), 피드 조회(`FeedQueryService`)는 Application 계층에서 분리된 역할을 갖는다.
- 추천 캐시와 점수 계산은 `Recommendation` 도메인(`LoadRecommendationForFeedPort`)에 위임한다.

### 타 도메인과의 관계

| 도메인             | 관계 설명                                                                                        |
| ------------------ | ------------------------------------------------------------------------------------------------ |
| **User**           | `LoadUserForFeedPort`를 통해 일기 작성자의 닉네임·아바타 URL을 피드 응답에 포함한다.             |
| **Diary**          | `LoadDiaryForFeedPort`를 통해 피드 후보 일기 목록 및 사진 URL을 조회한다.                        |
| **Social**         | `LoadSocialForFeedPort`를 통해 친구 관계·좋아요·댓글 수를 피드 DTO에 포함한다.                   |
| **Recommendation** | `LoadRecommendationForFeedPort`를 통해 추천 점수 조회, 캐시 저장/무효화, 선호도 기록을 위임한다. |

---

## 피드 클릭(FeedClick)

_Entity_

### 속성

- `id` : Long. 피드 클릭 이력의 고유 식별자
- `userId` : String (UUID 36자리). 피드를 클릭하여 조회한 사용자의 고유 식별자
- `diaryId` : Long. 클릭의 대상이 된 다이어리 식별자
- `clickedAt` : LocalDateTime. 다이어리를 클릭하여 열람을 시작한 일시
- `viewDurationSeconds` : Integer. 사용자가 해당 다이어리를 조회하며 체류한 총 시간(초 단위)

### 행위

- `static Builder()` : 새로운 피드 클릭 이력을 초기화하여 생성한다. (클릭 시간 자동 부여)
- `updateViewDuration(Integer seconds)` : 사용자가 체류한 최종 시간을 업데이트한다.

### 규칙

- `userId`, `diaryId`는 필수 값으로 결측이 불가능하다.
- 피드 클릭 생성 시점(`clickedAt`)에는 서버의 현재 시간이 자동으로 기록된다.
- 체류 시간(`viewDurationSeconds`)은 초기에는 빈 값(Null)일 수 있으며, 조회가 종료되는 시점에 시스템에 의해 업데이트될 수 있다.
