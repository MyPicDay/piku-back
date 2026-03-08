# Feed Cursor Backend PRD

작성일: 2026-03-08

## 문서 목적

- 기존 offset 기반 피드 조회를 cursor 기반 조회로 전환하기 위한 백엔드 작업 범위를 정의한다.
- 추천 모델 고도화가 아니라 `정책 기반 랭킹`과 `대규모 트래픽 대응`을 목표로 한다.
- 읽음/미읽음 기준, API 계약, 단계별 구현 순서, 완료 기준을 명확히 한다.

---

## 배경

현재 피드는 다음 특성을 가진다.

- 구조적으로는 추천 시스템보다 `정책 기반 피드 + 약한 개인화 보정`에 가깝다.
- 개인화 신호는 아직 `CLICK` 중심이라 품질을 강하게 신뢰하기 어렵다.
- offset 기반 페이지네이션은 deep page로 갈수록 비용이 커지고, 후보 상한을 두면 API 계약이 흔들린다.
- 피드 특성상 대부분의 소비는 앞 페이지에서 발생하므로, cursor 기반 무한 스크롤이 더 자연스럽다.

---

## 문제 정의

### 1. offset 기반은 피드에 적합하지 않다

- `page`, `size`, `totalElements` 기반 계약은 deep page 비용이 크다.
- 후보 범위 제한을 적용하면 실제 제공 가능한 전체 페이지 수와 계약이 어긋난다.
- 랭킹 정책이 변경되거나 visibility가 변하면 뒤 페이지 일관성이 약해진다.

### 2. 읽음/미읽음 기준이 불명확하다

- 지금은 사실상 `click`만 읽음에 가까운 신호로 쓰고 있다.
- 단순 클릭 기준은 실제 소비와 다르고, 피드 노출 여부도 반영하지 못한다.
- 피드 정책을 정교하게 하려면 read-state를 먼저 정의해야 한다.

### 3. 추천 모델보다 정책이 먼저여야 한다

- 현재 데이터와 신호 품질로는 복잡한 점수 모델보다 설명 가능한 랭킹 정책이 더 적합하다.
- 커서 기반으로 가려면 정렬 규칙이 안정적이어야 하므로, bucket 중심 정책이 필요하다.
- cursor 후보 조회는 인덱스 전제가 필요한 쿼리다. 상세한 인덱스 근거와 우선순위는 [FEED_CURSOR_INDEXING_PLAN.md](/Users/yk/piku/piku-back/docs/FEED_CURSOR_INDEXING_PLAN.md) 를 따른다.

---

## 목표

- 피드 목록 API를 cursor 기반 계약으로 전환한다.
- 피드 정렬을 `정책 기반 랭킹`으로 명확히 정의한다.
- 읽음/미읽음 대신 `consumed 여부` 중심의 상태 모델을 도입한다.
- 대규모 트래픽에서도 후보 전체 전수 조회 없이 안정적으로 동작하게 만든다.
- 기존 visibility 정책, 특히 `PRIVATE` 제외 규칙을 유지한다.

---

## 비목표

- personalized reranker를 이번 단계에서 도입하지 않는다.
- CTR/ML 모델/학습 기반 추천으로 확장하지 않는다.
- 프론트 전체 UX를 이 문서에서 결정하지 않는다.
- feed detail API를 전면 개편하지 않는다.

---

## 정책 기준

## 1. Visibility

- `PRIVATE`
  - 피드 후보에 절대 포함되지 않는다.
  - cache 복원, 후보 수집, cursor 조회 어느 단계에서도 노출되면 안 된다.
- `FRIENDS`
  - 현재 친구 관계가 유효한 경우에만 후보가 된다.
- `PUBLIC`
  - 본인을 제외한 누구에게나 후보가 될 수 있다.

## 2. Read State

이번 단계에서는 `read/unread` 대신 `consumed/not consumed`를 기준으로 사용한다.

- `NOT_CONSUMED`
  - 사용자가 상세 조회, 좋아요, 댓글 등 명시적 소비 행위를 하지 않은 상태
- `CONSUMED`
  - 아래 중 하나 이상 만족
  - feed card 클릭 후 상세 진입
  - 좋아요
  - 댓글 작성

향후 확장:

- `UNSEEN`
- `SEEN`
- `CONSUMED`

단, 이번 백엔드 1차 작업에서는 `CONSUMED 여부`만 우선 반영한다.

## 3. Ranking Bucket

기본 bucket 우선순위는 다음과 같다.

1. `NOT_CONSUMED_FRIEND`
2. `NOT_CONSUMED_PUBLIC`
3. `CONSUMED_FRIEND`
4. `CONSUMED_PUBLIC`

각 bucket 내부 정렬:

- `createdAt DESC`
- `likeCount DESC`
- `commentCount DESC`
- `diaryId DESC`

이번 단계에서는 최신성을 우선하고, `likeCount`, `commentCount`는 보조 정렬로 포함한다.
단, `likeCount`, `commentCount`는 요청 사이에 바뀔 수 있으므로 strict snapshot 일관성은 보장하지 않는다.

### 4. Cursor Consistency Policy

v1 cursor 피드는 `eventually consistent`를 허용한다.

- 사용자가 페이지를 넘기는 도중
- click
- like
- comment
- friendship 변경
- visibility 변경

같은 상태 변화가 발생하면 다음 페이지에서 중복 또는 누락이 생길 수 있다.

이번 단계에서는 이를 허용한다.

- `snapshotAt` 기반의 세션 고정 일관성은 도입하지 않는다.
- 프론트는 `diaryId` 기준 dedup을 적용한다.
- 백엔드는 strict no-duplicate/no-gap을 목표로 하지 않는다.

---

## API 방향

## 1. 신규 목록 API

기존:

- `GET /api/diary?page=0&size=20`

신규:

- `GET /api/diary?cursor=...&limit=20`

기존 목록 endpoint 경로는 유지하고, query contract만 cursor 기반으로 전환한다.
즉 v1 cursor 전환의 기준 경로는 `/api/diary`다.

권장 반환 형식:

```json
{
  "items": [],
  "nextCursor": "base64-encoded-cursor",
  "hasNext": true
}
```

더 이상 `totalElements`, `totalPages`에 의존하지 않는다.

## 2. Cursor 구성

cursor는 최소 다음 정보를 가진다.

- `bucket`
- `likeCount`
- `commentCount`
- `createdAt`
- `diaryId`

예시 개념:

```json
{
  "bucket": "NOT_CONSUMED_PUBLIC",
  "likeCount": 12,
  "commentCount": 3,
  "createdAt": "2026-03-08T10:00:00",
  "diaryId": 123
}
```

권장 방식:

- JSON 직렬화 후 base64 인코딩
- 서버 검증 실패 시 `400 Bad Request`

## 3. API 전환 정책

이번 작업은 신규 endpoint를 추가하지 않고 기존 목록 endpoint를 교체하는 방향으로 진행한다.

- 경로는 `/api/diary` 유지
- 목록 query contract는 `page/size`에서 `cursor/limit`으로 변경
- controller/service/integration 테스트도 함께 교체
- 상세 조회 endpoint `/api/diary/{diaryId}`는 유지

---

## 아키텍처 방향

헥사고날 + DDD 구조를 유지하면서 아래처럼 분리한다.

- application
  - `GetFeedCursorUseCase`
  - `FeedCursorPage`
  - `FeedCursorRequest`
  - `FeedBucket`
  - `FeedCursorTokenCodec`
- port out
  - `LoadFeedCursorCandidatesPort`
  - `LoadFeedConsumptionStatePort`
  - `LoadFeedListViewPort` 재사용 여부 검토
- adapter out
  - bucket별 cursor query
  - current friendship/visibility filtering
  - consumed 여부 batch 조회

중요한 원칙:

- 서비스는 cursor 해석, bucket 전환, 응답 조립 orchestration만 담당한다.
- 실제 bucket query 책임은 outbound adapter에 둔다.
- Web DTO는 port 반환 타입으로 쓰지 않는다.

---

## 상세 작업 항목

## Task 1. Cursor 계약과 응답 모델 도입

### 작업 내용

- `FeedCursorRequest`, `FeedCursorPage`, `FeedBucket`, `FeedCursor` 정의
- cursor encode/decode 유틸 또는 컴포넌트 추가
- 기존 `/api/diary` controller query contract를 cursor 방식으로 변경
- 기존 목록 endpoint 테스트를 cursor 기준으로 교체

### 완료 기준

- backend 내부에서 cursor를 타입으로 다룬다.
- controller가 기존 `/api/diary` 경로에서 `page/size`가 아닌 `cursor/limit` 계약을 제공한다.

### 권장 커밋 메시지

- `feat: 피드 cursor 조회 계약과 응답 모델 도입`

## Task 2. Consumed 상태 조회 포트 도입

### 작업 내용

- `FeedClick`, `Like`, `Comment`를 기반으로 consumed 여부를 판별하는 조회 포트 추가
- diaryId 집합 기준 batch 조회 지원
- application에서 `NOT_CONSUMED / CONSUMED` bucket 판별 가능하게 구성

### 완료 기준

- 단건 반복 조회 없이 diaryId 집합 기준으로 consumed 여부를 판별한다.

### 권장 커밋 메시지

- `feat: 피드 consumed 상태 조회 포트 추가`

## Task 3. Bucket별 cursor candidate query 구현

### 작업 내용

- `NOT_CONSUMED_FRIEND`
- `NOT_CONSUMED_PUBLIC`
- `CONSUMED_FRIEND`
- `CONSUMED_PUBLIC`

각 bucket에 대해:

- visibility 조건
- self exclusion
- friendship 조건
- consumed exclusion/inclusion 조건
- `(likeCount, commentCount, createdAt, diaryId)` cursor 조건
- `limit + 1` 조회

### 완료 기준

- bucket 단위로 안정적인 다음 페이지 조회가 가능하다.
- offset 없이도 `hasNext` 판별이 된다.

### 권장 커밋 메시지

- `feat: 피드 bucket별 cursor 후보 조회 구현`

## Task 4. Feed cursor service orchestration 구현

### 작업 내용

- 현재 bucket에서 `limit` 만큼 채우고 부족하면 다음 bucket으로 넘어가는 orchestration 구현
- 각 bucket에서 가져온 id를 read model query로 materialize
- nextCursor 계산

### 완료 기준

- cursor 기반으로 여러 bucket을 순차 소비할 수 있다.
- page 번호 없이도 무한 스크롤이 가능하다.

### 권장 커밋 메시지

- `feat: 피드 cursor orchestration 서비스 구현`

## Task 5. 기존 read model 조합 재사용 정리

### 작업 내용

- 현재 `LoadFeedListViewPort`를 cursor API에서도 재사용할지 결정
- materialize 시 순서 보존 유지
- `PRIVATE`, self, invalid id 필터 정책 유지

### 완료 기준

- cursor path에서도 현재 목록 read model 품질과 쿼리 최적화가 유지된다.

### 권장 커밋 메시지

- `refactor: 피드 cursor 조회에서 read model materialize 경로 재사용`

## Task 6. 이벤트/상태 전이용 API 보강

### 작업 내용

- impression 이벤트 수집 API 추가 여부 검토
- dwell time 저장 API 또는 detail exit 기반 집계 방식 정의
- click/logging API와 consumed 상태 연결 방식 정리

### 완료 기준

- 프론트가 필요한 이벤트를 보낼 수 있는 최소 계약이 확정된다.

### 권장 커밋 메시지

- `feat: 피드 소비 상태 이벤트 계약 추가`

## Task 7. 테스트 보강

### 단위 테스트

- cursor decode 실패
- bucket 전환 순서
- nextCursor 계산
- consumed/not consumed 우선순위 보장

### 통합 테스트

- H2 기준 cursor paging 동작 검증
- bucket 경계 이동 검증
- `PRIVATE` 제외 검증
- 친구 관계 변경 후 visibility 반영 검증
- row 수 증가 시 query count bound 검증

### 완료 기준

- offset API와 별도로 cursor API 회귀 테스트가 존재한다.

### 권장 커밋 메시지

- `test: 피드 cursor 조회와 bucket 정책 회귀 검증 추가`

---

## 데이터 및 저장 모델 고려사항

### 1. 지금 바로 재사용 가능한 신호

- `FeedClick`
- `Like`
- `Comment`

### 2. 추후 추가할 신호

- impression
- dwell time
- hide/not interested

### 3. 권장 원칙

- 1차에서는 기존 테이블을 최대한 재사용
- 2차부터 노출 로그 테이블 도입 검토

---

## 리스크

### 1. cursor 안정성

- 정렬 기준이 흔들리면 중복/누락이 생긴다.
- v1은 eventual consistency를 허용한다.
- bucket 내부 정렬 키는 문서에 정의된 순서로 고정돼야 한다.

### 2. consumed 정의의 제품 합의

- click만 consumed로 볼지
- like/comment까지 포함할지
- 노출은 언제부터 seen으로 볼지

이 기준이 확정돼야 프론트/백엔드 이벤트 계약도 정리된다.

### 3. 기존 page API 제거 전환 리스크

- 이번 전환은 기존 목록 endpoint를 교체하므로, 배포 타이밍과 프론트 전환 순서 관리가 필요하다.
- 목록 API 테스트도 cursor 계약 기준으로 함께 수정돼야 한다.

---

## 성공 기준

- 피드 목록이 cursor 기반으로 조회된다.
- deep page 비용이 offset 대비 안정적으로 줄어든다.
- `PRIVATE`는 어느 단계에서도 피드 후보에 노출되지 않는다.
- consumed/not consumed 기준이 코드와 문서에 고정된다.
- 프론트가 `nextCursor` 기반 무한 스크롤로 전환할 수 있다.

---

## 최종 권고

이번 단계의 핵심은 `추천 모델 강화`가 아니라 `정책 기반 피드 + cursor 기반 조회 + 상태 정의 명확화`다.

즉 먼저 해야 할 일은 다음 순서다.

1. cursor 계약 도입
2. consumed 상태 정의
3. bucket 기반 query 구현
4. 프론트 무한 스크롤 전환
5. 이후에만 impression/dwell 기반 고도화
