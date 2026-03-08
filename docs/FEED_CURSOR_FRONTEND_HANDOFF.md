# Feed Cursor Frontend Handoff

작성일: 2026-03-08

## 문서 목적

- 백엔드의 cursor 기반 피드 전환에 맞춰 프론트가 무엇을 바꿔야 하는지 정리한다.
- API 적용 순서, 상태 관리 방식, 이벤트 수집 포인트를 명확히 한다.
- 이 문서는 구현 코드가 아니라 작업 지시서다.

---

## 변경 배경

피드 목록은 기존 `page/size` 기반 offset API에서 `cursor/limit` 기반 API로 전환될 예정이다.

이유는 다음과 같다.

- 피드 특성상 무한 스크롤이 자연스럽다.
- deep page 비용을 줄일 수 있다.
- 정책 기반 랭킹과 bucket 전환 구조를 안정적으로 유지할 수 있다.

프론트는 이에 맞춰:

- 페이지 번호 상태 제거
- `nextCursor` 기반 append 방식으로 전환
- 피드 소비 이벤트를 더 명확히 전송

해야 한다.

---

## 핵심 변경 사항

## 1. 목록 조회 방식 변경

기존:

- `GET /api/diary?page=0&size=20`
- 응답: Spring `Page`

신규:

- `GET /api/diary?cursor=<token>&limit=20`
- 응답:

```json
{
  "items": [],
  "nextCursor": "opaque-token",
  "hasNext": true
}
```

프론트는 `totalPages`, `totalElements`, `pageNumber`에 의존하지 않아야 한다.
목록 endpoint 경로는 그대로 `/api/diary`를 사용하고, query contract만 바뀐다.

## 2. 리스트 상태 관리 변경

기존:

- 현재 페이지 번호 기반
- 페이지 교체형 상태 관리 가능

신규:

- `items[]` 누적 append
- `nextCursor`
- `hasNext`
- `isLoading`
- `isRefreshing`

권장 상태 예시:

```ts
type FeedState = {
  items: FeedItem[];
  nextCursor: string | null;
  hasNext: boolean;
  isLoading: boolean;
  isRefreshing: boolean;
};
```

## 3. 읽음/미읽음 용어 주의

백엔드 1차 기준은 `read/unread`보다 `consumed/not consumed`에 가깝다.

현재 consumed에 가까운 이벤트:

- 상세 진입 click
- 좋아요
- 댓글 작성

따라서 프론트 문구와 분석 이벤트 명도 가능하면 `read`보다 `consumed` 또는 `interaction`에 가깝게 맞추는 것이 좋다.
댓글 작성은 별도 feed 전용 이벤트가 아니라 기존 댓글 작성 성공을 consumed 상태로 간주하는 방향이다.

## 4. eventual consistency 주의

cursor v1에서는 사용 중 상태 변화가 있으면 중복/누락이 생길 수 있다.

예:

- 사용 중 좋아요 수 증가
- 사용 중 댓글 작성
- 친구 관계 변경

이 경우 다음 페이지에서 일부 항목이 다시 보이거나 빠질 수 있다.
프론트는 `diaryId` 기준 dedup을 기본 적용하는 것이 좋다.

---

## API 연동 가이드

## 1. 첫 페이지 요청

```http
GET /api/diary?limit=20
```

cursor가 없으면 첫 페이지다.

## 2. 다음 페이지 요청

```http
GET /api/diary?cursor=<nextCursor>&limit=20
```

## 3. 새로고침

- 기존 items 초기화
- `cursor = null`
- 첫 페이지 재조회

## 4. 중복 요청 방지

아래 조건에서는 추가 요청을 막아야 한다.

- `isLoading === true`
- `hasNext === false`
- 같은 cursor로 이미 요청 중인 경우

---

## UI/UX 작업 항목

## 1. 무한 스크롤 전환

- 하단 sentinel 또는 intersection observer 기반 추가 로딩
- 더보기 버튼이 있다면 임시 유지 가능
- 최종적으로는 cursor append 흐름이 기준

## 2. 새로고침 처리

- pull-to-refresh 또는 상단 새로고침 시 첫 페이지 재요청
- 새로고침은 기존 items를 clear하고 다시 쌓는 방식 권장

## 3. 오류 처리

- 첫 페이지 실패: 전체 에러 상태
- 다음 페이지 실패: 기존 목록 유지 + 재시도 affordance 제공

## 4. 중복 아이템 방지

- v1 cursor는 eventual consistency를 허용하므로 `diaryId` 기준 dedup 로직을 기본 적용해야 한다

---

## 이벤트 수집 작업

## 1. 지금 필요한 이벤트

- `feed_impression`
- `feed_click`
- `feed_dwell`
- `feed_like`

## 2. 1차 우선순위

백엔드 1차에서 반드시 필요한 것은 다음 두 가지다.

- `feed_click`
- `feed_like`

이미 detail 진입 click이 있다면 기존 이벤트 경로를 재사용한다.
댓글 작성은 기존 댓글 API 성공 응답을 consumed 상태로 간주하므로 별도 feed 이벤트를 새로 만들지는 않는다.

## 3. 2차 우선순위

백엔드가 impression/dwell 계약을 제공하면 아래를 추가한다.

### impression 권장 기준

- 카드가 화면에 50% 이상 노출
- 1초 이상 유지

### dwell 권장 기준

- 상세 화면 진입 시 start timestamp 저장
- 상세 화면 이탈 시 dwell 전송

---

## 프론트 작업 체크리스트

## Task 1. 피드 API 클라이언트 추가

- 기존 `/api/diary` 목록 API를 cursor 계약으로 전환
- 응답 타입 정의
- 기존 page 기반 호출 로직 제거 범위 식별

## Task 2. 피드 상태 저장소 수정

- page index 제거
- `items`, `nextCursor`, `hasNext` 중심으로 전환

## Task 3. 무한 스크롤 구현

- sentinel 기반 다음 페이지 요청
- 중복 요청 방지

## Task 4. 새로고침 UX 정리

- 첫 페이지 재호출
- 기존 목록 교체

## Task 5. 소비 이벤트 전송 정리

- click 전송 보장
- like 이벤트 전송 경로 정리
- 추후 impression/dwell 추가 준비

## Task 6. 제거 대상 정리

- `totalPages`, `page`, `pageNumber` 중심 UI 로직 제거
- 페이지네이션 컴포넌트 사용 중이면 영향 범위 점검

---

## QA 체크리스트

- 첫 진입 시 첫 페이지가 정상 노출되는가
- 하단 도달 시 다음 cursor로 정상 append 되는가
- 같은 항목이 중복으로 붙지 않는가
- 새로고침 시 목록이 깨끗하게 재로드되는가
- 에러 후 재시도 시 cursor가 꼬이지 않는가
- 상세 진입 click이 누락되지 않는가
- 좋아요 후 목록 상태가 깨지지 않는가

---

## 백엔드 연동 시 주의사항

## 1. cursor는 opaque token으로 취급

- 프론트는 cursor 내부 값을 해석하지 않는다.
- 그대로 저장하고 그대로 다음 요청에 사용한다.

## 2. hasNext 우선 사용

- `nextCursor`가 있어도 `hasNext`가 false면 추가 요청하지 않는다.

## 3. 정렬 가정 금지

- 프론트는 특정 score나 page 번호를 가정하지 않는다.
- 백엔드가 제공한 순서를 그대로 렌더링한다.

---

## 권장 적용 순서

1. 백엔드가 기존 `/api/diary` 목록 계약을 cursor 기반으로 변경
2. 프론트가 같은 경로에 대해 `cursor/limit` 계약으로 전환
3. 내부 테스트 후 기본값 전환
4. 기존 page 기반 상태 관리 로직 제거

---

## 프론트 팀에 전달할 최종 메시지

이번 변경은 단순한 query param 교체가 아니다.

- 페이지 번호 기반 목록을
- cursor 기반 무한 스크롤 목록으로 바꾸고
- 향후 impression/dwell 이벤트 수집이 가능한 구조로 준비하는 작업이다.

1차 목표는 다음 두 가지다.

- cursor API로 안정적으로 append 되는 피드 목록 구현
- click/like 기반 소비 이벤트가 누락되지 않도록 정리
