# Notification 도메인 모델

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-02

## 도메인 개요

Notification 도메인은 **시스템 내 이벤트(소셜 상호작용, 컨텐츠 활동 등)를 사용자에게 비동기적으로 전달하는 알림 발송 및 기기 토큰 관리를 담당**하는 도메인입니다.

### 목적

- 친구 요청 및 수락, 좋아요, 댓글, 답글, 친구 일기 등 다른 도메인에서 발생한 이벤트를 수신 대상 사용자에게 실시간으로 전달한다.
- 알림 이력(`Notification`)을 보존하여 사용자가 읽지 않은 알림을 확인하거나 읽음·삭제 처리할 수 있도록 한다.
- FCM 기기 토큰(`FcmToken`)을 관리하여 사용자가 앱을 사용하지 않는 상태에도 푸시 알림을 발송할 수 있는 기반을 제공한다.

### 핵심 책임

- 알림(`Notification`) 생성, 단건 읽음 처리, 전체 읽음 처리, 소프트 삭제 관리
- 알림 이력 저장이 확정된 뒤 SSE 스트림(실시간)과 FCM 푸시(백그라운드)를 이중으로 발송
- 알림 응답에 발신자 닉네임 및 아바타 URL, 대상 일기 썸네일 URL 포함
- 알림 타입(`NotificationType`)에 따른 메시지 문자열 자동 생성
- FCM 발송 실패 시 해당 토큰을 자동으로 삭제하여 유효하지 않은 토큰 관리
- 사용자별 FCM 기기 토큰(`FcmToken`) 등록 및 갱신

### 도메인 경계

- **Aggregate Root**: `Notification`, `FcmToken` (각각 독립적 Aggregate)
- `Notification`은 이벤트 발생 시점의 스냅샷으로, 생성 이후 알림 내용 자체는 불변이다 (읽음 여부·삭제만 변경 가능).
- SSE Emitter 관리(`SseEmitterPort`)와 FCM 실제 전송(`PushNotificationPort`)은 Infrastructure 계층에 위임하며 도메인 범위 밖이다.
- 알림 원인이 되는 이벤트(친구 요청, 좋아요 등) 자체는 각 원천 도메인(Social, Diary)에서 관리한다.

### 타 도메인과의 관계

| 도메인     | 관계 설명                                                                                  |
| ---------- | ------------------------------------------------------------------------------------------ |
| **User**   | `LoadUserForNotificationPort`로 발신자 닉네임 및 아바타를 알림 응답에 포함한다.            |
| **Diary**  | `LoadDiaryForNotificationPort`로 대상 일기의 썸네일 URL을 알림 응답에 포함한다.            |
| **Social** | `Social` 도메인의 `PublishEventPort`가 발행한 이벤트를 구독하여 친구 관련 알림을 생성한다. |

---

## 알림 타입(NotificationType)

_Enum_

### 상수

- `FRIEND_REQUEST` : 친구 요청 수신 알림
- `FRIEND_ACCEPT` : 내 친구 요청에 대한 수락 알림
- `COMMENT` : 내 일기에 달린 새로운 댓글 알림
- `REPLY` : 내 댓글에 달린 새로운 답글 알림
- `FRIEND_DIARY` : 내 친구가 새로 작성한 일기 등록 알림
- `LIKE` : 내 일기에 추가된 좋아요 알림

---

## 팝업/푸시 알림(Notification)

_Entity_

### 속성

- `id` : Long. 단일 알림 메시지의 고유 식별자
- `receiverId` : String. 알림을 수신받아야 하는 대상 사용자의 식별자
- `senderId` : String. 알림 발생을 유발한 주체 사용자의 식별자
- `type` : NotificationType. 발생한 알림의 성격/종류
- `diaryId` : Long. 알림의 원인이 된 일기(Diary)의 식별자 (이벤트에 따라 Null 가능)
- `isRead` : Boolean. 수신자의 알림 확인/읽음 처리에 대한 여부
- `createdAt` : LocalDateTime. 알림 생성 일시 (`BaseEntity` 공통)
- `updatedAt` : LocalDateTime. 최종 수정 일시 (`BaseEntity` 공통)
- `deletedAt` : LocalDateTime. `Notification`이 소유하는 삭제 처리 일시

### 행위

- `Notification(receiverId, senderId, type, diaryId)` : 특정 사용자에게 보낼 새로운 알림 객체를 생성한다.
- `markAsRead()` : 해당 알림을 수신자가 읽음 처리한다.
- `delete()` : 알림을 논리적 삭제 처리하고 삭제 일시를 기록한다.
- `isDeleted()` : 알림이 삭제 처리되었는지 여부를 반환한다.

### 규칙

- 알림이 최초로 생성될 때 읽음 여부(`isRead`)는 항상 `false` (안 읽음) 상태로 초기화된다.
- 수신자(`receiverId`)는 필수 값으로 결측될 수 없다.
- 생성된 알림 내용은 불변이며, 상태 변화는 오직 읽음 처리(`markAsRead()`) 행위만을 통해 발생한다.
- 알림 이력 저장이 확정되지 않은 상태에서는 SSE/FCM 발송을 시도하지 않는다.
- 알림 삭제 시 물리적 데이터 삭제 대신 `deletedAt` 값을 갖는 논리적 삭제 구조를 따른다.

---

## FCM 기기 토큰(FcmToken)

_Entity_

### 속성

- `id` : Long. 기기 토큰 할당 이력의 고유 식별자
- `userId` : String. 해당 기기를 소유 및 사용 중인 사용자 식별자
- `token` : String. 푸시 알림 서버(FCM)로부터 발급된 기기 별 고유 전달 토큰
- `deviceId` : String. 사용자가 접속한 기기(스마트폰 등)의 자체 식별자

### 행위

- `updateToken(String newToken)` : 만료되거나 갱신된 푸시 알림 토큰으로 값을 수정한다.

### 규칙

- 하나의 기기(`deviceId`)를 식별하여 해당 기기에 대한 푸시 토큰(`token`)을 관리한다.
- 특정 기기의 토큰은 만료될 수 있으며, 앱 실행 또는 토큰 갱신 이벤트 리스너를 통해 새로운 토큰으로 교체(`updateToken()`)될 수 있다.
