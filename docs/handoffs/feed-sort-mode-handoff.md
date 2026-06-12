# Feed Sort Mode Handoff

- Status: Active
- Audience: Mobile Engineers, Frontend Engineers, Backend Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-06-08

## 변경점

`GET /api/diary`에 선택 쿼리 파라미터 `sort`가 추가됐다. 응답 형식은 기존 cursor page와 동일하다.

## 요청 계약

- 추천순 피드: `GET /api/diary` 또는 `GET /api/diary?sort=recommended`
- 최신순 피드: `GET /api/diary?sort=latest`
- 다음 페이지: 같은 `sort` 값에서 응답의 `nextCursor`를 그대로 전달
- 정렬 변경: 기존 `cursor`를 버리고 새 정렬 모드의 첫 페이지부터 요청

`cursor`는 opaque token이다. 클라이언트는 cursor 내용을 해석하거나 수정하지 않는다.

## 정렬 의미

- `recommended`: 추천순. 로그인 사용자는 미소비 친구글, 미소비 공개글, 소비한 친구글, 소비한 공개글 순서로 노출된다.
- `latest`: 피드에 노출 가능한 일기를 `date DESC`, `diaryId DESC` 기록일 최신순으로 노출한다.

두 모드 모두 기존 공개 범위 규칙을 유지한다. 피드 후보 범위는 `PUBLIC`, `ANONYMOUS` 일기와 현재 조회 사용자의 친구가 작성한 `FRIENDS` 일기로 제한된다. 요청 사용자의 자기 일기, 친구가 아닌 사용자의 `FRIENDS` 일기, `PRIVATE` 일기는 피드 목록에서 제외된다. 비로그인 사용자는 `PUBLIC`과 `ANONYMOUS` 일기를 받을 수 있다.

`ANONYMOUS` 일기는 피드 정렬에서는 공개 피드 후보처럼 취급되지만, 응답에서는 작성자 닉네임, 아바타, 사용자 ID, 친구 상태가 익명 전용 값으로 마스킹된다. 클라이언트는 익명 일기 작성자 여부를 `userId` 비교가 아니라 `isOwner`로 판단한다.

## 에러

알 수 없는 `sort` 값은 `400 Bad Request` Problem Details로 반환된다.

- `type`: `https://api.pikume.com/problems/feed/invalid-sort`

다른 정렬 모드에서 발급된 cursor를 재사용하거나 깨진 cursor를 보내면 기존 invalid cursor Problem Details가 반환된다.

- `type`: `https://api.pikume.com/problems/feed/invalid-cursor`

`createdAt` 기준 latest cursor는 `date` 기준 latest cursor로 변환하지 않는다. 이전 버전에서 발급된 latest cursor를 재사용하면 invalid cursor가 반환되며, 클라이언트는 latest 첫 페이지를 다시 요청한다.
