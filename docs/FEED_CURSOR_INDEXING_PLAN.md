# Feed Cursor Indexing Plan

작성일: 2026-03-08

## 문서 목적

- cursor 기반 피드 조회에서 왜 인덱스가 필요한지 설명한다.
- 어떤 인덱스를 우선 추가해야 하는지 정리한다.
- 인덱스로 해결되는 문제와, 인덱스만으로는 해결되지 않는 구조적 한계를 구분한다.
- 배포 전후에 무엇을 검증해야 하는지 기준을 남긴다.

---

## 배경

현재 피드 목록은 bucket 단위 cursor query로 동작한다.

핵심 구현은 [FeedCursorPersistenceAdapter.java](/Users/yk/piku/piku-back/src/main/java/com/pikume/back/feed/adapter/out/persistence/FeedCursorPersistenceAdapter.java) 에 있다.

현재 쿼리의 특징은 다음과 같다.

- `diary`를 기준 테이블로 사용한다.
- `likes`, `comments`를 집계해서 `likeCount`, `commentCount`를 만든다.
- `feed_click`, `likes`, `comments`에 대해 `EXISTS`로 consumed 여부를 판별한다.
- 친구 bucket에서는 `friend` 테이블을 조회한다.
- 현재 bucket 내부 정렬은 `createdAt DESC`, `likeCount DESC`, `commentCount DESC`, `diaryId DESC` 이다.

즉 이 쿼리는 단순한 최신 diary 조회가 아니라,

- visibility 필터
- friendship 판별
- consumed 판별
- engagement 집계 정렬

을 한 번에 수행한다.

이 구조에서는 인덱스가 없으면 `rows examined`가 급격히 커지고, 대규모 트래픽에서 DB 병목이 바로 발생한다.

---

## 현재 쿼리에서 비싼 부분

### 1. consumed 판별 `EXISTS`

현재 consumed 조건은 아래 3가지를 본다.

- `feed_click`
- `likes`
- `comments`

즉 diary 후보 하나마다 아래 형태의 탐색이 반복된다.

- `WHERE user_id = :currentUserId AND diary_id = d.id`

이 패턴은 `(user_id, diary_id)` 또는 이에 준하는 복합 인덱스가 없으면 비싸다.

특히 `feed_click`은 현재 baseline schema상 PK만 있고 보조 인덱스가 없다.  
[V1__baseline.sql](/Users/yk/piku/piku-back/src/main/resources/db/migration/V1__baseline.sql)

이 상태에서는 사용자의 click 기록이 누적될수록 consumed bucket 판별 비용이 선형으로 나빠진다.

### 2. `likeCount`, `commentCount` 집계

정렬을 위해 `likes`, `comments`에서 diary별 count를 만든다.

```sql
SELECT diary_id, COUNT(id)
FROM likes
WHERE deleted_at IS NULL
GROUP BY diary_id
```

```sql
SELECT diary_id, COUNT(id)
FROM comments
WHERE deleted_at IS NULL
GROUP BY diary_id
```

이 집계는 `diary_id`, `deleted_at` 기준 인덱스가 없으면 테이블 스캔 비용이 커진다.

### 3. diary 후보 필터

현재 diary는 아래 축으로 필터된다.

- `deleted_at IS NULL`
- `status`
- `user_id <> :currentUserId`
- 필요 시 `user_id`와 friendship 조건

즉 `status`, `deleted_at`, `user_id`, `created_at`, `id` 조합을 자주 탄다.

이쪽 인덱스가 없으면 diary 테이블도 큰 범위 스캔으로 이어질 수 있다.

### 4. friendship 판별

친구 bucket / 공개 bucket 모두 `friend` 테이블에 대해 아래 조건을 사용한다.

```sql
WHERE (f.user_id_1 = :currentUserId AND f.user_id_2 = d.user_id)
   OR (f.user_id_2 = :currentUserId AND f.user_id_1 = d.user_id)
```

현재 `friend`는 PK가 `(user_id_1, user_id_2)` 이므로 첫 번째 방향에는 유리하지만, 반대 방향은 별도 인덱스가 없으면 불리할 수 있다.

---

## 왜 인덱스가 필요한가

요약하면 이유는 4가지다.

### 1. consumed 여부를 빠르게 판별해야 한다

피드는 사용자별 상태가 중요하다.

- `NOT_CONSUMED_FRIEND`
- `NOT_CONSUMED_PUBLIC`
- `CONSUMED_FRIEND`
- `CONSUMED_PUBLIC`

이 bucket 분리는 결국 `user_id + diary_id` 탐색 성능에 달려 있다.
이 경로가 느리면 첫 페이지부터 DB가 버벅인다.

### 2. count 집계 정렬은 기본적으로 비싸다

현재 정렬은 최신성을 우선으로 두되 popularity 신호를 함께 포함한다.
그러면 DB는 diary row만 보는 게 아니라 `likes`, `comments`도 같이 본다.

인덱스가 없으면:

- 집계 서브쿼리 스캔이 무거워지고
- 임시 테이블/정렬 비용이 커지고
- bucket 4개 조회가 누적되어 요청 1건 비용이 크게 뛴다

### 3. cursor는 첫 페이지와 다음 몇 페이지가 매우 자주 호출된다

cursor는 deep page 비용을 줄여주지만, 첫 페이지와 앞쪽 페이지 호출량은 오히려 매우 높다.
즉 “가끔 느린 쿼리”가 아니라 “항상 자주 실행되는 기본 쿼리”이기 때문에 인덱스가 필요하다.

### 4. 인덱스 없이 스케일링하면 DB가 먼저 병목된다

이 문제는 앱 CPU보다 DB I/O와 rows examined 문제다.
앱 서버를 늘려도 SQL 자체가 비싸면 근본 해결이 안 된다.

---

## 우선 추가 권장 인덱스

아래는 v1 cursor 배포 기준으로 우선순위가 높은 인덱스다.

## P0. consumed 판별 인덱스

### 1. `feed_click(user_id, diary_id)`

목적:

- `EXISTS (WHERE user_id = ? AND diary_id = ?)` 최적화

이유:

- 현재 `feed_click`에는 이 용도의 인덱스가 없다.
- click 누적이 늘수록 consumed 판별이 바로 느려진다.

권장 예시:

```sql
CREATE INDEX idx_feed_click_user_diary
ON feed_click (user_id, diary_id);
```

### 2. `comments(user_id, diary_id, deleted_at)`

목적:

- 댓글 작성 기반 consumed 판별 최적화

이유:

- 현재 consumed 판별에서 `comments`도 `EXISTS`로 본다.
- 사용자별 댓글 여부 조회가 빨라져야 한다.

권장 예시:

```sql
CREATE INDEX idx_comments_user_diary_deleted
ON comments (user_id, diary_id, deleted_at);
```

### 3. `likes`는 기존 `UNIQUE (user_id, diary_id)` 활용

현재 `likes`는 이미 아래 유니크 인덱스를 가진다.

- `uk_user_diary (user_id, diary_id)`

즉 consumed 판별의 `likes` 경로는 상대적으로 덜 급하다.
다만 count 집계용 인덱스는 별도로 필요하다.

---

## P0. count 집계 인덱스

### 4. `likes(diary_id, deleted_at)`

목적:

- diary별 like count 집계 최적화

이유:

- 현재 `GROUP BY diary_id` 집계를 자주 수행한다.
- `deleted_at IS NULL` 필터도 같이 사용한다.

권장 예시:

```sql
CREATE INDEX idx_likes_diary_deleted
ON likes (diary_id, deleted_at);
```

### 5. `comments(diary_id, deleted_at)`

목적:

- diary별 comment count 집계 최적화

이유:

- `comments`도 동일하게 count 집계를 반복한다.

권장 예시:

```sql
CREATE INDEX idx_comments_diary_deleted
ON comments (diary_id, deleted_at);
```

---

## P1. diary 후보 필터 인덱스

### 6. `diary(status, deleted_at, created_at, id)`

목적:

- 공개 bucket의 기본 후보 범위 조회 최적화

이유:

- `PUBLIC` / `FRIENDS` / `deleted_at IS NULL` 조건을 자주 사용한다.
- 정렬 키에 `created_at`, `id`가 들어간다.

권장 예시:

```sql
CREATE INDEX idx_diary_status_deleted_created_id
ON diary (status, deleted_at, created_at, id);
```

### 7. `diary(user_id, status, deleted_at, created_at, id)`

목적:

- 작성자 축 필터가 있는 경우 최적화

이유:

- 친구 작성자/본인 제외/사용자별 상태 탐색에서 `user_id`가 계속 사용된다.

권장 예시:

```sql
CREATE INDEX idx_diary_user_status_deleted_created_id
ON diary (user_id, status, deleted_at, created_at, id);
```

주의:

- 현재 정렬이 `likeCount`, `commentCount` 우선이라 diary 인덱스만으로 정렬 전체를 해결할 수는 없다.
- 그래도 candidate 범위를 줄이는 데는 유의미하다.

---

## P1. friendship 판별 인덱스

### 8. `friend(user_id_2, user_id_1)`

목적:

- 양방향 friendship 검사에서 반대 방향 탐색 보완

이유:

- 현재 PK는 `(user_id_1, user_id_2)` 뿐이다.
- 쿼리는 `(user_id_2 = :currentUserId AND user_id_1 = d.user_id)` 도 본다.

권장 예시:

```sql
CREATE INDEX idx_friend_user2_user1
ON friend (user_id_2, user_id_1);
```

---

## 인덱스로 해결되는 것과 해결되지 않는 것

## 인덱스로 해결되는 것

- consumed `EXISTS` 비용 감소
- like/comment count 집계 접근 비용 감소
- diary 후보 필터 비용 감소
- friendship lookup 비용 감소

## 인덱스로만 해결되지 않는 것

- popularity 정렬을 위해 전체 `likes/comments` 집계를 매 요청 계산하는 구조
- bucket 4개를 순차 조회하는 구조 자체의 누적 비용
- 요청마다 popularity가 계속 바뀌는 eventual consistency 특성

중요한 결론:

인덱스는 필요하지만 충분조건은 아니다.

현재 정렬에 `likeCount`, `commentCount`가 포함되는 한,
아무리 인덱스를 보강해도 “매 요청 popularity 집계” 비용은 완전히 없어지지 않는다.

---

## 구조적으로 더 좋은 다음 단계

만약 최신성을 더 우선하고 popularity를 보조로 내리고 싶다면,
구조적으로 더 유리해진다.

예:

- `createdAt DESC`
- `likeCount DESC`
- `commentCount DESC`
- `diaryId DESC`

이 방향이면:

- diary 인덱스를 더 잘 활용할 수 있고
- 우선 최신 candidate를 좁게 잡은 뒤
- 그 집합 안에서 popularity를 보조로 처리하는 방향이 가능하다

더 나아가면 아래도 고려할 수 있다.

- diary에 denormalized `like_count`, `comment_count` 유지
- feed 전용 summary/materialized view 도입
- popularity는 precomputed counter 사용

즉 현재 인덱스 작업은 필수이지만,
정렬 정책이 최신성 우선으로 바뀌면 구조도 더 건강해진다.

---

## 배포 전 검증 방법

배포 전에는 H2가 아니라 MySQL 계열의 실제 실행계획으로 본다.

필수 체크:

1. bucket별 쿼리 `EXPLAIN ANALYZE`
2. 첫 페이지 요청 기준 `rows examined`
3. `Using temporary`, `Using filesort` 여부
4. p95 응답 시간

권장 목표:

- bucket query 1개: `50~100ms` 이내
- 첫 페이지 전체: `200ms` 안쪽부터 시작

확인 대상 쿼리:

- `NOT_CONSUMED_FRIEND`
- `NOT_CONSUMED_PUBLIC`
- `CONSUMED_FRIEND`
- `CONSUMED_PUBLIC`

그리고 아래 데이터 분포로 봐야 한다.

- 친구 수가 적은 사용자
- 친구 수가 많은 사용자
- click/like/comment 이력이 많은 사용자
- 공개 diary가 많은 시점

---

## 권장 적용 순서

1. `feed_click(user_id, diary_id)`
2. `likes(diary_id, deleted_at)`
3. `comments(diary_id, deleted_at)`
4. `comments(user_id, diary_id, deleted_at)`
5. `diary(status, deleted_at, created_at, id)`
6. `diary(user_id, status, deleted_at, created_at, id)`
7. `friend(user_id_2, user_id_1)`

---

## 최종 정리

현재 cursor 피드는 인덱스 없이 운영하기엔 위험하다.

가장 먼저 필요한 이유는:

- consumed 판별이 사용자별 상태 조회이기 때문
- popularity 집계가 매 요청 수행되기 때문
- cursor feed가 매우 자주 호출되는 핵심 읽기 경로이기 때문

다만 인덱스는 필수 조건이지 최종 해결책은 아니다.

장기적으로는 아래 중 하나가 같이 가야 한다.

- 최신성 우선 정렬로 단순화
- popularity counter 사전 계산
- feed summary/materialized read model 도입
