# Feed 도메인 모델

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-04-12

## 도메인 개요

Feed 도메인은 **여러 사용자의 일기를 정책 기반으로 정렬해 탐색하게 하고, 사용자의 열람 행위 이력을 수집·관리**하는 도메인입니다. 수집된 클릭 이력은 향후 추천 시스템의 입력 데이터로 활용됩니다.

### 목적

- `PUBLIC` 또는 `FRIENDS` 공개 범위 일기를 cursor 기반 정책 랭킹으로 탐색할 수 있도록 피드를 제공한다.
- 일기 열람 행위(`FeedClick`)를 기록함으로써 사용자 행위 기반 선호도 데이터를 수집한다.
- 로그인 사용자 기준으로 친구/비친구, consumed/not consumed를 구분해 우선순위를 제공한다.

### 핵심 책임

- 피드 클릭 이력(`FeedClick`) 생성 및 중복 클릭 방지
- 클릭 발생 시 일기 토픽 조회 후 사용자 선호도(`recordInteraction`) 자동 업데이트
- 피드 목록을 `cursor -> bucket query -> read model materialize` 순서로 orchestration
- 피드 목록 응답 DTO에 좋아요 수, 댓글 수, 현재 사용자 좋아요 여부, 작성자 닉네임·아바타·친구 상태 포함
- 일기 상세 조회 시 접근 권한(소유자 / 친구 / 비친구)을 `DiaryVisibility` 정책으로 검증

### 도메인 경계

- **Aggregate Root**: `FeedClick`
- `FeedClick`은 독립적인 로그 성격의 Aggregate이며, `Diary`나 `User`의 상태에 직접 영향을 주지 않는다.
- 현재 피드 목록은 `FeedQueryService`가 cursor 해석과 bucket 순회를 담당하고, `FeedCursorPersistenceAdapter`가 후보 조회를 담당하며, `FeedListViewPersistenceAdapter`가 read model 조립을 담당한다.
- `Recommendation` 도메인은 현재 목록 정렬보다 클릭 후 선호도 기록(`recordInteraction`)에 가깝게 사용된다.

### 타 도메인과의 관계

| 도메인             | 관계 설명                                                                                        |
| ------------------ | ------------------------------------------------------------------------------------------------ |
| **User**           | 작성자의 닉네임·아바타 URL과 친구 상태를 피드 응답에 포함한다.                                      |
| **Diary**          | 피드 후보 일기와 상세 일기의 공개 범위, 본문, 사진을 제공한다.                                     |
| **Social**         | 좋아요·댓글 수, 현재 사용자의 좋아요 여부, 친구 관계를 피드 응답에 포함한다.                       |
| **Recommendation** | 클릭 후 토픽 선호도 기록과 metadata topic 조회를 위임한다. 현재 cursor 목록 정렬의 직접 주체는 아니다. |

### 상세 동작 문서

- 현재 구현 기준의 상세 흐름은 [RECOMMENDATION_FLOW.md](/Users/yk/piku/piku-back/RECOMMENDATION_FLOW.md) 를 따른다.

---

## 피드 후보 정책

Feed 도메인은 [`Diary` 도메인이 정의한 공개 범위 정책](DIARY.md)을 그대로 따른다.
피드 후보 수집기, 추천 점수 계산기, 캐시 복원 로직은 모두 아래 규칙을 지켜야 한다.

### 공개 범위별 후보 규칙

- 현재 피드를 요청한 사용자의 일기는 공개 범위와 무관하게 피드 추천 후보에서 제외되어야 한다.
- `PUBLIC` : 피드 추천 후보에 포함될 수 있다.
- `FRIENDS` : 현재 조회 사용자가 작성자의 친구인 경우에만 피드 추천 후보에 포함될 수 있다.
- `PRIVATE` : **어떠한 경우에도 피드 추천 후보에 포함되어서는 안 된다.**

### PRIVATE 정책 준수 규칙

- 현재 요청 사용자의 own diary는 신규 후보 수집, 캐시 복원, 재정렬, 페이지 materialize 어느 단계에서도 다시 포함되면 안 된다.
- `PRIVATE` 일기는 신규 후보 수집 시점에 제외되어야 한다.
- `PRIVATE` 일기는 캐시 hit 복원 시점에도 다시 검증되어 제외되어야 한다.
- `PRIVATE` 일기는 추천 점수 계산, 후보 병합, 정렬, 페이지 materialize 어느 단계에서도 다시 살아나면 안 된다.
- `PRIVATE` 일기는 피드 상세 조회에서도 비소유자에게 `404`로 숨겨져야 하며, `diaryId`와 이미지가 노출되면 안 된다.

### 상세 조회 규칙

- `PUBLIC` 상세는 누구나 조회할 수 있다.
- `FRIENDS` 상세는 작성자 본인과 친구만 조회할 수 있다.
- 비친구는 `FRIENDS` 상세를 조회할 때 `404`로 숨겨져야 하며, 부분 응답을 받아서는 안 된다.
- `PRIVATE` 상세는 작성자 본인만 조회할 수 있다.

### 구현 메모

- Feed는 `DiaryVisibility`를 직접 새로 정의하지 않고 `Diary` 정책의 소비자 역할만 수행한다.
- 따라서 `PRIVATE` 관련 정책 변경이 발생하면 `Feed` 문서는 동작 규칙만 갱신하고, 정책의 원문 정의는 `DIARY.md`를 기준으로 유지한다.

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
